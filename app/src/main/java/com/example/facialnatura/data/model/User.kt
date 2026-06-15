package com.example.facialnatura.data.model

data class User(
    val id: String = "",           // Firebase Auth UID
    val name: String = "",
    val email: String = "",
    val facePoints: List<Double> = emptyList(),   // 304 landmarks normalizados
    val createdAt: Long = 0L
)

