package com.workhourtracker.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val userId: String,
    val checkInTime: Long,
    var checkOutTime: Long? = null,
    val type: RecordType = RecordType.MANUAL,
    var isSynced: Boolean = false,
    var firebaseId: String? = null,
    var userName: String? = null,
    var companyId: String? = null,
    var breakMinutes: Long = 0L
) {
    val checkInDateTime: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(checkInTime), ZoneId.systemDefault())

    val checkOutDateTime: LocalDateTime?
        get() = checkOutTime?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }

    val checkInDate: LocalDate
        get() = checkInDateTime.toLocalDate()

    val durationMinutes: Long?
        get() = checkOutTime?.let {
            val total = (it - checkInTime) / (1000 * 60)
            (total - breakMinutes).coerceAtLeast(0L)
        }

    val isActive: Boolean
        get() = checkOutTime == null
}

enum class RecordType {
    MANUAL,     // Kullanıcı manuel girdi
    AUTO,       // Otomatik (geofence vb.)
    RETROACTIVE // Geriye dönük giriş
}

data class DailyStats(
    val date: LocalDate,
    val totalMinutes: Long,
    val recordCount: Int,
    val firstCheckIn: LocalDateTime?,
    val lastCheckOut: LocalDateTime?
)

data class MonthlyStats(
    val year: Int,
    val month: Int,
    val totalMinutes: Long,
    val totalDays: Int,
    val averageDailyMinutes: Double,
    val dailyBreakdown: Map<LocalDate, DailyStats>
)

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val companyId: String? = null,
    val companyName: String? = null,
    val role: String = "member" // "owner", "admin", "member"
)

data class Company(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val creatorId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class MemberAttendanceStatus(
    val userId: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isCurrentlyActive: Boolean = false,
    val checkInTime: Long? = null,
    val todayTotalMinutes: Long = 0L,
    val weeklyTotalMinutes: Long = 0L,
    val monthlyTotalMinutes: Long = 0L,
    val role: String = "member"
)
