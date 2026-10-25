package com.workhourtracker.data.repository

import com.workhourtracker.data.local.AttendanceDao
import com.workhourtracker.data.remote.FirestoreAttendanceSource
import com.workhourtracker.domain.model.AttendanceRecord
import com.workhourtracker.domain.model.DailyStats
import com.workhourtracker.domain.model.MonthlyStats
import com.workhourtracker.domain.model.RecordType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class AttendanceRepositoryImpl(
    private val dao: AttendanceDao,
    private val firestoreSource: FirestoreAttendanceSource,
    private val scope: CoroutineScope
) : AttendanceRepository {

    override fun getAllRecords(userId: String): Flow<List<AttendanceRecord>> = dao.getAllRecords(userId)

    override fun getRecordsForDate(userId: String, date: LocalDate): Flow<List<AttendanceRecord>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.getRecordsForDate(userId, startOfDay, endOfDay)
    }

    override fun getActiveRecord(userId: String): Flow<AttendanceRecord?> = dao.getActiveRecord(userId)

    override fun getRecordsInRange(userId: String, start: LocalDate, end: LocalDate): Flow<List<AttendanceRecord>> {
        val startTime = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTime = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.getRecordsInRange(userId, startTime, endTime)
    }

    override suspend fun checkIn(userId: String, type: RecordType): AttendanceRecord {
        val now = System.currentTimeMillis()
        val record = AttendanceRecord(
            userId = userId,
            checkInTime = now,
            type = type
        )
        val localId = dao.insert(record)
        val newRecord = record.copy(id = localId)
        scope.launch { syncRecord(userId, newRecord) }
        return newRecord
    }

    override suspend fun checkOut(userId: String, breakMinutes: Long): AttendanceRecord? {
        val activeRecord = dao.getActiveRecord(userId).first()
        return activeRecord?.let { record ->
            val now = System.currentTimeMillis()
            val updatedRecord = record.copy(
                checkOutTime = now,
                breakMinutes = breakMinutes,
                isSynced = false
            )
            dao.update(updatedRecord)
            scope.launch { syncRecord(userId, updatedRecord) }
            updatedRecord
        }
    }

    override suspend fun addRetroactiveRecord(userId: String, checkInTime: Long, checkOutTime: Long?, breakMinutes: Long): AttendanceRecord {
        val localRecords = dao.getAllRecords(userId).first()
        val existing = localRecords.find { Math.abs(it.checkInTime - checkInTime) < 1000L }
        if (existing != null) {
            val updated = existing.copy(checkOutTime = checkOutTime, breakMinutes = breakMinutes, isSynced = false)
            dao.update(updated)
            scope.launch { syncRecord(userId, updated) }
            return updated
        }

        val record = AttendanceRecord(
            userId = userId,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime,
            type = RecordType.RETROACTIVE,
            breakMinutes = breakMinutes
        )
        val localId = dao.insert(record)
        val newRecord = record.copy(id = localId)
        scope.launch { syncRecord(userId, newRecord) }
        return newRecord
    }

    override suspend fun updateRecord(userId: String, record: AttendanceRecord) {
        dao.update(record.copy(isSynced = false))
        scope.launch { syncRecord(userId, record.copy(isSynced = false)) }
    }

    override suspend fun deleteRecord(userId: String, record: AttendanceRecord) {
        try {
            dao.delete(record)
            record.firebaseId?.let { firebaseId ->
                scope.launch {
                    try {
                        firestoreSource.deleteRecord(userId, firebaseId)
                    } catch (_: Exception) { }
                }
            }
        } catch (_: Exception) { }
    }

    override suspend fun syncAllUnsynced(userId: String) {
        val unsynced = dao.getUnsyncedRecords(userId).first()
        if (unsynced.isNotEmpty()) {
            try {
                firestoreSource.uploadRecords(userId, unsynced)
                unsynced.forEach { dao.update(it.copy(isSynced = true)) }
            } catch (e: Exception) {
                // Will retry on network reconnect
            }
        }
    }

    override suspend fun pullRemoteRecords(userId: String) {
        try {
            val remoteRecords = firestoreSource.getAllRemoteRecords(userId)
            val localRecords = dao.getAllRecords(userId).first()

            remoteRecords.forEach { remote ->
                val matchingLocal = localRecords.find { 
                    (it.firebaseId != null && it.firebaseId == remote.firebaseId) ||
                    (Math.abs(it.checkInTime - remote.checkInTime) < 2000L)
                }
                if (matchingLocal != null) {
                    if (matchingLocal.firebaseId == null && remote.firebaseId != null) {
                        dao.update(matchingLocal.copy(firebaseId = remote.firebaseId, isSynced = true))
                    }
                } else {
                    dao.insert(remote.copy(userId = userId, isSynced = true))
                }
            }
        } catch (_: Exception) { }
    }

    private suspend fun syncRecord(userId: String, record: AttendanceRecord) {
        try {
            val firebaseId = firestoreSource.uploadRecord(userId, record)
            if (firebaseId != null) {
                dao.update(record.copy(firebaseId = firebaseId, isSynced = true))
            }
        } catch (e: Exception) {
            // Sync failed, will retry later via WorkManager
        }
    }

    override fun getDailyStats(userId: String, date: LocalDate): Flow<DailyStats> =
        getRecordsForDate(userId, date).map { records ->
            val completedRecords = records.filter { it.checkOutTime != null }
            val totalMinutes = completedRecords.sumOf { it.durationMinutes ?: 0L }
            val firstCheckIn = records.minByOrNull { it.checkInTime }?.checkInDateTime
            val lastCheckOut = completedRecords.maxByOrNull { it.checkOutTime ?: 0L }?.checkOutDateTime
            DailyStats(date, totalMinutes, records.size, firstCheckIn, lastCheckOut)
        }

    override fun getMonthlyStats(userId: String, year: Int, month: Int): Flow<MonthlyStats> {
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.plusMonths(1).minusDays(1)
        return getRecordsInRange(userId, startDate, endDate).map { records ->
            val dailyMap = mutableMapOf<LocalDate, DailyStats>()
            val daysInMonth = startDate.lengthOfMonth()

            (1..daysInMonth).forEach { day ->
                val date = LocalDate.of(year, month, day)
                val dayRecords = records.filter { it.checkInDate == date }
                val completedRecords = dayRecords.filter { it.checkOutTime != null }
                val totalMinutes = completedRecords.sumOf { it.durationMinutes ?: 0L }
                val firstCheckIn = dayRecords.minByOrNull { it.checkInTime }?.checkInDateTime
                val lastCheckOut = completedRecords.maxByOrNull { it.checkOutTime ?: 0L }?.checkOutDateTime
                dailyMap[date] = DailyStats(date, totalMinutes, dayRecords.size, firstCheckIn, lastCheckOut)
            }

            val totalMinutes = dailyMap.values.sumOf { it.totalMinutes }
            val daysWithRecords = dailyMap.values.count { it.recordCount > 0 }
            val average = if (daysWithRecords > 0) totalMinutes.toDouble() / daysWithRecords else 0.0

            MonthlyStats(year, month, totalMinutes, daysWithRecords, average, dailyMap)
        }
    }
}
