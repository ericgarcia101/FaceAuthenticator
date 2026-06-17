package com.example.facialnatura

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.facialnatura.data.repository.UserRepository
import com.example.facialnatura.ml.FaceEmbeddingHelper
import com.example.facialnatura.viewModel.RegisterViewModel
import com.example.facialnatura.viewModel.RegisterViewModelFactory
import com.example.facialnatura.screen.RegisterScreen

class RegisterActivity : ComponentActivity() {

    private val embeddingHelper by lazy { FaceEmbeddingHelper(applicationContext) }
    private val repository by lazy { UserRepository(embeddingHelper) }
    private val viewModel: RegisterViewModel by viewModels {
        RegisterViewModelFactory(repository)
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (!granted) finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasCameraPermission()) requestPermission.launch(Manifest.permission.CAMERA)

        setContent {
            FaceLoginTheme {
                RegisterScreen(
                    viewModel = viewModel,
                    onBack    = { finish() },
                    onSuccess = { finish() }
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