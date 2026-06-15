package com.example.facialnatura

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FaceLoginTheme {
                SplashScreen(
                    onFinish = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun SplashScreen(onFinish: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(2000)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        // Glow de fondo
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(listOf(Cyan.copy(0.12f), Color.Transparent))
                )
        )

        AnimatedVisibility(
            visible = visible,
            enter   = scaleIn(
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
            ) + fadeIn(tween(400))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icono con fondo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(listOf(SurfaceVar, CardBg))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Face,
                        contentDescription = null,
                        tint = Cyan,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    "FaceLogin",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 34.sp, fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    "Tu rostro, tu contraseña",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Indicador de carga en la parte inferior
        LinearProgressIndicator(
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .width(100.dp)
                .clip(RoundedCornerShape(2.dp)),
            color     = Cyan,
            trackColor = SurfaceVar
        )
    }
}