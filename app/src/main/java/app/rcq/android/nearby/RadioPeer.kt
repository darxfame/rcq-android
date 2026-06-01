package app.rcq.android.nearby

/**
 * One discoverable thing on the local Radio mesh — either a person willing to
 * take a 1:1 chat invite, or a room someone is hosting. Android port of the
 * iOS `RadioPeer`.
 *
 * Unlike iOS (which leans on `MCPeerID`), the device-level identity here is
 * [endpointId] — a stable per-session random token the device mints once when
 * it goes online and broadcasts in its BLE advertisement. It dedupes peers
 * across scan ticks and addresses them on the data plane. [wifiMac] is the
 * peer's Wi-Fi Direct device address (also carried in the advertisement), used
 * to open the Wi-Fi Direct connection once the user picks the peer.
 */
data class RadioPeer(
    val endpointId: String,
    /** Anonymous label minted client-side before advertising — "Wandering
     *  Stranger #4982" style. The real UIN never goes on the Radio wire. */
    val displayName: String,
    val kind: Kind,
    /** Room metadata when [kind] == [Kind.Room]; null for 1:1 peers. */
    val room: RadioRoomMetadata? = null,
    /** Wi-Fi Direct device address advertised over BLE, used to connect. */
    val wifiMac: String? = null,
    /** Session-level connection state driving the discovery row badge. */
    val state: ConnectionState = ConnectionState.Discovered,
) {
    enum class Kind { OneToOne, Room }

    enum class ConnectionState { Discovered, Inviting, Connecting, Connected, Disconnected }
}

/**
 * Metadata about a room being advertised. The shared AES key is NOT here — for
 * open rooms it's derived from the room id (anyone may join), for closed rooms
 * from the password client-side (see [app.rcq.android.crypto.RadioCrypto]). The
 * BLE advertisement only carries the id, a trimmed name and the password flag;
 * how those bytes are packed into the advertisement / GATT read lives in the
 * BLE discovery layer.
 */
data class RadioRoomMetadata(
    /** Stable UUID per room — both for joining and for key derivation.
     *  Kept lowercase for parity with the iOS room id. */
    val roomId: String,
    val name: String,
    val needsPassword: Boolean,
)
