package com.example.facialnatura.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.facialnatura.data.model.FaceLoginResult
import com.example.facialnatura.Components.*
import com.example.facialnatura.CameraFacePreview
import com.example.facialnatura.FaceOverlayState
import com.example.facialnatura.viewModel.LoginViewModel
import com.example.facialnatura.Background
import com.example.facialnatura.Cyan
import com.example.facialnatura.SurfaceVar
import com.example.facialnatura.ErrorRed
import com.example.facialnatura.OnBg
import com.example.facialnatura.TextHint

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (FaceLoginResult) -> Unit,
    onGoToRegister: () -> Unit
) {
    val uiState by viewModel.uiState.observeAsState(LoginViewModel.LoginUiState.Idle)
    val isFaceReady by viewModel.isFaceReady.observeAsState(false)

    var cameraManager by remember { mutableStateOf<com.example.facialnatura.utils.CameraManager?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    // Derivar estado del overlay según UI state
    val overlayState = when (uiState) {
        is LoginViewModel.LoginUiState.FaceDetected  -> FaceOverlayState.DETECTED
        is LoginViewModel.LoginUiState.Scanning      -> FaceOverlayState.SCANNING
        is LoginViewModel.LoginUiState.Success       -> FaceOverlayState.SUCCESS
        is LoginViewModel.LoginUiState.Error         -> FaceOverlayState.ERROR
        is LoginViewModel.LoginUiState.MultipleFaces -> FaceOverlayState.MULTIPLE
        else                                          -> FaceOverlayState.IDLE
    }

    // Navegar en éxito
    LaunchedEffect(uiState) {
        if (uiState is LoginViewModel.LoginUiState.Success) {
            val result = (uiState as LoginViewModel.LoginUiState.Success).result
            kotlinx.coroutines.delay(800)
            onLoginSuccess(result)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Glow decorativo arriba a la derecha
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = 80.dp, y = (-80).dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Cyan.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

            // ── Header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bienvenido",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Identifícate con tu rostro",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // Mini logo
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceVar),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Face,
                        contentDescription = null,
                        tint = Cyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Cámara + overlay facial ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0A1020))
            ) {
                CameraFacePreview(
                    modifier      = Modifier
                        .fillMaxSize()
                        .padding(bottom = 52.dp), // espacio para el chip
                    overlayState  = overlayState,
                    onFaceDetected = { detected, count ->
                        viewModel.onFaceDetectionUpdate(detected, count)
                    },
                    onCameraReady = { manager ->
                        cameraManager = manager
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Status card ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState is LoginViewModel.LoginUiState.Error,
                enter   = slideInVertically() + fadeIn(),
                exit    = slideOutVertically() + fadeOut()
            ) {
                val msg = (uiState as? LoginViewModel.LoginUiState.Error)?.message ?: ""
                Surface(
                    modifier  = Modifier.fillMaxWidth(),
                    color     = ErrorRed.copy(alpha = 0.12f),
                    shape     = RoundedCornerShape(14.dp),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PulseDot(color = ErrorRed)
                        Text(
                            text  = msg,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ErrorRed, fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Botón principal: escanear ─────────────────────────────────
            PrimaryButton(
                text    = when (uiState) {
                    is LoginViewModel.LoginUiState.Scanning -> "Verificando…"
                    is LoginViewModel.LoginUiState.Success  -> "¡Acceso concedido!"
                    else -> "Iniciar con reconocimiento facial"
                },
                onClick = {
                    if (uiState is LoginViewModel.LoginUiState.Error) viewModel.resetState()
                    cameraManager?.takePicture { bitmap ->
                        bitmap?.let { viewModel.attemptFaceLogin(it) }
                    }
                },
                enabled = isFaceReady || uiState is LoginViewModel.LoginUiState.Error,
                loading = uiState is LoginViewModel.LoginUiState.Scanning,
                icon    = Icons.Outlined.Face,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // ── Botón secundario: registrarse ─────────────────────────────
            OutlineButton(
                text    = "¿Primera vez? Registra tu rostro",
                onClick = onGoToRegister,
                icon    = Icons.Outlined.PersonAdd,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = { showPasswordDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Lock, null, tint = TextHint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Acceder con contraseña", color = TextHint)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPasswordDialog) {
        PasswordLoginDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { email, password ->
                showPasswordDialog = false
                viewModel.loginWithPassword(email, password)
            }
        )
    }
}

@Composable
private fun PasswordLoginDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Acceder con contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    leadingIcon = { Icon(Icons.Outlined.Email, null, tint = Cyan) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = Cyan) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = TextHint
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(email.trim(), password) },
                enabled = email.isNotBlank() && password.isNotBlank()
            ) {
                Text("Entrar", color = Cyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextHint)
            }
        }
    )
}