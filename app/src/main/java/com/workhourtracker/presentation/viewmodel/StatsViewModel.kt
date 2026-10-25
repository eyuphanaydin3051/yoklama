package com.workhourtracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workhourtracker.data.remote.AuthRepository
import com.workhourtracker.data.remote.CompanyRepository
import com.workhourtracker.data.repository.AttendanceRepository
import com.workhourtracker.domain.model.AttendanceRecord
import com.workhourtracker.domain.model.MemberAttendanceStatus
import com.workhourtracker.domain.model.MonthlyStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class TeamTimeFilter(val label: String) {
    TODAY("Bugün"),
    WEEKLY("Bu Hafta"),
    MONTHLY("Bu Ay")
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: AttendanceRepository,
    private val authRepository: AuthRepository,
    private val companyRepository: CompanyRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _monthlyStats = MutableStateFlow<MonthlyStats?>(null)
    val monthlyStats = _monthlyStats.asStateFlow()

    private val _monthRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val monthRecords = _monthRecords.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _teamMembers = MutableStateFlow<List<MemberAttendanceStatus>>(emptyList())
    val teamMembers = _teamMembers.asStateFlow()

    init {
        observeStats()
    }

    val currentCompany = companyRepository.currentCompanyFlow

    private val _teamTimeFilter = MutableStateFlow(TeamTimeFilter.TODAY)
    val teamTimeFilter = _teamTimeFilter.asStateFlow()

    private val _selectedMember = MutableStateFlow<MemberAttendanceStatus?>(null)
    val selectedMember = _selectedMember.asStateFlow()

    private val _memberRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val memberRecords = _memberRecords.asStateFlow()

    private val _isMemberLoading = MutableStateFlow(false)
    val isMemberLoading = _isMemberLoading.asStateFlow()

    fun setTeamTimeFilter(filter: TeamTimeFilter) {
        _teamTimeFilter.value = filter
    }

    fun selectMember(member: MemberAttendanceStatus?) {
        _selectedMember.value = member
        if (member == null) {
            _memberRecords.value = emptyList()
            return
        }

        val myUid = authRepository.getCurrentUser()?.uid ?: "local_user"
        _isMemberLoading.value = true
        viewModelScope.launch {
            try {
                if (member.userId == myUid || member.userId == "local_user") {
                    val localList = repository.getAllRecords(myUid).first()
                    _memberRecords.value = localList
                } else {
                    val remoteList = companyRepository.fetchMemberAttendanceHistory(member.userId)
                    _memberRecords.value = remoteList
                }
            } catch (e: Exception) {
                _memberRecords.value = emptyList()
            } finally {
                _isMemberLoading.value = false
            }
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            authRepository.currentUserFlow.collectLatest { user ->
                val userId = user?.uid ?: "local_user"

                launch {
                    _selectedMonth.collectLatest { date ->
                        _isLoading.value = true
                        val startDate = LocalDate.of(date.year, date.monthValue, 1)
                        val endDate = startDate.plusMonths(1).minusDays(1)

                        launch {
                            repository.getRecordsInRange(userId, startDate, endDate).collectLatest { records ->
                                _monthRecords.value = records
                            }
                        }

                        repository.getMonthlyStats(userId, date.year, date.monthValue).collectLatest { stats ->
                            _monthlyStats.value = stats
                            _isLoading.value = false
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            companyRepository.currentCompanyFlow.collectLatest { company ->
                if (company != null) {
                    val myUid = authRepository.getCurrentUser()?.uid ?: "local_user"
                    val today = LocalDate.now()
                    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val startOfMonth = today.withDayOfMonth(1)

                    companyRepository.observeCompanyMembersAttendance(company.id).collectLatest { list ->
                        try {
                            val todayRecs = repository.getRecordsForDate(myUid, today).first()
                            val todayMins = todayRecs.sumOf { it.durationMinutes ?: 0L }
                            val weekRecs = repository.getRecordsInRange(myUid, startOfWeek, today).first()
                            val weekMins = weekRecs.sumOf { it.durationMinutes ?: 0L }
                            val monthRecs = repository.getRecordsInRange(myUid, startOfMonth, today).first()
                            val monthMins = monthRecs.sumOf { it.durationMinutes ?: 0L }

                            val updatedList = list.map { m ->
                                if (m.userId == myUid || m.userId == "local_user") {
                                    m.copy(
                                        todayTotalMinutes = if (todayMins > 0) todayMins else m.todayTotalMinutes,
                                        weeklyTotalMinutes = if (weekMins > 0) weekMins else m.weeklyTotalMinutes,
                                        monthlyTotalMinutes = if (monthMins > 0) monthMins else m.monthlyTotalMinutes
                                    )
                                } else m
                            }
                            _teamMembers.value = updatedList
                        } catch (e: Exception) {
                            _teamMembers.value = list
                        }
                    }
                } else {
                    _teamMembers.value = emptyList()
                }
            }
        }
    }

    fun setSelectedMonth(year: Int, month: Int) {
        _selectedMonth.value = LocalDate.of(year, month, 1)
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun getMonthLabel(): String = _selectedMonth.value.month.name

    fun getUserCompany(): String? = companyRepository.getCurrentCompany()?.name

    fun formatDuration(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours} saat ${mins} dk" else "${mins} dk"
    }

    fun getRecordsForDate(date: LocalDate): Flow<List<AttendanceRecord>> {
        val userId = authRepository.getCurrentUser()?.uid ?: "local_user"
        return repository.getRecordsForDate(userId, date)
    }

    fun deleteRecord(record: AttendanceRecord) {
        val userId = authRepository.getCurrentUser()?.uid ?: "local_user"
        viewModelScope.launch {
            try {
                repository.deleteRecord(userId, record)
            } catch (_: Exception) { }
        }
    }

    fun updateRecord(record: AttendanceRecord) {
        val userId = authRepository.getCurrentUser()?.uid ?: "local_user"
        viewModelScope.launch {
            try {
                repository.updateRecord(userId, record)
            } catch (_: Exception) { }
        }
    }

    fun getDailyDataForChart(): List<Pair<String, Long>> = _monthlyStats.value?.dailyBreakdown
        ?.toList()
        ?.sortedBy { it.first }
        ?.map { (date, stats) -> date.dayOfMonth.toString() to stats.totalMinutes } ?: emptyList()
}
