package com.example.facialnatura.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

// Kept only for CameraManager face-presence detection in the preview.
// All identity recognition is handled by FaceEmbeddingHelper.
class FaceLandmarkHelper {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.15f)
            .build()
    )

    fun detectFaces(bitmap: Bitmap, onResult: (count: Int) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
            .addOnSuccessListener { faces -> onResult(faces.size) }
            .addOnFailureListener { onResult(0) }
    }

    fun close() = detector.close()
}