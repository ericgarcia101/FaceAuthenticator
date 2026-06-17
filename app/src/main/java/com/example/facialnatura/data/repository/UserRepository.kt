package com.example.facialnatura.data.repository

import android.graphics.Bitmap
import com.example.facialnatura.data.model.User
import com.example.facialnatura.ml.FaceEmbeddingHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(
    private val embeddingHelper: FaceEmbeddingHelper
) {
    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCol  = firestore.collection("users")

    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        bitmap: Bitmap
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val embedding = embeddingHelper.extractEmbedding(bitmap)
                ?: return@withContext Result.failure(
                    Exception("No se detectó ninguna cara. Asegúrate de que tu cara esté bien iluminada y centrada.")
                )

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return@withContext Result.failure(Exception("Error al crear cuenta."))

            usersCol.document(uid).set(
                mapOf(
                    "name"        to name,
                    "email"       to email,
                    "facePoints"  to embedding.map { it.toDouble() },
                    "createdAt"   to System.currentTimeMillis()
                )
            ).await()

            Result.success(
                User(
                    id         = uid,
                    name       = name,
                    email      = email,
                    facePoints = embedding.map { it.toDouble() },
                    createdAt  = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithPassword(email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid
                    ?: return@withContext Result.failure(Exception("Error de autenticación."))

                val doc  = usersCol.document(uid).get().await()
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
     * Login facial usando FaceNet embeddings.
     * Compara el embedding del bitmap de entrada contra todos los usuarios
     * mediante distancia L2 entre vectores L2-normalizados.
     * Misma persona: L2 ≈ 0.3–0.7 | Distinta: L2 ≈ 1.0+
     */
    suspend fun loginWithFace(bitmap: Bitmap): Result<Pair<User, Float>> =
        withContext(Dispatchers.IO) {
            try {
                val queryEmbedding = embeddingHelper.extractEmbedding(bitmap)
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
                var bestDist  = Float.MAX_VALUE

                for (doc in snapshot.documents) {
                    val stored = (doc.get("facePoints") as? List<*>)
                        ?.filterIsInstance<Double>()
                        ?.map { it.toFloat() }
                        ?.toFloatArray()
                        ?: continue

                    if (stored.size != FaceEmbeddingHelper.EMBEDDING_SIZE) continue

                    val dist = embeddingHelper.l2Distance(queryEmbedding, stored)
                    if (dist < bestDist) {
                        bestDist = dist
                        bestMatch = User(
                            id         = doc.id,
                            name       = doc.getString("name") ?: "",
                            email      = doc.getString("email") ?: "",
                            facePoints = stored.map { it.toDouble() },
                            createdAt  = doc.getLong("createdAt") ?: 0L
                        )
                    }
                }

                if (bestMatch != null && bestDist <= FaceEmbeddingHelper.MATCH_THRESHOLD) {
                    // Convertir distancia a puntuación 0-1 para UI (menor distancia = mayor confianza)
                    val confidence = ((2f - bestDist) / 2f).coerceIn(0f, 1f)
                    Result.success(bestMatch to confidence)
                } else {
                    Result.failure(
                        Exception("Cara no reconocida. La distancia más cercana fue ${String.format("%.2f", bestDist)} (umbral: ${FaceEmbeddingHelper.MATCH_THRESHOLD}).")
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}