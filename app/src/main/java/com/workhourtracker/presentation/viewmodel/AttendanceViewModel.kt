package com.workhourtracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workhourtracker.data.remote.AuthRepository
import com.workhourtracker.data.remote.CompanyRepository
import com.workhourtracker.data.repository.AttendanceRepository
import com.workhourtracker.domain.model.AttendanceRecord
import com.workhourtracker.domain.model.DailyStats
import com.workhourtracker.domain.model.MemberAttendanceStatus
import com.workhourtracker.domain.model.RecordType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val repository: AttendanceRepository,
    private val authRepository: AuthRepository,
    private val companyRepository: CompanyRepository
) : ViewModel() {

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate = _currentDate.asStateFlow()

    private val _records = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val records = _records.asStateFlow()

    private val _activeRecord = MutableStateFlow<AttendanceRecord?>(null)
    val activeRecord = _activeRecord.asStateFlow()

    private val _dailyStats = MutableStateFlow<DailyStats?>(null)
    val dailyStats = _dailyStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    // Company / Team members live status
    private val _teamMembers = MutableStateFlow<List<MemberAttendanceStatus>>(emptyList())
    val teamMembers = _teamMembers.asStateFlow()

    init {
        observeData()
    }

    fun getEffectiveUserId(): String =
        authRepository.getCurrentUser()?.uid ?: "local_user"

    private fun observeData() {
        viewModelScope.launch {
            authRepository.currentUserFlow.collectLatest { user ->
                val userId = user?.uid ?: "local_user"

                launch {
                    repository.getActiveRecord(userId)
                        .distinctUntilChanged()
                        .collectLatest { active ->
                            _activeRecord.value = active
                            syncLiveStatusToCompany(userId, active)
                        }
                }

                launch {
                    _currentDate.collectLatest { date ->
                        repository.getRecordsForDate(userId, date).collectLatest {
                            _records.value = it
                        }
                    }
                }

                launch {
                    _currentDate.collectLatest { date ->
                        repository.getDailyStats(userId, date).collectLatest {
                            _dailyStats.value = it
                        }
                    }
                }

                val companyId = user?.companyId
                if (companyId != null) {
                    launch {
                        companyRepository.observeCompanyMembersAttendance(companyId).collectLatest {
                            _teamMembers.value = it
                        }
                    }
                } else {
                    _teamMembers.value = emptyList()
                }
            }
        }
    }

    fun setCurrentDate(date: LocalDate) {
        _currentDate.value = date
    }

    fun onCheckInClick() {
        val userId = getEffectiveUserId()
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val record = repository.checkIn(userId)
                syncLiveStatusToCompany(userId, record)
                _error.value = null
                _userMessage.value = "Mesai başlatıldı!"
            } catch (e: Exception) {
                _error.value = e.message ?: "Giriş yapılamadı"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onCheckOutClick(breakMinutes: Long = 0L) {
        val userId = getEffectiveUserId()
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.checkOut(userId, breakMinutes)
                syncLiveStatusToCompany(userId, null)
                _error.value = null
                _userMessage.value = if (breakMinutes > 0) "Mesai sonlandırıldı (${breakMinutes} dk öğle molası düşüldü)!" else "Mesai sonlandırıldı!"
            } catch (e: Exception) {
                _error.value = e.message ?: "Çıkış yapılamadı"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onRetroactiveEntry(checkInTime: Long, checkOutTime: Long?, targetDate: LocalDate, breakMinutes: Long = 0L) {
        val userId = getEffectiveUserId()
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.addRetroactiveRecord(userId, checkInTime, checkOutTime, breakMinutes)
                syncLiveStatusToCompany(userId, _activeRecord.value)
                // Switch view to target date so user sees the newly saved record immediately
                _currentDate.value = targetDate
                _error.value = null
                _userMessage.value = "Geçmiş kayıt başarıyla eklendi!"
            } catch (e: Exception) {
                _error.value = e.message ?: "Kayıt eklenemedi"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onDeleteRecord(record: AttendanceRecord) {
        val userId = getEffectiveUserId()
        viewModelScope.launch {
            try {
                repository.deleteRecord(userId, record)
                syncLiveStatusToCompany(userId, _activeRecord.value)
                _userMessage.value = "Kayıt silindi."
            } catch (e: Exception) {
                _userMessage.value = "Kayıt silindi."
            }
        }
    }

    fun onUpdateRecord(record: AttendanceRecord) {
        val userId = getEffectiveUserId()
        viewModelScope.launch {
            repository.updateRecord(userId, record)
            syncLiveStatusToCompany(userId, _activeRecord.value)
        }
    }

    private suspend fun syncLiveStatusToCompany(userId: String, active: AttendanceRecord?) {
        try {
            val company = companyRepository.getCurrentCompany()
            val companyId = company?.id
            if (companyId != null) {
                val today = LocalDate.now()
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val startOfMonth = today.withDayOfMonth(1)

                val todayRecs = repository.getRecordsForDate(userId, today).first()
                val todayMins = todayRecs.sumOf { it.durationMinutes ?: 0L }

                val weeklyRecords = repository.getRecordsInRange(userId, startOfWeek, today).first()
                val weeklyMinutes = weeklyRecords.sumOf { it.durationMinutes ?: 0L }

                val monthlyRecords = repository.getRecordsInRange(userId, startOfMonth, today).first()
                val monthlyMinutes = monthlyRecords.sumOf { it.durationMinutes ?: 0L }

                companyRepository.updateMemberLiveStatus(
                    companyId = companyId,
                    userId = userId,
                    isActive = active != null,
                    checkInTime = active?.checkInTime,
                    todayTotalMinutes = todayMins,
                    weeklyTotalMinutes = weeklyMinutes,
                    monthlyTotalMinutes = monthlyMinutes
                )
            }
        } catch (_: Exception) {}
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun syncNow() {
        val userId = getEffectiveUserId()
        viewModelScope.launch {
            repository.syncAllUnsynced(userId)
        }
    }

    val isCheckedIn: Boolean
        get() = _activeRecord.value?.isActive == true

    val checkInButtonText: String
        get() = if (isCheckedIn) "Çıkış Yap" else "Giriş Yap"

    fun formatDuration(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours}s ${mins}dk" else "${mins}dk"
    }

    fun getTodayTotalMinutes(): Long = _dailyStats.value?.totalMinutes ?: 0L
}
