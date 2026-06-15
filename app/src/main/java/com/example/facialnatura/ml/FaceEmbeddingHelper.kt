package com.example.facialnatura.ml

import android.content.Context
import android.graphics.Bitmap

// Stub reservado para uso futuro. La app usa FaceLandmarkHelper (ML Kit) para reconocimiento facial.
class FaceEmbeddingHelper(private val context: Context) {
    fun initialize() {}
    fun getEmbeddings(bitmap: Bitmap): FloatArray = FloatArray(EMBEDDING_SIZE)
    fun close() {}

    companion object {
        private const val EMBEDDING_SIZE = 128
    }
}