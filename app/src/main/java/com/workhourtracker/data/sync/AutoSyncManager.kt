package com.workhourtracker.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.workhourtracker.data.remote.AuthRepository
import com.workhourtracker.data.repository.AttendanceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AttendanceRepository,
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun startAutoSync() {
        if (connectivityManager == null) return

        // Trigger immediate sync if already online
        if (isNetworkAvailable()) {
            triggerSync()
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                triggerSync()
            }
        })
    }

    fun triggerSync() {
        val user = authRepository.getCurrentUser()
        val userId = user?.uid ?: "local_user"
        scope.launch {
            try {
                repository.syncAllUnsynced(userId)
                if (user != null) {
                    repository.pullRemoteRecords(userId)
                }
            } catch (_: Exception) { }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager?.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
