package com.example.facialnatura

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.facialnatura.ml.FaceEmbeddingHelper
import com.example.facialnatura.data.repository.UserRepository
import com.example.facialnatura.viewModel.LoginViewModel
import com.example.facialnatura.viewModel.LoginViewModelFactory
import com.example.facialnatura.screen.LoginScreen

class LoginActivity : ComponentActivity() {

    private val embeddingHelper by lazy { FaceEmbeddingHelper(applicationContext) }
    private val repository by lazy { UserRepository(embeddingHelper) }
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(repository)
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasCameraPermission()) requestPermission.launch(Manifest.permission.CAMERA)

        setContent {
            FaceLoginTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { result ->
                        startActivity(
                            Intent(this, HomeActivity::class.java).apply {
                                putExtra("user_name", result.user?.name)
                                putExtra("user_email", result.user?.email)
                                putExtra("confidence", result.confidence)
                            }
                        )
                        finish()
                    },
                    onGoToRegister = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        embeddingHelper.close()
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
}