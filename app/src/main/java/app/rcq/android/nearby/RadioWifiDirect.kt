package app.rcq.android.nearby

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Wi-Fi Direct half of the hybrid Radio transport: the high-bandwidth *data
 * session* (text + reactions + PTT voice) that [RadioBleDiscovery] hands off to
 * once the user picks a peer.
 *
 * ## Topology — host is the hub
 * The host (room creator, or the inviter in a 1:1) becomes the Wi-Fi Direct
 * **group owner (GO)** and runs a [ServerSocket]; every joiner opens a [Socket]
 * to it. The GO relays each frame it receives to the *other* clients (star
 * fan-out) and also delivers it locally, so an N-party room is GO + N−1 spokes.
 * A 1:1 is just the degenerate two-node case.
 *
 * ## Bootstrap — endpointId is the join token
 * Both sides register a Wi-Fi Direct DNS-SD local service whose TXT record
 * echoes their BLE [myEndpointId]. To connect, the initiator runs a short
 * service-discovery burst, matches the TXT `eid` to the BLE-discovered peer,
 * learns its `deviceAddress`, and calls [WifiP2pManager.connect]. The OS
 * handles the peer's consent (the system "invitation to connect" prompt) — there
 * is no in-app invite sheet, because the BLE beacons here are non-connectable.
 * After `WIFI_P2P_CONNECTION_CHANGED` we request the connection info: if we're
 * the GO we serve, otherwise we connect to `groupOwnerAddress`.
 *
 * ## v1 scope
 * All frames (including voice) ride the reliable TCP socket — over a dedicated
 * single-hop Wi-Fi Direct LAN the latency is low and there's no competing
 * traffic. A dedicated UDP path for voice (matching the iOS `.unreliable` send)
 * is a documented follow-up.
 *
 * ⚠ COMPILE-VERIFIED ONLY. The emulator has no Wi-Fi Direct radio. Group
 * formation, GO negotiation and DNS-SD discovery are notoriously OEM-variable;
 * the real check is two physical devices.
 */
class RadioWifiDirect(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val myEndpointId: String,
    private val onFrameReceived: (RadioFrame) -> Unit,
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit,
    private val onError: (String) -> Unit,
) {
    private val manager by lazy {
        appContext.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null

    // Data plane.
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private val goClients = CopyOnWriteArrayList<ClientLink>() // GO side: one per joiner
    private var clientOut: DataOutputStream? = null            // joiner side: to the GO
    private var ioJob: Job? = null
    private var discoveryJob: Job? = null

    @Volatile private var isHost = false
    @Volatile private var connectedAnnounced = false
    /** eid we are trying to reach (joiner side), matched against TXT records. */
    @Volatile private var targetEndpointId: String? = null
    @Volatile private var resolvedDeviceAddress: String? = null

    private class ClientLink(val socket: Socket, val out: DataOutputStream)

    // ── lifecycle ─────────────────────────────────────────────────────
    fun start() {
        val mgr = manager ?: run { onError("wifip2p_unavailable"); return }
        channel = mgr.initialize(appContext, appContext.mainLooper, null)
        registerReceiver()
        registerLocalService()
    }

    /** Host a session: become an autonomous GO and start accepting joiners. */
    @SuppressLint("MissingPermission")
    fun hostGroup() {
        isHost = true
        val mgr = manager ?: return
        val ch = channel ?: return
        // The connection-changed broadcast then triggers requestConnectionInfo
        // → startServer() once the group is up.
        mgr.createGroup(ch, actionListener("createGroup") {})
    }

    /** Join the peer advertising [targetEid]: resolve it over DNS-SD, then
     *  connect. The connection-changed broadcast wires up the socket. */
    fun connectToPeer(targetEid: String) {
        isHost = false
        targetEndpointId = targetEid
        resolvedDeviceAddress = null
        startServiceDiscovery()
    }

    /** Send a frame to every connected peer. On the GO this writes to all
     *  client sockets; on a joiner it writes to the GO (which fans out). */
    fun broadcastFrame(frame: RadioFrame) {
        val bytes = RadioWire.encodeFrame(frame)
        scope.launch(Dispatchers.IO) {
            if (isHost) {
                goClients.forEach { link -> writeFrame(link.out, bytes) { dropClient(link) } }
            } else {
                clientOut?.let { writeFrame(it, bytes) { onDisconnected() } }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun teardown() {
        discoveryJob?.cancel(); discoveryJob = null
        ioJob?.cancel(); ioJob = null
        runCatching { serverSocket?.close() }; serverSocket = null
        goClients.forEach { runCatching { it.socket.close() } }; goClients.clear()
        runCatching { clientSocket?.close() }; clientSocket = null
        clientOut = null
        connectedAnnounced = false
        targetEndpointId = null
        resolvedDeviceAddress = null
        val mgr = manager; val ch = channel
        if (mgr != null && ch != null) {
            runCatching { mgr.clearLocalServices(ch, null) }
            runCatching { mgr.clearServiceRequests(ch, null) }
            runCatching { mgr.removeGroup(ch, null) }
        }
        unregisterReceiver()
        isHost = false
    }

    // ── DNS-SD: advertise our eid, resolve the target's ────────────────
    @SuppressLint("MissingPermission")
    private fun registerLocalService() {
        val mgr = manager ?: return; val ch = channel ?: return
        val record = mapOf(TXT_EID to myEndpointId)
        val info = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_INSTANCE, SERVICE_TYPE, record)
        mgr.addLocalService(ch, info, actionListener("addLocalService") {})
    }

    @SuppressLint("MissingPermission")
    private fun startServiceDiscovery() {
        val mgr = manager ?: return; val ch = channel ?: return

        val servListener = WifiP2pManager.DnsSdServiceResponseListener { _, _, _ -> }
        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { fullDomain, record, device ->
            val eid = record?.get(TXT_EID)
            if (eid != null && eid == targetEndpointId && resolvedDeviceAddress == null) {
                resolvedDeviceAddress = device.deviceAddress
                Log.i(TAG, "resolved eid=$eid → ${device.deviceAddress} ($fullDomain)")
                connectToResolved(device)
            }
        }
        mgr.setDnsSdResponseListeners(ch, servListener, txtListener)
        val request = WifiP2pDnsSdServiceRequest.newInstance()
        mgr.addServiceRequest(ch, request, actionListener("addServiceRequest") {
            mgr.discoverServices(ch, actionListener("discoverServices") {})
        })

        // Re-kick discovery for a while — DNS-SD often misses the first pass.
        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            var tries = 0
            while (isActive && resolvedDeviceAddress == null && tries < 8) {
                delay(2_000)
                tries++
                runCatching { mgr.discoverServices(ch, null) }
            }
            if (resolvedDeviceAddress == null) onError("wifip2p_peer_not_found")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToResolved(device: WifiP2pDevice) {
        val mgr = manager ?: return; val ch = channel ?: return
        discoveryJob?.cancel()
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
            groupOwnerIntent = 0 // we want to be the client; the host should be GO
        }
        mgr.connect(ch, config, actionListener("connect") {})
    }

    // ── connection-changed → wire up the socket ────────────────────────
    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION) {
                    requestConnectionInfo()
                }
            }
        }
        receiver = r
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(r, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(r, filter)
        }
    }

    private fun unregisterReceiver() {
        receiver?.let { runCatching { appContext.unregisterReceiver(it) } }
        receiver = null
    }

    @SuppressLint("MissingPermission")
    private fun requestConnectionInfo() {
        val mgr = manager ?: return; val ch = channel ?: return
        mgr.requestConnectionInfo(ch) { info -> onConnectionInfo(info) }
    }

    private fun onConnectionInfo(info: WifiP2pInfo) {
        if (!info.groupFormed) {
            if (connectedAnnounced) { connectedAnnounced = false; onDisconnected() }
            return
        }
        if (info.isGroupOwner) {
            if (serverSocket == null) startServer()
        } else {
            val goAddress = info.groupOwnerAddress?.hostAddress ?: return
            if (clientSocket == null) startClient(goAddress)
        }
    }

    // ── GO data plane ──────────────────────────────────────────────────
    private fun startServer() {
        ioJob = scope.launch(Dispatchers.IO) {
            val ss = runCatching { ServerSocket(DATA_PORT) }.getOrElse {
                onError("wifip2p_server_bind"); return@launch
            }
            serverSocket = ss
            announceConnectedOnce()
            while (isActive && !ss.isClosed) {
                val socket = runCatching { ss.accept() }.getOrNull() ?: break
                handleNewClient(socket)
            }
        }
    }

    private fun handleNewClient(socket: Socket) {
        runCatching { socket.tcpNoDelay = true }
        val out = DataOutputStream(socket.getOutputStream().buffered())
        val link = ClientLink(socket, out)
        goClients.add(link)
        announceConnectedOnce()
        scope.launch(Dispatchers.IO) {
            readLoop(socket) { frame, raw ->
                onFrameReceived(frame)              // deliver locally on the GO
                relayToOthers(raw, except = link)   // fan-out to the other spokes
            }
            dropClient(link)
        }
    }

    private fun relayToOthers(raw: ByteArray, except: ClientLink) {
        goClients.forEach { link -> if (link !== except) writeFrame(link.out, raw) { dropClient(link) } }
    }

    private fun dropClient(link: ClientLink) {
        if (goClients.remove(link)) {
            runCatching { link.socket.close() }
            if (goClients.isEmpty()) { connectedAnnounced = false; onDisconnected() }
        }
    }

    // ── joiner data plane ──────────────────────────────────────────────
    private fun startClient(goAddress: String) {
        ioJob = scope.launch(Dispatchers.IO) {
            val socket = Socket()
            val ok = runCatching {
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(goAddress, DATA_PORT), 8_000)
            }.isSuccess
            if (!ok) { onError("wifip2p_client_connect"); return@launch }
            clientSocket = socket
            clientOut = DataOutputStream(socket.getOutputStream().buffered())
            announceConnectedOnce()
            readLoop(socket) { frame, _ -> onFrameReceived(frame) }
            clientOut = null
            connectedAnnounced = false
            onDisconnected()
        }
    }

    // ── shared framed-socket IO (4-byte length prefix) ─────────────────
    private inline fun readLoop(socket: Socket, onEach: (RadioFrame, ByteArray) -> Unit) {
        val input = DataInputStream(socket.getInputStream().buffered())
        try {
            while (!socket.isClosed) {
                val len = input.readInt()
                if (len <= 0 || len > MAX_FRAME) break
                val raw = ByteArray(len)
                input.readFully(raw)
                val frame = RadioWire.decodeFrame(raw) ?: continue
                onEach(frame, raw)
            }
        } catch (_: Exception) {
            // socket closed / peer gone — loop ends, caller cleans up
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun writeFrame(out: DataOutputStream, raw: ByteArray, onFail: () -> Unit) {
        try {
            synchronized(out) {
                out.writeInt(raw.size)
                out.write(raw)
                out.flush()
            }
        } catch (_: Exception) {
            onFail()
        }
    }

    private fun announceConnectedOnce() {
        if (!connectedAnnounced) { connectedAnnounced = true; scope.launch { onConnected() } }
    }

    private fun actionListener(tag: String, onOk: () -> Unit) = object : WifiP2pManager.ActionListener {
        override fun onSuccess() = onOk()
        override fun onFailure(reason: Int) {
            Log.w(TAG, "$tag failed: $reason")
            if (reason == WifiP2pManager.P2P_UNSUPPORTED) onError("wifip2p_unsupported")
        }
    }

    companion object {
        private const val TAG = "RadioWifiP2p"
        private const val DATA_PORT = 8989
        private const val MAX_FRAME = 512 * 1024
        private const val SERVICE_INSTANCE = "rcqradio"
        private const val SERVICE_TYPE = "_presence._tcp"
        private const val TXT_EID = "eid"
    }
}
