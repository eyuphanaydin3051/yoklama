package com.workhourtracker.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.workhourtracker.presentation.ui.screens.auth.AuthScreen
import com.workhourtracker.presentation.ui.screens.main.MainScreen
import com.workhourtracker.presentation.ui.screens.settings.SettingsScreen
import com.workhourtracker.presentation.ui.screens.stats.StatsScreen
import com.workhourtracker.presentation.viewmodel.AttendanceViewModel
import com.workhourtracker.presentation.viewmodel.AuthViewModel
import com.workhourtracker.presentation.viewmodel.SettingsViewModel
import com.workhourtracker.presentation.viewmodel.StatsViewModel

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val user by authViewModel.user.collectAsStateWithLifecycle()

    if (user == null) {
        // Auth screen without bottom bar
        AuthScreen(
            onAuthSuccess = { },
            viewModel = authViewModel
        )
    } else {
        // Main authenticated app with Bottom Navigation Bar
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: "main"

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    val items = listOf(
                        Triple("main", "Yoklama", Icons.Filled.Schedule),
                        Triple("stats", "İstatistikler", Icons.Filled.BarChart),
                        Triple("settings", "Ayarlar", Icons.Filled.Settings)
                    )

                    items.forEach { (route, label, icon) ->
                        val selected = currentRoute == route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(icon, contentDescription = label)
                            },
                            label = {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "main",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                composable("main") {
                    val attendanceViewModel: AttendanceViewModel = hiltViewModel()
                    MainScreen(viewModel = attendanceViewModel)
                }

                composable("stats") {
                    val statsViewModel: StatsViewModel = hiltViewModel()
                    StatsScreen(viewModel = statsViewModel)
                }

                composable("settings") {
                    val settingsViewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onSignOut = {
                            authViewModel.signOut()
                        }
                    )
                }
            }
        }
    }
}
