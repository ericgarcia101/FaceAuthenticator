package com.example.facialnatura.data.repository

import android.graphics.Bitmap
import com.example.facialnatura.data.model.User
import com.example.facialnatura.ml.FaceLandmarkHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(
    private val landmarkHelper: FaceLandmarkHelper
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCol = firestore.collection("users")

    /**
     * Registra usuario: crea cuenta en Firebase Auth (contraseña segura)
     * y guarda los puntos faciales en Firestore vinculados al mismo UID.
     */
    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        bitmap: Bitmap
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val landmarks = landmarkHelper.extractLandmarks(bitmap)
                ?: return@withContext Result.failure(
                    Exception("No se detectó ninguna cara. Asegúrate de estar bien iluminado.")
                )

            // Crea el usuario en Firebase Auth (gestiona la contraseña de forma segura)
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return@withContext Result.failure(Exception("Error al crear cuenta."))

            // Guarda perfil + puntos faciales en Firestore (sin la contraseña)
            usersCol.document(uid).set(
                mapOf(
                    "name"       to name,
                    "email"      to email,
                    "facePoints" to landmarks.map { it.toDouble() },
                    "createdAt"  to System.currentTimeMillis()
                )
            ).await()

            Result.success(User(id = uid, name = name, email = email,
                facePoints = landmarks.map { it.toDouble() },
                createdAt = System.currentTimeMillis()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Login con email + contraseña via Firebase Auth.
     */
    suspend fun loginWithPassword(email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid
                    ?: return@withContext Result.failure(Exception("Error de autenticación."))

                val doc = usersCol.document(uid).get().await()
                val user = User(
                    id         = uid,
                    name       = doc.getString("name") ?: "",
                    email      = doc.getString("email") ?: email,
                    facePoints = (doc.get("facePoints") as? List<*>)
                                    ?.filterIsInstance<Double>() ?: emptyList(),
                    createdAt  = doc.getLong("createdAt") ?: 0L
                )
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Login facial: compara los puntos del bitmap contra todos los usuarios en Firestore.
     */
    suspend fun loginWithFace(bitmap: Bitmap): Result<Pair<User, Float>> =
        withContext(Dispatchers.IO) {
            try {
                val userLandmarks = landmarkHelper.extractLandmarks(bitmap)
                    ?: return@withContext Result.failure(
                        Exception("No se detectó ninguna cara.")
                    )

                val snapshot = usersCol.get().await()

                if (snapshot.isEmpty) {
                    return@withContext Result.failure(
                        Exception("No hay usuarios registrados. Regístrate primero.")
                    )
                }

                var bestMatch: User? = null
                var bestScore = 0f

                for (doc in snapshot.documents) {
                    val stored = (doc.get("facePoints") as? List<*>)
                        ?.filterIsInstance<Double>()
                        ?.map { it.toFloat() }
                        ?.toFloatArray()
                        ?: continue

                    val score = landmarkHelper.cosineSimilarity(userLandmarks, stored)
                    if (score > bestScore && score >= CONFIDENCE_THRESHOLD) {
                        bestScore = score
                        bestMatch = User(
                            id         = doc.id,
                            name       = doc.getString("name") ?: "",
                            email      = doc.getString("email") ?: "",
                            facePoints = stored.map { it.toDouble() },
                            createdAt  = doc.getLong("createdAt") ?: 0L
                        )
                    }
                }

                if (bestMatch != null) {
                    Result.success(bestMatch to bestScore)
                } else {
                    Result.failure(Exception("Cara no reconocida. Regístrate o intenta de nuevo con mejor iluminación."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.72f
    }
}