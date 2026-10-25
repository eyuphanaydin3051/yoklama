package com.workhourtracker.data.repository

import com.workhourtracker.domain.model.AttendanceRecord
import com.workhourtracker.domain.model.DailyStats
import com.workhourtracker.domain.model.MonthlyStats
import com.workhourtracker.domain.model.RecordType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AttendanceRepository {
    fun getAllRecords(userId: String): Flow<List<AttendanceRecord>>
    fun getRecordsForDate(userId: String, date: LocalDate): Flow<List<AttendanceRecord>>
    fun getActiveRecord(userId: String): Flow<AttendanceRecord?>
    fun getRecordsInRange(userId: String, start: LocalDate, end: LocalDate): Flow<List<AttendanceRecord>>

    suspend fun checkIn(userId: String, type: RecordType = RecordType.MANUAL): AttendanceRecord
    suspend fun checkOut(userId: String, breakMinutes: Long = 0L): AttendanceRecord?
    suspend fun addRetroactiveRecord(userId: String, checkInTime: Long, checkOutTime: Long?, breakMinutes: Long = 0L): AttendanceRecord
    suspend fun updateRecord(userId: String, record: AttendanceRecord)
    suspend fun deleteRecord(userId: String, record: AttendanceRecord)
    suspend fun syncAllUnsynced(userId: String)
    suspend fun pullRemoteRecords(userId: String)

    fun getDailyStats(userId: String, date: LocalDate): Flow<DailyStats>
    fun getMonthlyStats(userId: String, year: Int, month: Int): Flow<MonthlyStats>
}
