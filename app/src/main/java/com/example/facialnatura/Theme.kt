package com.example.facialnatura

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Paleta ───────────────────────────────────────────────────────────────────
val Background    = Color(0xFF060D20)
val Surface       = Color(0xFF0D1535)
val SurfaceVar    = Color(0xFF162040)
val CardBg        = Color(0xFF111C3F)

val Cyan          = Color(0xFF00D4FF)
val CyanDim       = Color(0xFF0099CC)
val Violet        = Color(0xFF7B61FF)

val OnBg          = Color(0xFFFFFFFF)
val TextSec       = Color(0xFF8899BB)
val TextHint      = Color(0xFF445577)

val Success       = Color(0xFF00E676)
val ErrorRed      = Color(0xFFFF4569)
val Warning       = Color(0xFFFFB300)

val FaceBorderActive = Color(0xFF00D4FF)
val FaceBorderIdle   = Color(0xFF445577)

// ── Tipografía ────────────────────────────────────────────────────────────────
// Si no tienes la fuente Inter como recurso, usa el sistema.
// Para añadirla: coloca inter_regular.ttf, inter_medium.ttf, inter_semibold.ttf en res/font/
// y descomenta las líneas de Font() abajo.

val InterFamily = FontFamily.Default   // reemplazar con FontFamily(Font(...)) cuando tengas el recurso

val FaceLoginTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, letterSpacing = (-0.5).sp, color = OnBg
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp, color = OnBg
    ),
    titleLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, color = OnBg
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, color = OnBg
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, color = OnBg
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, color = TextSec
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, color = TextSec
    ),
    labelLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, letterSpacing = 1.sp, color = Cyan
    )
)

// ── Color Scheme ──────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = Cyan,
    onPrimary        = Background,
    primaryContainer = SurfaceVar,
    secondary        = Violet,
    onSecondary      = OnBg,
    background       = Background,
    onBackground     = OnBg,
    surface          = Surface,
    onSurface        = OnBg,
    surfaceVariant   = SurfaceVar,
    onSurfaceVariant = TextSec,
    error            = ErrorRed,
    outline          = TextHint
)

// ── Shapes ────────────────────────────────────────────────────────────────────
val FaceLoginShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ── Theme composable ──────────────────────────────────────────────────────────
@Composable
fun FaceLoginTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = FaceLoginTypography,
        shapes      = FaceLoginShapes,
        content     = content
    )
}