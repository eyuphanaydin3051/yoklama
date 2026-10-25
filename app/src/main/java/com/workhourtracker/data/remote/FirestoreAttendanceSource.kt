package com.workhourtracker.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.workhourtracker.domain.model.AttendanceRecord
import com.workhourtracker.domain.model.RecordType
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirestoreAttendanceSource private constructor(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        @Volatile private var INSTANCE: FirestoreAttendanceSource? = null
        fun getInstance(): FirestoreAttendanceSource = INSTANCE ?: synchronized(this) {
            FirestoreAttendanceSource().also { INSTANCE = it }
        }
    }

    private fun getUserCollection(userId: String) =
        firestore.collection("users").document(userId).collection("attendance")

    suspend fun uploadRecord(userId: String, record: AttendanceRecord): String? {
        return try {
            val data = recordToMap(record)
            val docRef = if (record.firebaseId.isNullOrBlank()) {
                getUserCollection(userId).add(data).await()
            } else {
                val ref = getUserCollection(userId).document(record.firebaseId!!)
                ref.set(data).await()
                ref
            }
            docRef.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadRecords(userId: String, records: List<AttendanceRecord>) {
        try {
            val batch = firestore.batch()
            records.forEach { record ->
                val docRef = if (record.firebaseId.isNullOrBlank()) {
                    getUserCollection(userId).document()
                } else {
                    getUserCollection(userId).document(record.firebaseId!!)
                }
                batch.set(docRef, recordToMap(record))
            }
            batch.commit().await()
        } catch (_: Exception) { }
    }

    suspend fun deleteRecord(userId: String, firebaseId: String) {
        try {
            getUserCollection(userId).document(firebaseId).delete().await()
        } catch (_: Exception) { }
    }

    suspend fun getAllRemoteRecords(userId: String): List<AttendanceRecord> {
        return try {
            val snapshot = getUserCollection(userId)
                .orderBy("checkInTime", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc -> doc.data?.let { mapToRecord(doc.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun streamRecords(userId: String) = flow<Result<List<AttendanceRecord>>> {
        try {
            val snapshot = getUserCollection(userId)
                .orderBy("checkInTime", Query.Direction.DESCENDING)
                .get()
                .await()
            emit(Result.success(snapshot.documents.mapNotNull { doc -> doc.data?.let { mapToRecord(doc.id, it) } }))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun streamRecordsInRange(userId: String, startTime: Long, endTime: Long) = flow<Result<List<AttendanceRecord>>> {
        try {
            val snapshot = getUserCollection(userId)
                .whereGreaterThanOrEqualTo("checkInTime", startTime)
                .whereLessThanOrEqualTo("checkInTime", endTime)
                .orderBy("checkInTime", Query.Direction.DESCENDING)
                .get()
                .await()
            emit(Result.success(snapshot.documents.mapNotNull { doc -> doc.data?.let { mapToRecord(doc.id, it) } }))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private fun recordToMap(record: AttendanceRecord): Map<String, Any?> = mapOf(
        "userId" to record.userId,
        "checkInTime" to record.checkInTime,
        "checkOutTime" to record.checkOutTime,
        "type" to record.type.name,
        "breakMinutes" to record.breakMinutes,
        "isSynced" to true
    )

    private fun mapToRecord(firebaseId: String, data: Map<String, Any>): AttendanceRecord {
        return AttendanceRecord(
            id = 0,
            userId = data["userId"] as? String ?: "",
            checkInTime = (data["checkInTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            checkOutTime = (data["checkOutTime"] as? Number)?.toLong(),
            type = (data["type"] as? String)?.let { runCatching { RecordType.valueOf(it) }.getOrNull() } ?: RecordType.MANUAL,
            isSynced = true,
            firebaseId = firebaseId,
            breakMinutes = (data["breakMinutes"] as? Number)?.toLong() ?: 0L
        )
    }
}
