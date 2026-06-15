package com.example.facialnatura.data.model

data class FaceLoginResult(
    val user: User? = null,
    val confidence: Float = 0f,
    val success: Boolean = false
)

