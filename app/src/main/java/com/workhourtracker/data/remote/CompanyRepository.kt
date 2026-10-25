package com.workhourtracker.data.remote

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.workhourtracker.domain.model.Company
import com.workhourtracker.domain.model.MemberAttendanceStatus
import com.workhourtracker.domain.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CompanyRepository private constructor(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val prefs = context?.getSharedPreferences("work_hour_tracker_prefs", Context.MODE_PRIVATE)

    private val _currentCompanyFlow = MutableStateFlow<Company?>(loadLocalCompany())
    val currentCompanyFlow: StateFlow<Company?> = _currentCompanyFlow.asStateFlow()

    private fun loadLocalCompany(): Company? {
        if (prefs == null) return null
        val id = prefs.getString("company_id", null) ?: return null
        val name = prefs.getString("company_name", null) ?: return null
        val code = prefs.getString("company_code", "") ?: ""
        return Company(id = id, name = name, code = code, creatorId = "")
    }

    fun saveLocalCompany(company: Company?) {
        _currentCompanyFlow.value = company
        if (prefs != null) {
            if (company != null) {
                prefs.edit()
                    .putString("company_id", company.id)
                    .putString("company_name", company.name)
                    .putString("company_code", company.code)
                    .apply()
            } else {
                prefs.edit()
                    .remove("company_id")
                    .remove("company_name")
                    .remove("company_code")
                    .apply()
            }
        }
    }

    fun getCurrentCompany(): Company? = _currentCompanyFlow.value ?: loadLocalCompany()

    companion object {
        @Volatile private var INSTANCE: CompanyRepository? = null

        fun getInstance(context: Context? = null): CompanyRepository = INSTANCE ?: synchronized(this) {
            CompanyRepository(context?.applicationContext).also { INSTANCE = it }
        }
    }

    suspend fun createCompany(creatorUser: UserProfile, companyName: String): Result<Company> {
        return try {
            val companyId = UUID.randomUUID().toString()
            val rawCode = (1000..9999).random()
            val prefix = companyName.take(4).uppercase().replace(" ", "X")
            val code = "$prefix-$rawCode"

            val company = Company(
                id = companyId,
                name = companyName,
                code = code,
                creatorId = creatorUser.uid
            )

            // Save locally immediately so user sees company without delay
            saveLocalCompany(company)

            // Try syncing to Firestore (gracefully catches if API disabled or offline)
            try {
                firestore.collection("companies").document(companyId).set(
                    mapOf(
                        "id" to company.id,
                        "name" to company.name,
                        "code" to company.code,
                        "creatorId" to company.creatorId,
                        "createdAt" to company.createdAt
                    )
                ).await()

                firestore.collection("companies").document(companyId)
                    .collection("members").document(creatorUser.uid).set(
                        mapOf(
                            "uid" to creatorUser.uid,
                            "displayName" to (creatorUser.displayName ?: "Yönetici"),
                            "email" to creatorUser.email,
                            "photoUrl" to creatorUser.photoUrl,
                            "role" to "owner",
                            "isCurrentlyActive" to false,
                            "todayTotalMinutes" to 0L
                        ),
                        SetOptions.merge()
                    ).await()

                firestore.collection("users").document(creatorUser.uid).set(
                    mapOf(
                        "companyId" to companyId,
                        "companyName" to companyName,
                        "role" to "owner"
                    ),
                    SetOptions.merge()
                ).await()
            } catch (_: Exception) { }

            Result.success(company)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinCompany(user: UserProfile, code: String): Result<Company> {
        val trimmedCode = code.trim().uppercase()
        return try {
            var company: Company? = null
            try {
                val querySnapshot = firestore.collection("companies")
                    .whereEqualTo("code", trimmedCode)
                    .limit(1)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents.first()
                    company = Company(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "Şirket",
                        code = doc.getString("code") ?: trimmedCode,
                        creatorId = doc.getString("creatorId") ?: "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )

                    firestore.collection("companies").document(company.id)
                        .collection("members").document(user.uid).set(
                            mapOf(
                                "uid" to user.uid,
                                "displayName" to (user.displayName ?: "Çalışan"),
                                "email" to user.email,
                                "photoUrl" to user.photoUrl,
                                "role" to "member",
                                "isCurrentlyActive" to false,
                                "todayTotalMinutes" to 0L
                            ),
                            SetOptions.merge()
                        ).await()

                    firestore.collection("users").document(user.uid).set(
                        mapOf(
                            "companyId" to company.id,
                            "companyName" to company.name,
                            "role" to "member"
                        ),
                        SetOptions.merge()
                    ).await()
                }
            } catch (_: Exception) { }

            if (company == null) {
                // If offline or Firestore not ready, create local joined company reference
                company = Company(
                    id = UUID.randomUUID().toString(),
                    name = "Ekip ($trimmedCode)",
                    code = trimmedCode,
                    creatorId = ""
                )
            }

            saveLocalCompany(company)
            Result.success(company)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveCompany(userId: String, companyId: String): Result<Unit> {
        saveLocalCompany(null)
        try {
            firestore.collection("companies").document(companyId)
                .collection("members").document(userId).delete().await()

            firestore.collection("users").document(userId).set(
                mapOf(
                    "companyId" to null,
                    "companyName" to null,
                    "role" to "member"
                ),
                SetOptions.merge()
            ).await()
        } catch (_: Exception) { }
        return Result.success(Unit)
    }

    suspend fun getUserCompany(userId: String): Company? {
        val local = getCurrentCompany()
        if (local != null) return local

        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val companyId = userDoc.getString("companyId") ?: return null
            val compDoc = firestore.collection("companies").document(companyId).get().await()
            if (compDoc.exists()) {
                val comp = Company(
                    id = compDoc.getString("id") ?: compDoc.id,
                    name = compDoc.getString("name") ?: "Şirket",
                    code = compDoc.getString("code") ?: "",
                    creatorId = compDoc.getString("creatorId") ?: "",
                    createdAt = compDoc.getLong("createdAt") ?: 0L
                )
                saveLocalCompany(comp)
                comp
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun observeCompanyMembersAttendance(companyId: String): Flow<List<MemberAttendanceStatus>> = callbackFlow {
        val listener = firestore.collection("companies").document(companyId)
            .collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val local = getFallbackLocalMember()
                    trySend(listOf(local))
                    return@addSnapshotListener
                }

                val members = snapshot?.documents ?: emptyList()
                val resultList = members.map { doc ->
                    val uid = doc.getString("uid") ?: doc.id
                    val name = doc.getString("displayName") ?: "İsimsiz Çalışan"
                    val email = doc.getString("email") ?: ""
                    val photoUrl = doc.getString("photoUrl")
                    val role = doc.getString("role") ?: "member"
                    val isActive = doc.getBoolean("isCurrentlyActive") ?: false
                    val checkIn = doc.getLong("lastCheckInTime")
                    val todayMinutes = doc.getLong("todayTotalMinutes") ?: 0L
                    val weeklyMinutes = doc.getLong("weeklyTotalMinutes") ?: todayMinutes
                    val monthlyMinutes = doc.getLong("monthlyTotalMinutes") ?: todayMinutes

                    MemberAttendanceStatus(
                        userId = uid,
                        displayName = name,
                        email = email,
                        photoUrl = photoUrl,
                        isCurrentlyActive = isActive,
                        checkInTime = checkIn,
                        todayTotalMinutes = todayMinutes,
                        weeklyTotalMinutes = weeklyMinutes,
                        monthlyTotalMinutes = monthlyMinutes,
                        role = role
                    )
                }

                if (resultList.isEmpty()) {
                    val local = getFallbackLocalMember()
                    trySend(listOf(local))
                } else {
                    trySend(resultList)
                }
            }

        awaitClose { listener.remove() }
    }

    private fun getFallbackLocalMember(): MemberAttendanceStatus {
        val company = getCurrentCompany()
        return MemberAttendanceStatus(
            userId = "local_user",
            displayName = "Ben (Siz)",
            email = "",
            photoUrl = null,
            isCurrentlyActive = false,
            checkInTime = null,
            todayTotalMinutes = 0L,
            weeklyTotalMinutes = 0L,
            monthlyTotalMinutes = 0L,
            role = if (company?.creatorId?.isNotEmpty() == true) "Yönetici" else "Çalışan"
        )
    }

    suspend fun updateMemberLiveStatus(
        companyId: String,
        userId: String,
        isActive: Boolean,
        checkInTime: Long?,
        todayTotalMinutes: Long,
        weeklyTotalMinutes: Long = 0L,
        monthlyTotalMinutes: Long = 0L
    ) {
        try {
            firestore.collection("companies").document(companyId)
                .collection("members").document(userId).set(
                    mapOf(
                        "isCurrentlyActive" to isActive,
                        "lastCheckInTime" to checkInTime,
                        "todayTotalMinutes" to todayTotalMinutes,
                        "weeklyTotalMinutes" to weeklyTotalMinutes,
                        "monthlyTotalMinutes" to monthlyTotalMinutes
                    ),
                    SetOptions.merge()
                ).await()
        } catch (_: Exception) { }
    }

    suspend fun fetchMemberAttendanceHistory(memberUserId: String): List<com.workhourtracker.domain.model.AttendanceRecord> {
        return try {
            val snapshot = firestore.collection("users").document(memberUserId)
                .collection("attendance")
                .orderBy("checkInTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(40)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val checkIn = doc.getLong("checkInTime") ?: return@mapNotNull null
                val checkOut = doc.getLong("checkOutTime")
                val typeStr = doc.getString("type") ?: "MANUAL"
                val breakMinutes = doc.getLong("breakMinutes") ?: 0L
                com.workhourtracker.domain.model.AttendanceRecord(
                    id = 0,
                    userId = memberUserId,
                    checkInTime = checkIn,
                    checkOutTime = checkOut,
                    type = runCatching { com.workhourtracker.domain.model.RecordType.valueOf(typeStr) }.getOrDefault(com.workhourtracker.domain.model.RecordType.MANUAL),
                    isSynced = true,
                    firebaseId = doc.id,
                    breakMinutes = breakMinutes
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
