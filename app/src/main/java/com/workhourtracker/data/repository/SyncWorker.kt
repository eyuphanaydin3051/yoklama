package com.workhourtracker.data.repository

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.workhourtracker.data.local.AppDatabase
import com.workhourtracker.data.remote.AuthRepository
import com.workhourtracker.data.remote.FirestoreAttendanceSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val userId = AuthRepository.getInstance(applicationContext).getCurrentUser()?.uid
            ?: return@withContext Result.success()

        val dao = AppDatabase.getDatabase(applicationContext).attendanceDao()
        val firestoreSource = FirestoreAttendanceSource.getInstance()

        try {
            val unsyncedRecords = dao.getUnsyncedRecords(userId).first()
            if (unsyncedRecords.isNotEmpty()) {
                firestoreSource.uploadRecords(userId, unsyncedRecords)
                unsyncedRecords.forEach { dao.update(it.copy(isSynced = true)) }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "sync_attendance_work"
        const val TAG = "attendance_sync"
    }
}
