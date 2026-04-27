package com.focusfirst.data.remote

import android.util.Log
import com.focusfirst.data.db.SessionDao
import com.focusfirst.data.db.SessionEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirestoreRepository
 *
 * Handles cloud backup and restore of [SessionEntity] records.
 *
 * Auth strategy: **anonymous auth** — no personal data is ever collected.
 * Each install gets a stable, random UID from Firebase Auth. Signing in
 * again on a new device generates a new UID; use restore to pull data back.
 *
 * Data shape (Firestore):
 *   users/{uid}/sessions/{sessionId}
 *     startedAt:       Long   (epoch ms)
 *     durationSeconds: Int
 *     wasCompleted:    Boolean
 *     tag:             String
 *     syncedAt:        Long   (epoch ms, server-write time)
 */
@Singleton
class FirestoreRepository @Inject constructor(
    private val sessionDao: SessionDao,
) {
    private val db          = Firebase.firestore
    private val auth        = Firebase.auth
    private val crashlytics = Firebase.crashlytics

    /** Current anonymous UID, or null before [ensureAuthenticated] succeeds. */
    val userId: String?
        get() = auth.currentUser?.uid

    /**
     * Signs in anonymously if no user is currently authenticated.
     * Safe to call repeatedly — a no-op when already signed in.
     */
    suspend fun ensureAuthenticated() {
        if (auth.currentUser != null) return
        try {
            auth.signInAnonymously().await()
            Log.d(TAG, "Signed in anonymously: ${auth.currentUser?.uid}")
        } catch (e: Exception) {
            crashlytics.recordException(e)
            Log.e(TAG, "Anonymous sign-in failed: ${e.message}")
        }
    }

    /**
     * Uploads every session that has [SessionEntity.isSynced] == false.
     * On success, marks the record synced in Room so it won't be re-uploaded.
     */
    suspend fun syncSessionsToCloud() {
        val uid      = userId ?: return
        val unsynced = sessionDao.getUnsyncedSessions()
        Log.d(TAG, "Syncing ${unsynced.size} unsynced session(s)")

        unsynced.forEach { session ->
            try {
                db.collection("users")
                    .document(uid)
                    .collection("sessions")
                    .document(session.id.toString())
                    .set(
                        mapOf(
                            "startedAt"       to session.startedAt,
                            "durationSeconds" to session.durationSeconds,
                            "wasCompleted"    to session.wasCompleted,
                            "tag"             to session.tag,
                            "syncedAt"        to System.currentTimeMillis(),
                        )
                    ).await()

                sessionDao.markSynced(session.id)
                Log.d(TAG, "Synced session ${session.id}")
            } catch (e: Exception) {
                crashlytics.recordException(e)
                Log.e(TAG, "Sync failed for session ${session.id}: ${e.message}")
            }
        }
    }

    /**
     * Downloads all sessions stored in Firestore and inserts any that don't
     * already exist in the local Room database. Existing rows are left intact
     * (IGNORE conflict strategy).
     */
    suspend fun restoreFromCloud() {
        val uid = userId ?: return
        try {
            val snapshot = db
                .collection("users")
                .document(uid)
                .collection("sessions")
                .get()
                .await()

            Log.d(TAG, "Restoring ${snapshot.size()} session(s) from cloud")

            snapshot.documents.forEach { doc ->
                try {
                    val roomId = doc.id.toIntOrNull() ?: return@forEach
                    val session = SessionEntity(
                        id              = roomId,
                        startedAt       = doc.getLong("startedAt")                    ?: 0L,
                        durationSeconds = doc.getLong("durationSeconds")?.toInt()     ?: 0,
                        wasCompleted    = doc.getBoolean("wasCompleted")               ?: false,
                        tag             = doc.getString("tag")                         ?: "Focus",
                        isSynced        = true,
                    )
                    sessionDao.insertIfNotExists(session)
                } catch (e: Exception) {
                    crashlytics.recordException(e)
                    Log.e(TAG, "Failed to restore doc ${doc.id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            crashlytics.recordException(e)
            Log.e(TAG, "Restore from cloud failed: ${e.message}")
            throw e   // propagate so SyncViewModel can surface the error state
        }
    }

    /**
     * Deletes the entire user document tree from Firestore.
     * Called when the user explicitly requests data deletion.
     */
    suspend fun deleteCloudData() {
        val uid = userId ?: return
        try {
            db.collection("users").document(uid).delete().await()
            Log.d(TAG, "Cloud data deleted for uid=$uid")
        } catch (e: Exception) {
            crashlytics.recordException(e)
            Log.e(TAG, "Delete cloud data failed: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "FirestoreRepository"
    }
}
