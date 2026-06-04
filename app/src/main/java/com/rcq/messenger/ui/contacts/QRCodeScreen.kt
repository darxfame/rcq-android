package com.rcq.messenger.ui.contacts

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.media.Image as MediaImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.rcq.messenger.ui.theme.LocalRCQColors
import com.rcq.messenger.ui.theme.RCQMetrics
import java.util.EnumMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeScreen(
    ownUin: Long,
    onBack: () -> Unit,
    onUserScanned: (Long) -> Unit = {}
) {
    val rcq = LocalRCQColors.current
    val context = LocalContext.current
    var mode by remember { mutableStateOf(QRMode.MyCode) }
    val qrBitmap = remember(ownUin) {
        if (ownUin > 0) generateQR("rcq://$ownUin", 512) else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My QR Code", color = rcq.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = rcq.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = rcq.bgPrimary)
            )
        },
        containerColor = rcq.bgPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(RCQMetrics.screenHPad * 2)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                QRMode.entries.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = mode == item,
                        onClick = { mode = item },
                        shape = SegmentedButtonDefaults.itemShape(index, QRMode.entries.size),
                        icon = {
                            Icon(
                                if (item == QRMode.MyCode) Icons.Default.QrCode else Icons.Default.CameraAlt,
                                contentDescription = null
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
            Spacer(Modifier.height(RCQMetrics.rowHPad * 2))

            when (mode) {
                QRMode.MyCode -> MyCodePane(
                    ownUin = ownUin,
                    qrBitmap = qrBitmap,
                    onShare = {
                        val share = Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, "rcq://$ownUin")
                        context.startActivity(Intent.createChooser(share, "Share UIN"))
                    }
                )
                QRMode.Scanner -> QRScannerPane(onUserScanned)
            }
        }
    }
}

@Composable
private fun MyCodePane(
    ownUin: Long,
    qrBitmap: Bitmap?,
    onShare: () -> Unit
) {
    val rcq = LocalRCQColors.current
    Spacer(Modifier.height(RCQMetrics.rowHPad * 2))
    qrBitmap?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "QR",
            modifier = Modifier
                .size(RCQMetrics.avatarLg * 6.6f)
                .clip(RoundedCornerShape(RCQMetrics.bubbleRadius))
        )
    }
    Spacer(Modifier.height(RCQMetrics.rowHPad * 2))
    Text(
        "UIN: $ownUin",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = rcq.textPrimary
    )
    Spacer(Modifier.height(RCQMetrics.rowVPad))
    Text(
        "Others can scan this to add you",
        color = rcq.textSecondary,
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(RCQMetrics.rowHPad * 2))
    Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Share, contentDescription = null)
        Text("Share")
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun QRScannerPane(onUserScanned: (Long) -> Unit) {
    val rcq = LocalRCQColors.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    if (!cameraPermission.status.isGranted) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Text("Allow camera")
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CameraPreview(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(RCQMetrics.bubbleRadius)),
            onQrValue = { value ->
                parseQrUin(value)?.let(onUserScanned)
            }
        )
        Spacer(Modifier.height(RCQMetrics.rowHPad))
        Text("Scan an RCQ QR code", color = rcq.textSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(
    modifier: Modifier,
    onQrValue: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val handled = remember { AtomicBoolean(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val reader = remember {
        MultiFormatReader().apply {
            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
            hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(BarcodeFormat.QR_CODE)
            setHints(hints)
        }
    }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor) { imageProxy ->
                        val result = runCatching {
                            imageProxy.toBinaryBitmap()?.let { reader.decodeWithState(it).text }
                        }.getOrNull()
                        reader.reset()
                        imageProxy.close()
                        if (result != null && handled.compareAndSet(false, true)) {
                            ContextCompat.getMainExecutor(ctx).execute { onQrValue(result) }
                        }
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                },
                ContextCompat.getMainExecutor(ctx)
            )
            previewView
        }
    )
}

private enum class QRMode(val label: String) {
    MyCode("My code"),
    Scanner("Scan")
}

fun generateQR(content: String, size: Int): Bitmap {
    val bits = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until size) {
            for (y in 0 until size) {
                setPixel(x, y, if (bits[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
    }
}

private fun parseQrUin(value: String): Long? {
    val trimmed = value.trim()
    val raw = when {
        trimmed.startsWith("rcq://") -> trimmed.removePrefix("rcq://")
        trimmed.startsWith("rcq:") -> trimmed.removePrefix("rcq:")
        else -> trimmed
    }
    return raw.substringBefore("?").filter { it.isDigit() }.toLongOrNull()
}

@ExperimentalGetImage
private fun ImageProxy.toBinaryBitmap(): BinaryBitmap? {
    val mediaImage = image ?: return null
    val luminance = mediaImage.copyLuminancePlane()
    val source = PlanarYUVLuminanceSource(
        luminance,
        mediaImage.width,
        mediaImage.height,
        0,
        0,
        mediaImage.width,
        mediaImage.height,
        false
    )
    return BinaryBitmap(HybridBinarizer(source))
}

private fun MediaImage.copyLuminancePlane(): ByteArray {
    val yPlane = planes[0]
    val buffer = yPlane.buffer.duplicate()
    val width = width
    val height = height
    val rowStride = yPlane.rowStride
    val pixelStride = yPlane.pixelStride
    val data = ByteArray(width * height)

    if (pixelStride == 1 && rowStride == width) {
        buffer.get(data, 0, data.size)
        return data
    }

    val row = ByteArray(rowStride)
    for (y in 0 until height) {
        buffer.position(y * rowStride)
        buffer.get(row, 0, rowStride.coerceAtMost(buffer.remaining()))
        for (x in 0 until width) {
            data[y * width + x] = row[x * pixelStride]
        }
    }
    return data
}
