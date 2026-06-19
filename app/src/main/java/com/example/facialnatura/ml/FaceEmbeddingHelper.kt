package com.example.facialnatura.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.suspendCancellableCoroutine
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Genera embeddings faciales de 512 dimensiones usando FaceNet (TFLite).
 * El modelo debe estar en app/src/main/assets/facenet.tflite
 *
 * Flujo: detectar cara → alinear por ojos → recortar → escalar 160×160 →
 *        normalizar a [-1,1] → inferencia TFLite → L2-normalizar embedding
 */
class FaceEmbeddingHelper(private val context: Context) {

    private var interpreter: Interpreter? = null

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()
    )

    init {
        try {
            interpreter = Interpreter(loadModel(), Interpreter.Options().apply { numThreads = 4 })
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo cargar facenet.tflite: ${e.message}")
        }
    }

    private fun loadModel(): MappedByteBuffer {
        val afd = context.assets.openFd(MODEL_FILE)
        return FileInputStream(afd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength
        )
    }

    /**
     * Detecta la cara en el bitmap, la recorta y alinea, y devuelve el embedding L2-normalizado.
     * Retorna null si no se detecta ninguna cara o el modelo no está cargado.
     */
    suspend fun extractEmbedding(bitmap: Bitmap): FloatArray? {
        val interp = interpreter ?: return null
        val face   = detectFace(bitmap) ?: return null
        val crop   = cropAndAlign(bitmap, face) ?: return null
        val scaled = Bitmap.createScaledBitmap(crop, INPUT_SIZE, INPUT_SIZE, true)
        val input  = bitmapToTensor(scaled)
        val output = Array(1) { FloatArray(EMBEDDING_SIZE) }
        interp.run(input, output)
        return l2Normalize(output[0])
    }

    private suspend fun detectFace(bitmap: Bitmap): Face? =
        suspendCancellableCoroutine { cont ->
            detector.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { faces -> cont.resume(faces.firstOrNull()) }
                .addOnFailureListener { cont.resume(null) }
        }

    private fun cropAndAlign(src: Bitmap, face: Face): Bitmap? {
        val box      = face.boundingBox
        val leftEye  = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        // Rotar para nivelar los ojos (elimina inclinación de cabeza)
        val aligned = if (leftEye != null && rightEye != null) {
            val angle = atan2(
                (rightEye.y - leftEye.y).toDouble(),
                (rightEye.x - leftEye.x).toDouble()
            ).toFloat() * (180f / Math.PI.toFloat())
            val cx  = (leftEye.x + rightEye.x) / 2f
            val cy  = (leftEye.y + rightEye.y) / 2f
            val mat = Matrix().apply { postRotate(-angle, cx, cy) }
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, mat, true)
        } else src

        // Recortar con 50% de padding alrededor del bounding box
        val pad    = (maxOf(box.width(), box.height()) * 0.5f).toInt()
        val left   = (box.left   - pad).coerceAtLeast(0)
        val top    = (box.top    - pad).coerceAtLeast(0)
        val right  = (box.right  + pad).coerceAtMost(aligned.width)
        val bottom = (box.bottom + pad).coerceAtMost(aligned.height)
        val w = right - left; val h = bottom - top
        if (w <= 0 || h <= 0) return null
        return Bitmap.createBitmap(aligned, left, top, w, h)
    }

    // NHWC float32 [1, 160, 160, 3], normalizado a [-1, 1]
    private fun bitmapToTensor(bmp: Bitmap): Array<Array<Array<FloatArray>>> {
        val t = Array(1) { Array(INPUT_SIZE) { Array(INPUT_SIZE) { FloatArray(3) } } }
        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val px = bmp.getPixel(x, y)
                t[0][y][x][0] = ((px shr 16 and 0xFF) / 255f - 0.5f) * 2f
                t[0][y][x][1] = ((px shr 8  and 0xFF) / 255f - 0.5f) * 2f
                t[0][y][x][2] = ((px        and 0xFF) / 255f - 0.5f) * 2f
            }
        }
        return t
    }

    private fun l2Normalize(v: FloatArray): FloatArray {
        val norm = sqrt(v.sumOf { (it * it).toDouble() }.toFloat()).coerceAtLeast(1e-8f)
        return FloatArray(v.size) { v[it] / norm }
    }

    /** Distancia L2 entre embeddings L2-normalizados. Rango: [0, 2]. */
    fun l2Distance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in 0 until minOf(a.size, b.size)) { val d = a[i] - b[i]; sum += d * d }
        return sqrt(sum)
    }

    fun close() {
        interpreter?.close(); interpreter = null
        detector.close()
    }

    companion object {
        private const val TAG            = "FaceEmbeddingHelper"
        const val MODEL_FILE             = "facenet.tflite"
        const val INPUT_SIZE             = 160
        const val EMBEDDING_SIZE         = 512
        // Misma persona: L2 ≈ 0.3–0.7 | Distinta: L2 ≈ 1.0–1.6
        const val MATCH_THRESHOLD        = 0.80f
    }
}