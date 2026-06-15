package com.example.facialnatura

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.facialnatura.Components.ConfidenceBar
import com.example.facialnatura.Components.FaceCard
import com.example.facialnatura.Components.OutlineButton

// ── Activity ──────────────────────────────────────────────────────────────────
class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userName   = intent.getStringExtra("user_name")   ?: "Usuario"
        val userEmail  = intent.getStringExtra("user_email")  ?: ""
        val confidence = intent.getFloatExtra("confidence", 0.95f)

        setContent {
            FaceLoginTheme {
                HomeScreen(
                    userName = userName,
                    userEmail = userEmail,
                    confidence = confidence,
                    onLogout = {
                        startActivity(
                            Intent(this@HomeActivity, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                    }
                )
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    userName: String,
    userEmail: String,
    confidence: Float,
    onLogout: () -> Unit
) {
    // Animación de entrada del avatar
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Glow superior derecho
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = 80.dp, y = (-100).dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(listOf(Cyan.copy(0.07f), Color.Transparent))
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            // ── Avatar + check ──────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Anillo de fondo del avatar
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(Cyan.copy(0.2f), SurfaceVar))
                            )
                    )
                    // Inicial del nombre
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(SurfaceVar)
                            .border(2.dp, Cyan.copy(0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = userName.take(1).uppercase(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 38.sp, color = Cyan
                            )
                        )
                    }
                    // Badge verificado
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Success),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Check, null,
                            tint = Background, modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "¡Hola, $userName!",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Identidad verificada con reconocimiento facial",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // ── Tarjeta de confianza ────────────────────────────────────
            FaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Shield, null, tint = Cyan, modifier = Modifier.size(18.dp))
                    Text(
                        "Verificación biométrica",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = Success.copy(0.15f), shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "ACTIVA",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Success, fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = SurfaceVar)
                Spacer(Modifier.height(16.dp))

                ConfidenceBar(confidence = confidence)
            }

            Spacer(Modifier.height(12.dp))

            // ── Tarjeta de info de sesión ────────────────────────────────
            FaceCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow(Icons.Outlined.Email, "Correo", userEmail)
                if (userEmail.isNotEmpty()) {
                    HorizontalDivider(color = SurfaceVar, modifier = Modifier.padding(vertical = 12.dp))
                }
                InfoRow(Icons.Outlined.AccessTime, "Último acceso", "Ahora mismo")
                HorizontalDivider(color = SurfaceVar, modifier = Modifier.padding(vertical = 12.dp))
                InfoRow(Icons.Outlined.Security, "Método", "Reconocimiento facial")
            }

            Spacer(Modifier.height(24.dp))

            // ── Placeholder de contenido de la app ───────────────────────
            FaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Violet.copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Apps, null, tint = Violet, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Tu aplicación", style = MaterialTheme.typography.titleMedium)
                        Text("Aquí va el contenido principal", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Botón logout ─────────────────────────────────────────────
            OutlineButton(
                text     = "Cerrar sesión",
                onClick  = onLogout,
                icon     = Icons.AutoMirrored.Outlined.Logout,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(36.dp))
        }
    }
}

// ── Info row ──────────────────────────────────────────────────────────────────
@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
            Text(value, style = MaterialTheme.typography.bodyMedium.copy(color = OnBg, fontSize = 13.sp))
        }
    }
}