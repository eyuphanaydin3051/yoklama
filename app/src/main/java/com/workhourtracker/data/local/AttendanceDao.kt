package com.workhourtracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.workhourtracker.domain.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AttendanceRecord>)

    @Update
    suspend fun update(record: AttendanceRecord)

    @Delete
    suspend fun delete(record: AttendanceRecord)

    @Query("SELECT * FROM attendance_records WHERE userId = :userId ORDER BY checkInTime DESC")
    fun getAllRecords(userId: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE userId = :userId AND isSynced = 0 ORDER BY checkInTime DESC")
    fun getUnsyncedRecords(userId: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE userId = :userId AND checkInTime >= :startTime AND checkInTime <= :endTime ORDER BY checkInTime DESC")
    fun getRecordsInRange(userId: String, startTime: Long, endTime: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE userId = :userId AND checkInTime >= :startOfDay AND checkInTime < :endOfDay ORDER BY checkInTime")
    fun getRecordsForDate(userId: String, startOfDay: Long, endOfDay: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE userId = :userId AND checkOutTime IS NULL ORDER BY checkInTime DESC LIMIT 1")
    fun getActiveRecord(userId: String): Flow<AttendanceRecord?>

    @Query("SELECT * FROM attendance_records WHERE userId = :userId AND id = :recordId")
    suspend fun getRecordById(userId: String, recordId: Long): AttendanceRecord?

    @Query("DELETE FROM attendance_records WHERE userId = :userId AND id = :recordId")
    suspend fun deleteRecord(userId: String, recordId: Long)

    @Query("SELECT COUNT(*) FROM attendance_records WHERE userId = :userId AND checkInTime >= :startOfDay AND checkInTime < :endOfDay")
    suspend fun getRecordCountForDate(userId: String, startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT SUM(((checkOutTime - checkInTime) / 60000) - breakMinutes) FROM attendance_records WHERE userId = :userId AND checkOutTime IS NOT NULL AND checkInTime >= :startTime AND checkInTime <= :endTime")
    suspend fun getTotalMinutesInRange(userId: String, startTime: Long, endTime: Long): Long?
}
