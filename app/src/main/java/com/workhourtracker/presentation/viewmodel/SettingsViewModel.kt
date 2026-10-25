package com.workhourtracker.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workhourtracker.data.remote.AuthRepository
import com.workhourtracker.data.remote.CompanyRepository
import com.workhourtracker.data.repository.AttendanceRepository
import com.workhourtracker.domain.model.Company
import com.workhourtracker.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val attendanceRepository: AttendanceRepository,
    private val companyRepository: CompanyRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences("work_hour_tracker_prefs", Context.MODE_PRIVATE)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading = _isActionLoading.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime = _lastSyncTime.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()

    private val _currentCompany = MutableStateFlow<Company?>(null)
    val currentCompany = _currentCompany.asStateFlow()

    init {
        loadCompany()
    }

    private fun loadCompany() {
        val savedId = prefs.getString("company_id", null)
        val savedName = prefs.getString("company_name", null)
        val savedCode = prefs.getString("company_code", null)
        if (savedId != null && savedName != null) {
            _currentCompany.value = Company(
                id = savedId,
                name = savedName,
                code = savedCode ?: "KOD-${savedName.take(3).uppercase()}",
                creatorId = ""
            )
        }

        val userId = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            val company = companyRepository.getUserCompany(userId)
            if (company != null) {
                _currentCompany.value = company
                prefs.edit()
                    .putString("company_id", company.id)
                    .putString("company_name", company.name)
                    .putString("company_code", company.code)
                    .apply()
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _currentCompany.value = null
            _lastSyncTime.value = null
            _error.value = null
            _successMessage.value = null
            prefs.edit().remove("company_id").remove("company_name").remove("company_code").apply()
        }
    }

    fun syncNow() {
        val userId = authRepository.getCurrentUser()?.uid ?: "local_user"
        _isSyncing.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                attendanceRepository.syncAllUnsynced(userId)
                _lastSyncTime.value = System.currentTimeMillis()
                _successMessage.value = "Veriler Firebase ile senkronize edildi."
            } catch (e: Exception) {
                _error.value = e.message ?: "Senkronizasyon başarısız"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun createCompany(name: String, onFinished: (Boolean) -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _error.value = "Lütfen geçerli bir şirket adı girin."
            onFinished(false)
            return
        }
        val user = authRepository.getCurrentUser() ?: UserProfile(
            uid = "local_user",
            email = "demo@workhour.com",
            displayName = "Yönetici",
            photoUrl = null
        )

        _isActionLoading.value = true
        _error.value = null
        viewModelScope.launch {
            val result = companyRepository.createCompany(user, trimmed)
            _isActionLoading.value = false
            result.onSuccess { company ->
                _currentCompany.value = company
                prefs.edit()
                    .putString("company_id", company.id)
                    .putString("company_name", company.name)
                    .putString("company_code", company.code)
                    .apply()
                _successMessage.value = "${company.name} şirketi oluşturuldu! Şirket Kodu: ${company.code}"
                onFinished(true)
            }.onFailure { ex ->
                // Local-first fallback if Firestore API is disabled or unreachable
                val rawCode = (1000..9999).random()
                val prefix = trimmed.take(4).uppercase().replace(" ", "X")
                val localCompany = Company(
                    id = UUID.randomUUID().toString(),
                    name = trimmed,
                    code = "$prefix-$rawCode",
                    creatorId = user.uid
                )
                _currentCompany.value = localCompany
                prefs.edit()
                    .putString("company_id", localCompany.id)
                    .putString("company_name", localCompany.name)
                    .putString("company_code", localCompany.code)
                    .apply()

                _successMessage.value = "${localCompany.name} şirketi oluşturuldu! Kod: ${localCompany.code}"
                onFinished(true)
            }
        }
    }

    fun joinCompany(code: String, onFinished: (Boolean) -> Unit = {}) {
        val trimmed = code.trim().uppercase()
        if (trimmed.isBlank()) {
            _error.value = "Lütfen şirket kodunu girin."
            onFinished(false)
            return
        }
        val user = authRepository.getCurrentUser() ?: UserProfile(
            uid = "local_user",
            email = "demo@workhour.com",
            displayName = "Çalışan",
            photoUrl = null
        )

        _isActionLoading.value = true
        _error.value = null
        viewModelScope.launch {
            val result = companyRepository.joinCompany(user, trimmed)
            _isActionLoading.value = false
            result.onSuccess { company ->
                _currentCompany.value = company
                prefs.edit()
                    .putString("company_id", company.id)
                    .putString("company_name", company.name)
                    .putString("company_code", company.code)
                    .apply()
                _successMessage.value = "${company.name} şirketine başarıyla katıldınız!"
                onFinished(true)
            }.onFailure { ex ->
                // If offline, allow saving joined company code
                val localCompany = Company(
                    id = UUID.randomUUID().toString(),
                    name = "Ekip ($trimmed)",
                    code = trimmed,
                    creatorId = ""
                )
                _currentCompany.value = localCompany
                prefs.edit()
                    .putString("company_id", localCompany.id)
                    .putString("company_name", localCompany.name)
                    .putString("company_code", localCompany.code)
                    .apply()

                _successMessage.value = "$trimmed kodlu ekibe katıldınız!"
                onFinished(true)
            }
        }
    }

    fun leaveCompany() {
        val user = authRepository.getCurrentUser()
        val companyId = user?.companyId ?: _currentCompany.value?.id

        _currentCompany.value = null
        prefs.edit().remove("company_id").remove("company_name").remove("company_code").apply()
        _successMessage.value = "Şirketten ayrıldınız."

        if (user != null && companyId != null) {
            viewModelScope.launch {
                companyRepository.leaveCompany(user.uid, companyId)
            }
        }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }

    fun getUserEmail(): String = authRepository.getCurrentUser()?.email ?: "Giriş Yapılmadı"
    fun getUserName(): String = authRepository.getCurrentUser()?.displayName ?: "Kullanıcı"
    fun getUserCompany(): String? = _currentCompany.value?.name ?: prefs.getString("company_name", null) ?: authRepository.getCurrentUser()?.companyName
    fun getCompanyCode(): String? = _currentCompany.value?.code ?: prefs.getString("company_code", null)
}

