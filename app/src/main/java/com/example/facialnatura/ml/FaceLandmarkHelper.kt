package com.example.facialnatura.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.sqrt

/**
 * Extrae puntos faciales usando ML Kit (sin modelo externo).
 * Devuelve un vector normalizado de ~304 floats (152 puntos x,y)
 * que representa la geometría del rostro.
 */
class FaceLandmarkHelper {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
    )

    // Contornos en orden fijo para vector consistente
    private val contourOrder = listOf(
        FaceContour.FACE,
        FaceContour.LEFT_EYEBROW_TOP,
        FaceContour.LEFT_EYEBROW_BOTTOM,
        FaceContour.RIGHT_EYEBROW_TOP,
        FaceContour.RIGHT_EYEBROW_BOTTOM,
        FaceContour.LEFT_EYE,
        FaceContour.RIGHT_EYE,
        FaceContour.NOSE_BRIDGE,
        FaceContour.NOSE_BOTTOM,
        FaceContour.UPPER_LIP_TOP,
        FaceContour.UPPER_LIP_BOTTOM,
        FaceContour.LOWER_LIP_TOP,
        FaceContour.LOWER_LIP_BOTTOM
    )

    // Número esperado de puntos por contorno (para zero-padding si no se detectan)
    private val expectedPoints = mapOf(
        FaceContour.FACE to 36,
        FaceContour.LEFT_EYEBROW_TOP to 8,
        FaceContour.LEFT_EYEBROW_BOTTOM to 8,
        FaceContour.RIGHT_EYEBROW_TOP to 8,
        FaceContour.RIGHT_EYEBROW_BOTTOM to 8,
        FaceContour.LEFT_EYE to 16,
        FaceContour.RIGHT_EYE to 16,
        FaceContour.NOSE_BRIDGE to 4,
        FaceContour.NOSE_BOTTOM to 6,
        FaceContour.UPPER_LIP_TOP to 8,
        FaceContour.UPPER_LIP_BOTTOM to 8,
        FaceContour.LOWER_LIP_TOP to 8,
        FaceContour.LOWER_LIP_BOTTOM to 8
    )

    /**
     * Extrae un vector de puntos faciales normalizados del bitmap dado.
     * Retorna null si no se detecta ninguna cara.
     */
    suspend fun extractLandmarks(bitmap: Bitmap): FloatArray? =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isEmpty()) { cont.resume(null); return@addOnSuccessListener }

                    val face = faces[0]
                    val bounds = face.boundingBox
                    val w = bounds.width().toFloat().coerceAtLeast(1f)
                    val h = bounds.height().toFloat().coerceAtLeast(1f)
                    val cx = bounds.centerX().toFloat()
                    val cy = bounds.centerY().toFloat()

                    val points = mutableListOf<Float>()

                    for (type in contourOrder) {
                        val contour = face.getContour(type)
                        val expected = expectedPoints[type] ?: 8

                        if (contour != null && contour.points.isNotEmpty()) {
                            for (p in contour.points) {
                                // Normalizar: centrar en la cara y escalar por su tamaño
                                points.add((p.x - cx) / w)
                                points.add((p.y - cy) / h)
                            }
                            // Rellenar con ceros si hay menos puntos de lo esperado
                            val missing = (expected - contour.points.size).coerceAtLeast(0)
                            repeat(missing * 2) { points.add(0f) }
                        } else {
                            // Contorno no detectado: vector cero
                            repeat(expected * 2) { points.add(0f) }
                        }
                    }

                    cont.resume(points.toFloatArray())
                }
                .addOnFailureListener { cont.resume(null) }
        }

    /**
     * Similitud coseno entre dos vectores de puntos faciales.
     * Retorna 1.0 para el mismo rostro, valores menores para diferentes.
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val len = minOf(a.size, b.size)
        var dot = 0f; var normA = 0f; var normB = 0f
        for (i in 0 until len) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        normA = sqrt(normA); normB = sqrt(normB)
        return if (normA > 0f && normB > 0f) (dot / (normA * normB)).coerceIn(0f, 1f) else 0f
    }

    fun close() = detector.close()
}