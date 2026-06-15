package com.example.facialnatura.Components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.facialnatura.*

// ── Botón primario cian ───────────────────────────────────────────────────────
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Cyan,
            contentColor   = Background,
            disabledContainerColor = Cyan.copy(alpha = 0.35f),
            disabledContentColor   = Background.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(27.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "btn_loading"
        ) { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Background,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Background, fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

// ── Botón outline cian ────────────────────────────────────────────────────────
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        border = BorderStroke(1.5.dp, Cyan),
        shape = RoundedCornerShape(25.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(color = Cyan)
            )
        }
    }
}

// ── Status chip (estado de detección facial) ──────────────────────────────────
@Composable
fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = OnBg,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

// ── Card base ─────────────────────────────────────────────────────────────────
@Composable
fun FaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier  = modifier,
        color     = CardBg,
        shape     = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, SurfaceVar, RoundedCornerShape(20.dp))
                .padding(18.dp),
            content = content
        )
    }
}

// ── Indicador de fiabilidad con barra ─────────────────────────────────────────
@Composable
fun ConfidenceBar(
    confidence: Float,       // 0f..1f
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = confidence,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "confidence"
    )
    val barColor = when {
        confidence >= 0.85f -> Success
        confidence >= 0.65f -> Warning
        else                -> ErrorRed
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Confianza de verificación",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "${(animated * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = barColor, fontWeight = FontWeight.SemiBold
                )
            )
        }
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color    = barColor,
            trackColor = SurfaceVar
        )
    }
}

// ── Sección etiqueta ──────────────────────────────────────────────────────────
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text     = text,
        modifier = modifier,
        style    = MaterialTheme.typography.labelSmall.copy(
            color = Cyan, fontWeight = FontWeight.SemiBold
        )
    )
}

// ── Indicador de escaneo pulsante ─────────────────────────────────────────────
@Composable
fun PulseDot(color: Color, modifier: Modifier = Modifier) {
    val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.85f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Box(
        modifier = modifier
            .size((8 * scale).dp)
            .clip(CircleShape)
            .background(color)
    )
}