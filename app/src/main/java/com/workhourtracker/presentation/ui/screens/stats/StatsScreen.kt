package com.workhourtracker.presentation.ui.screens.stats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workhourtracker.domain.model.AttendanceRecord
import com.workhourtracker.domain.model.DailyStats
import com.workhourtracker.domain.model.MemberAttendanceStatus
import com.workhourtracker.presentation.ui.components.DailyStatCard
import com.workhourtracker.presentation.ui.components.EditRecordDialog
import com.workhourtracker.presentation.viewmodel.StatsViewModel
import com.workhourtracker.presentation.viewmodel.TeamTimeFilter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val monthRecords by viewModel.monthRecords.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    val currentCompany by viewModel.currentCompany.collectAsStateWithLifecycle()
    val companyName = currentCompany?.name ?: viewModel.getUserCompany()

    val teamTimeFilter by viewModel.teamTimeFilter.collectAsStateWithLifecycle()
    val selectedMember by viewModel.selectedMember.collectAsStateWithLifecycle()
    val memberRecords by viewModel.memberRecords.collectAsStateWithLifecycle()
    val isMemberLoading by viewModel.isMemberLoading.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var recordToEdit by remember { mutableStateOf<com.workhourtracker.domain.model.AttendanceRecord?>(null) }
    val monthName = selectedMonth.month.getDisplayName(TextStyle.FULL, Locale("tr"))

    if (selectedMember != null) {
        MemberDetailScreen(
            member = selectedMember!!,
            records = memberRecords,
            isLoading = isMemberLoading,
            viewModel = viewModel,
            selectedMonth = selectedMonth,
            onBack = { viewModel.selectMember(null) }
        )
    } else {
        Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedTabIndex == 0) {
                                IconButton(onClick = { viewModel.previousMonth() }) {
                                    Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = "Önceki ay")
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$monthName ${selectedMonth.year}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Aylık Mesai Raporu",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { viewModel.nextMonth() }) {
                                    Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "Sonraki ay")
                                }
                            } else {
                                Text(
                                    text = companyName?.let { "🏢 $it Ekibi" } ?: "Şirket & Ekip Saatleri",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Tabs: Kişisel vs Şirket/Ekip
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Kişisel Rapor", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ekip Saatleri", fontWeight = FontWeight.SemiBold)
                                if (teamMembers.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("${teamMembers.size}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (selectedTabIndex == 0) {
            // Tab 0: Kişisel İstatistikler
            PersonalStatsContent(
                isLoading = isLoading,
                monthlyStats = monthlyStats,
                monthRecords = monthRecords,
                paddingValues = paddingValues,
                viewModel = viewModel,
                onEditRecord = { recordToEdit = it },
                onDeleteRecord = { viewModel.deleteRecord(it) }
            )
        } else {
            // Tab 1: Şirket / Ekip Durumu
            TeamAttendanceContent(
                teamMembers = teamMembers,
                companyName = companyName,
                paddingValues = paddingValues,
                viewModel = viewModel,
                teamTimeFilter = teamTimeFilter
            )
        }
    }
    }

    recordToEdit?.let { record ->
        com.workhourtracker.presentation.ui.components.EditRecordDialog(
            record = record,
            onDismiss = { recordToEdit = null },
            onSave = { updated ->
                viewModel.updateRecord(updated)
                recordToEdit = null
            },
            onDelete = { toDelete ->
                viewModel.deleteRecord(toDelete)
                recordToEdit = null
            }
        )
    }
}

@Composable
private fun PersonalStatsContent(
    isLoading: Boolean,
    monthlyStats: com.workhourtracker.domain.model.MonthlyStats?,
    monthRecords: List<com.workhourtracker.domain.model.AttendanceRecord>,
    paddingValues: PaddingValues,
    viewModel: StatsViewModel,
    onEditRecord: (com.workhourtracker.domain.model.AttendanceRecord) -> Unit,
    onDeleteRecord: (com.workhourtracker.domain.model.AttendanceRecord) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        val stats = monthlyStats
        if (stats == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Bu ay için istatistik bulunamadı.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Metric Summary Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatSummaryCard(
                            title = "Toplam Süre",
                            value = viewModel.formatDuration(stats.totalMinutes),
                            icon = Icons.Filled.Timer,
                            color = Color(0xFF2563EB)
                        )
                        StatSummaryCard(
                            title = "Çalışılan Gün",
                            value = "${stats.totalDays} gün",
                            icon = Icons.Filled.CalendarMonth,
                            color = Color(0xFF10B981)
                        )
                        StatSummaryCard(
                            title = "Ortalama",
                            value = viewModel.formatDuration(stats.averageDailyMinutes.toLong()),
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            color = Color(0xFF8B5CF6)
                        )
                    }
                }

                // 2. Visual Daily Bar Chart Card
                item {
                    WorkHourBarChartCard(dailyMap = stats.dailyBreakdown)
                }

                // 3. Section Title
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Günlük Döküm",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${stats.dailyBreakdown.size} gün",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4. Daily List (Expandable with full edit/delete support)
                items(stats.dailyBreakdown.toList().sortedByDescending { it.first }) { (date, dailyStat) ->
                    val dayRecords = monthRecords.filter { it.checkInDate == date }
                    DailyStatCard(
                        date = date,
                        stats = dailyStat,
                        dayRecords = dayRecords,
                        onEditRecord = onEditRecord,
                        onDeleteRecord = onDeleteRecord
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamAttendanceContent(
    teamMembers: List<MemberAttendanceStatus>,
    companyName: String?,
    paddingValues: PaddingValues,
    viewModel: StatsViewModel,
    teamTimeFilter: TeamTimeFilter
) {
    if (companyName == null) {
        // Not in a company
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Business,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Bir Şirkete Dahil Değilsiniz",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Çalışma arkadaşlarınızın saatlerini görebilmek için Ayarlar sekmesinden bir şirket oluşturun veya şirket kodunu girin.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    } else {
        // Company members list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(companyName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Toplam ${teamMembers.size} çalışan kayıtlı", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // Period Filter Chips (Bugün / Bu Hafta / Bu Ay)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TeamTimeFilter.values().forEach { filter ->
                        FilterChip(
                            selected = teamTimeFilter == filter,
                            onClick = { viewModel.setTeamTimeFilter(filter) },
                            label = {
                                Text(
                                    text = filter.label,
                                    fontWeight = if (teamTimeFilter == filter) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            items(teamMembers) { member ->
                MemberAttendanceCard(
                    member = member,
                    filter = teamTimeFilter,
                    onClick = { viewModel.selectMember(member) }
                )
            }
        }
    }
}

@Composable
private fun MemberAttendanceCard(
    member: MemberAttendanceStatus,
    filter: TeamTimeFilter,
    onClick: () -> Unit
) {
    val checkInStr = member.checkInTime?.let {
        DateTimeFormatter.ofPattern("HH:mm").format(
            java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        )
    }

    val displayMinutes = when (filter) {
        TeamTimeFilter.TODAY -> member.todayTotalMinutes
        TeamTimeFilter.WEEKLY -> member.weeklyTotalMinutes
        TeamTimeFilter.MONTHLY -> member.monthlyTotalMinutes
    }

    val hours = displayMinutes / 60
    val mins = displayMinutes % 60
    val durationText = if (hours > 0) "${hours}s ${mins}dk" else "${mins}dk"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.displayName.firstOrNull()?.uppercase() ?: "Ç",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (member.role == "owner") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("Yönetici", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.FiberManualRecord,
                            contentDescription = null,
                            modifier = Modifier.size(8.dp),
                            tint = if (member.isCurrentlyActive) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (member.isCurrentlyActive) "Ofiste (Giriş: $checkInStr)" else "Mesai Dışı",
                            fontSize = 12.sp,
                            color = if (member.isCurrentlyActive) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Period Duration badge & Chevron icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = durationText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = filter.label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Detay",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberDetailScreen(
    member: MemberAttendanceStatus,
    records: List<AttendanceRecord>,
    isLoading: Boolean,
    viewModel: StatsViewModel,
    selectedMonth: LocalDate,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, EEEE", Locale("tr"))
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val checkInStr = member.checkInTime?.let {
        DateTimeFormatter.ofPattern("HH:mm").format(
            java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        )
    }

    // Build member's daily breakdown map for the selected month so WorkHourBarChartCard can render it!
    val memberDailyMap = remember(records, selectedMonth) {
        val daysInMonth = selectedMonth.lengthOfMonth()
        (1..daysInMonth).associate { day ->
            val date = selectedMonth.withDayOfMonth(day)
            val dayRecs = records.filter { it.checkInDate == date && it.checkOutTime != null }
            val totalMins = dayRecs.sumOf { it.durationMinutes ?: 0L }
            val firstIn = dayRecs.minByOrNull { it.checkInTime }?.checkInDateTime
            val lastOut = dayRecs.maxByOrNull { it.checkOutTime ?: 0L }?.checkOutDateTime
            date to DailyStats(date, totalMins, dayRecs.size, firstIn, lastOut)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = member.displayName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (member.role == "owner") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Yönetici", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = member.email.ifBlank { "Ekip Üyesi Mesai Profili" },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live Office Status Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (member.isCurrentlyActive) Color(0xFF10B981).copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (member.isCurrentlyActive) Color(0xFF10B981).copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.displayName.firstOrNull()?.uppercase() ?: "Ç",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (member.isCurrentlyActive) Color(0xFF047857) else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.FiberManualRecord,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = if (member.isCurrentlyActive) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (member.isCurrentlyActive) "Ofiste Çalışıyor" else "Mesai Dışı",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (member.isCurrentlyActive) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (member.isCurrentlyActive) "Giriş saati: $checkInStr" else "Aktif mesai bulunmuyor",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. 3 Period Summary Cards (Bugün, Bu Hafta, Bu Ay)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PeriodStatCard(
                        title = "Bugün",
                        duration = viewModel.formatDuration(member.todayTotalMinutes),
                        modifier = Modifier.weight(1f)
                    )
                    PeriodStatCard(
                        title = "Bu Hafta",
                        duration = viewModel.formatDuration(member.weeklyTotalMinutes),
                        modifier = Modifier.weight(1f)
                    )
                    PeriodStatCard(
                        title = "Bu Ay",
                        duration = viewModel.formatDuration(member.monthlyTotalMinutes),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Member Daily Distribution Bar Chart
            item {
                WorkHourBarChartCard(
                    dailyMap = memberDailyMap,
                    title = "${member.displayName} - Günlük Dağılım"
                )
            }

            // 4. Session History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mesai Kayıtları Geçmişi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (records.isNotEmpty()) {
                        Text(
                            text = "${records.size} oturum",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 5. Member Records List
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    }
                }
            } else if (records.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Kayıtlı mesai geçmişi bulunmuyor.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val sortedRecords = records.sortedByDescending { it.checkInTime }
                items(sortedRecords) { rec ->
                    MemberRecordItem(rec = rec, dateFormatter = dateFormatter, timeFormatter = timeFormatter)
                }
            }
        }
    }
}

@Composable
private fun PeriodStatCard(
    title: String,
    duration: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = duration,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MemberRecordItem(
    rec: AttendanceRecord,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter
) {
    val checkInStr = rec.checkInDateTime.format(timeFormatter)
    val checkOutStr = rec.checkOutDateTime?.format(timeFormatter) ?: "Devam Ediyor"
    val dateStr = rec.checkInDateTime.format(dateFormatter)

    val durationText = if (rec.isActive) {
        "Devam Ediyor"
    } else {
        val totalMins = rec.durationMinutes ?: 0L
        val hours = totalMins / 60
        val mins = totalMins % 60
        if (hours > 0) "${hours}s ${mins}dk" else "${mins}dk"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$checkInStr - $checkOutStr",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (rec.breakMinutes > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🍽️ ${rec.breakMinutes} dk mola",
                            fontSize = 10.sp,
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        if (rec.isActive) Color(0xFF10B981).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = durationText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (rec.isActive) Color(0xFF047857) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RowScope.StatSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .height(105.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkHourBarChartCard(
    dailyMap: Map<LocalDate, DailyStats>,
    title: String = "Günlük Dağılım Grafiği"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("≥8 Saat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable chart canvas with auto-center on current day
            val sortedList = dailyMap.toList().sortedBy { it.first }
            val barWidth = 14.dp
            val barPaddingHorizontal = 4.dp
            val totalItemWidth = barWidth + (barPaddingHorizontal * 2) // 22.dp
            val maxHours = 12f // 12 hours max scale

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val viewportWidthPx = constraints.maxWidth
                val scrollState = rememberScrollState()
                val density = LocalDensity.current
                val itemWidthPx = with(density) { totalItemWidth.toPx() }
                val today = LocalDate.now()
                val todayIndex = sortedList.indexOfFirst { it.first == today }

                LaunchedEffect(sortedList, todayIndex, viewportWidthPx) {
                    if (todayIndex >= 0 && viewportWidthPx > 0) {
                        val centerTargetPx = (todayIndex * itemWidthPx + (itemWidthPx / 2f) - (viewportWidthPx / 2f)).toInt().coerceAtLeast(0)
                        scrollState.scrollTo(centerTargetPx)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(vertical = 8.dp)
                ) {
                    sortedList.forEach { (date, stat) ->
                        val hours = stat.totalMinutes / 60f
                        val isToday = date == today
                        val isTargetMet = hours >= 8f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = barPaddingHorizontal)
                        ) {
                            // Bar Column
                            Box(
                                modifier = Modifier
                                    .width(barWidth)
                                    .height(120.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                // Background track
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                )

                                // Active Fill Bar
                                val heightFraction = (hours / maxHours).coerceIn(0f, 1f)
                                if (heightFraction > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((120 * heightFraction).dp)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                if (isTargetMet) Color(0xFF10B981)
                                                else if (isToday) MaterialTheme.colorScheme.primary
                                                else Color(0xFF3B82F6)
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Day label
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = 11.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
