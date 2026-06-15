package com.example.facialnatura.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.facialnatura.*
import com.example.facialnatura.Components.*
import com.example.facialnatura.utils.CameraManager
import com.example.facialnatura.viewModel.RegisterUiState
import com.example.facialnatura.viewModel.RegisterViewModel

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cameraManager by remember { mutableStateOf<CameraManager?>(null) }
    var faceDetected by remember { mutableStateOf(false) }

    LaunchedEffect(state.step) {
        if (state.step == RegisterUiState.Step.SUCCESS) onSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Glow decorativo
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(listOf(Violet.copy(alpha = 0.08f), Color.Transparent))
                )
        )

        when (state.step) {
            RegisterUiState.Step.FORM -> {
                RegisterFormStep(
                    name = name,
                    onNameChange = { name = it },
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    nameError = state.nameError,
                    emailError = state.emailError,
                    passwordError = state.passwordError,
                    generalError = state.error,
                    faceDetected = faceDetected,
                    onFaceDetected = { detected, count ->
                        faceDetected = detected && count == 1
                        viewModel.onFaceDetected(detected, count)
                    },
                    onCameraReady = { cameraManager = it },
                    onCapture = {
                        cameraManager?.takePicture { bitmap ->
                            viewModel.captureFace(bitmap)
                        }
                    },
                    onBack = onBack
                )
            }

            RegisterUiState.Step.CAPTURED -> {
                RegisterCapturedStep(
                    name = name,
                    email = email,
                    onConfirm = { viewModel.register(name, email, password) },
                    onRetake = { viewModel.retakePhoto() }
                )
            }

            RegisterUiState.Step.UPLOADING -> {
                RegisterUploadingStep()
            }

            RegisterUiState.Step.SUCCESS -> {
                // La LaunchedEffect de arriba llama a onSuccess()
            }
        }
    }
}

// ── Paso 1: Formulario + Cámara ───────────────────────────────────────────────

@Composable
private fun RegisterFormStep(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    nameError: String?,
    emailError: String?,
    passwordError: String?,
    generalError: String?,
    faceDetected: Boolean,
    onFaceDetected: (Boolean, Int) -> Unit,
    onCameraReady: (CameraManager) -> Unit,
    onCapture: () -> Unit,
    onBack: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val overlayState = if (faceDetected) FaceOverlayState.DETECTED else FaceOverlayState.IDLE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = OnBg
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Registrar Rostro", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Introduce tus datos y captura tu cara",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Campos de texto
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nombre completo") },
            leadingIcon = { Icon(Icons.Outlined.Person, null, tint = Cyan) },
            modifier = Modifier.fillMaxWidth(),
            isError = nameError != null,
            supportingText = { nameError?.let { Text(it, color = ErrorRed) } },
            colors = outlinedTextFieldColors(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Outlined.Email, null, tint = Cyan) },
            modifier = Modifier.fillMaxWidth(),
            isError = emailError != null,
            supportingText = { emailError?.let { Text(it, color = ErrorRed) } },
            colors = outlinedTextFieldColors(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = Cyan) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                        tint = TextHint
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            isError = passwordError != null,
            supportingText = { passwordError?.let { Text(it, color = ErrorRed) } },
            colors = outlinedTextFieldColors(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(16.dp))

        // Cámara
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0A1020))
        ) {
            CameraFacePreview(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 48.dp),
                overlayState = overlayState,
                onFaceDetected = onFaceDetected,
                onCameraReady = onCameraReady
            )
        }

        Spacer(Modifier.height(16.dp))

        // Error general
        AnimatedVisibility(visible = generalError != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ErrorRed.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulseDot(color = ErrorRed)
                    Text(
                        generalError ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Botón capturar
        PrimaryButton(
            text = if (faceDetected) "Capturar Rostro" else "Coloca tu cara en el óvalo",
            onClick = onCapture,
            enabled = faceDetected,
            icon = Icons.Outlined.Face,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))
    }
}

// ── Paso 2: Confirmación ───────────────────────────────────────────────────────

@Composable
private fun RegisterCapturedStep(
    name: String,
    email: String,
    onConfirm: () -> Unit,
    onRetake: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        // Icono de éxito
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(Success.copy(0.18f), Color.Transparent))
                        )
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Success.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.FaceRetouchingNatural,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Rostro capturado",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Los puntos faciales han sido extraídos.\nConfirma tus datos para completar el registro.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Tarjeta de datos
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardBg,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                SectionLabel("DATOS DE REGISTRO")
                Spacer(Modifier.height(14.dp))
                DataRow(icon = Icons.Outlined.Person, label = "Nombre", value = name)
                HorizontalDivider(
                    color = SurfaceVar,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                DataRow(icon = Icons.Outlined.Email, label = "Correo", value = email)
                HorizontalDivider(
                    color = SurfaceVar,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                // Indicador de datos biométricos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.Fingerprint, null, tint = Cyan, modifier = Modifier.size(18.dp))
                    Column {
                        Text(
                            "Puntos faciales",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextHint, letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            "~152 puntos capturados",
                            style = MaterialTheme.typography.bodyMedium.copy(color = OnBg)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = Success.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "OK",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Success, fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            text = "Confirmar y Registrar",
            onClick = onConfirm,
            icon = Icons.Outlined.Check,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlineButton(
            text = "Repetir captura",
            onClick = onRetake,
            icon = Icons.Outlined.Refresh,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(36.dp))
    }
}

// ── Paso 3: Cargando ──────────────────────────────────────────────────────────

@Composable
private fun RegisterUploadingStep() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                color = Cyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(56.dp)
            )
            Text(
                "Registrando tu rostro…",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Guardando puntos faciales en la nube",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

@Composable
private fun DataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(18.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextHint, letterSpacing = 0.5.sp
                )
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(color = OnBg)
            )
        }
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Cyan,
    unfocusedBorderColor = TextHint,
    focusedLabelColor = Cyan,
    unfocusedLabelColor = TextSec,
    cursorColor = Cyan,
    focusedTextColor = OnBg,
    unfocusedTextColor = OnBg
)