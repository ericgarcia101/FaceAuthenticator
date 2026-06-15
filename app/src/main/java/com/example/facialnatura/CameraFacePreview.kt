package com.example.facialnatura

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.facialnatura.Components.StatusChip
import com.example.facialnatura.utils.CameraManager

/**
 * Composable que muestra la cámara frontal + overlay facial en Compose puro.
 *
 * @param overlayState  estado visual del óvalo (IDLE/SCANNING/SUCCESS/ERROR)
 * @param onFaceDetected  (detected: Boolean, count: Int)
 * @param onCameraReady   devuelve el CameraManager para llamar takePicture()
 */
@Composable
fun CameraFacePreview(
    modifier: Modifier = Modifier,
    overlayState: FaceOverlayState = FaceOverlayState.IDLE,
    onFaceDetected: (Boolean, Int) -> Unit = { _, _ -> },
    onCameraReady: (CameraManager) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraManager by remember { mutableStateOf<CameraManager?>(null) }

    // Cerrar la cámara cuando el composable sale de la composición
    DisposableEffect(Unit) {
        onDispose {
            cameraManager?.close()
            cameraManager = null
        }
    }

    Box(modifier = modifier) {
        // ── CameraX Preview (AndroidView) ────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { preview ->
                    val manager = CameraManager(
                        context        = ctx,
                        lifecycleOwner = lifecycleOwner,
                        previewView    = preview
                    )
                    manager.onFaceDetected = { detected, count ->
                        onFaceDetected(detected, count)
                    }
                    // startCamera() ya se llama desde init → setUpCamera()
                    cameraManager = manager
                    onCameraReady(manager)
                }
            }
        )

        // ── Overlay Compose (Canvas) ──────────────────────────────────────
        FaceOverlayCanvas(
            modifier = Modifier.fillMaxSize(),
            state    = overlayState
        )

        // ── Status chip centrado abajo ─────────────────────────────────────
        val chipText = when (overlayState) {
            FaceOverlayState.IDLE     -> "Coloca tu rostro en el óvalo"
            FaceOverlayState.SCANNING -> "Escaneando…"
            FaceOverlayState.SUCCESS  -> "✓ Rostro verificado"
            FaceOverlayState.ERROR    -> "Rostro no reconocido"
            FaceOverlayState.DETECTED -> "Rostro detectado"
            FaceOverlayState.MULTIPLE -> "Demasiadas personas"
        }
        val chipColor = when (overlayState) {
            FaceOverlayState.SUCCESS  -> Color(0xFF00E676)
            FaceOverlayState.ERROR    -> Color(0xFFFF4569)
            FaceOverlayState.DETECTED,
            FaceOverlayState.SCANNING -> Color(0xFF00D4FF)
            FaceOverlayState.MULTIPLE -> Color(0xFFFFB300)
            else                      -> Color(0xFF8899BB)
        }

        StatusChip(
            text  = chipText,
            color = chipColor,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}

// ── Estado del overlay ────────────────────────────────────────────────────────
enum class FaceOverlayState { IDLE, DETECTED, SCANNING, SUCCESS, ERROR, MULTIPLE }

// ── Canvas del overlay facial ─────────────────────────────────────────────────
@Composable
fun FaceOverlayCanvas(
    modifier: Modifier = Modifier,
    state: FaceOverlayState
) {
    val scanY = remember { androidx.compose.animation.core.Animatable(0f) }
    val borderAlpha = remember { androidx.compose.animation.core.Animatable(1f) }

    val isScanning = state == FaceOverlayState.SCANNING || state == FaceOverlayState.DETECTED

    // Animación de línea de escaneo
    LaunchedEffect(isScanning) {
        if (isScanning) {
            scanY.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            scanY.snapTo(0f)
        }
    }

    // Pulso del borde
    LaunchedEffect(isScanning) {
        if (isScanning) {
            borderAlpha.animateTo(
                targetValue = 0.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            borderAlpha.snapTo(1f)
        }
    }

    val borderColor = when (state) {
        FaceOverlayState.IDLE     -> Color(0xFF445577)
        FaceOverlayState.DETECTED,
        FaceOverlayState.SCANNING -> Color(0xFF00D4FF)
        FaceOverlayState.SUCCESS  -> Color(0xFF00E676)
        FaceOverlayState.ERROR    -> Color(0xFFFF4569)
        FaceOverlayState.MULTIPLE -> Color(0xFFFFB300)
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Óvalo: 72% del ancho, ratio 1:1.25, centrado y ligeramente más arriba
        val ovalW = w * 0.72f
        val ovalH = ovalW * 1.28f
        val cx = w / 2f
        val cy = h / 2f - h * 0.04f

        val ovalRect = Rect(
            left   = cx - ovalW / 2,
            top    = cy - ovalH / 2,
            right  = cx + ovalW / 2,
            bottom = cy + ovalH / 2
        )

        // 1. Fondo oscuro (fuera del óvalo) con Path
        val path = Path().apply {
            addRect(Rect(Offset.Zero, Size(w, h)))     // rect completo
            addOval(ovalRect)                           // "resta" el óvalo
        }
        drawPath(
            path  = path,
            color = Color(0xCC060D20),
            style = Fill
        )

        // 2. Borde del óvalo
        drawOval(
            color      = borderColor.copy(alpha = borderAlpha.value),
            topLeft    = ovalRect.topLeft,
            size       = Size(ovalRect.width, ovalRect.height),
            style      = Stroke(width = 2.5f)
        )

        // 3. Línea de escaneo
        if (isScanning) {
            val lineY = ovalRect.top + ovalRect.height * scanY.value
            drawLine(
                brush  = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xCC00D4FF),
                        Color.Transparent
                    ),
                    startX = ovalRect.left,
                    endX   = ovalRect.right
                ),
                start       = Offset(ovalRect.left + 10f, lineY),
                end         = Offset(ovalRect.right - 10f, lineY),
                strokeWidth = 2f
            )
        }

        // 4. Esquinas decorativas (tipo mira)
        drawCorners(ovalRect, borderColor)
    }
}

private fun DrawScope.drawCorners(oval: Rect, color: Color) {
    val cs = 26f   // corner size
    val sw = 3f    // stroke width
    val paint = Stroke(width = sw, cap = StrokeCap.Round)

    val l = oval.left; val t = oval.top
    val r = oval.right; val b = oval.bottom

    // Superior izquierda
    drawLine(color, Offset(l, t + cs), Offset(l, t + 2), paint.width)
    drawLine(color, Offset(l + 2, t), Offset(l + cs, t), paint.width)
    // Superior derecha
    drawLine(color, Offset(r - cs, t), Offset(r - 2, t), paint.width)
    drawLine(color, Offset(r, t + 2), Offset(r, t + cs), paint.width)
    // Inferior izquierda
    drawLine(color, Offset(l, b - cs), Offset(l, b - 2), paint.width)
    drawLine(color, Offset(l + 2, b), Offset(l + cs, b), paint.width)
    // Inferior derecha
    drawLine(color, Offset(r - cs, b), Offset(r - 2, b), paint.width)
    drawLine(color, Offset(r, b - 2), Offset(r, b - cs), paint.width)
}

// extension para StrokeCap.Round en drawLine
private fun DrawScope.drawLine(
    color: Color, start: Offset, end: Offset, strokeWidth: Float
) = drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)