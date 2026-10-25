package com.workhourtracker.presentation.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workhourtracker.presentation.ui.components.RecordListItem
import com.workhourtracker.presentation.ui.components.RetroactiveEntryDialog
import com.workhourtracker.presentation.viewmodel.AttendanceViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val activeRecord by viewModel.activeRecord.collectAsStateWithLifecycle()
    val dailyStats by viewModel.dailyStats.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val currentDate by viewModel.currentDate.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    val showRetroactiveDialog = remember { mutableStateOf(false) }
    val showCheckOutDialog = remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<com.workhourtracker.domain.model.AttendanceRecord?>(null) }

    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            delay(3000)
            viewModel.clearUserMessage()
        }
    }

    val todayTotal = viewModel.getTodayTotalMinutes()
    val isCheckedIn = viewModel.isCheckedIn
    val isToday = currentDate == LocalDate.now()

    // Live Ticker for active work time
    var currentElapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(activeRecord) {
        if (activeRecord != null) {
            while (true) {
                val now = System.currentTimeMillis()
                val diffSecs = (now - activeRecord!!.checkInTime) / 1000
                currentElapsedSeconds = if (diffSecs > 0) diffSecs else 0
                delay(1000L)
            }
        } else {
            currentElapsedSeconds = 0L
        }
    }

    // Pulsing animation for active shift
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Yoklama & Mesai",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isCheckedIn) "Şu an mesai devam ediyor" else "Mesai durumu pasif",
                            fontSize = 12.sp,
                            color = if (isCheckedIn) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    // Quick retroactive entry icon
                    IconButton(onClick = { showRetroactiveDialog.value = true }) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "Geçmiş Kayıt",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Date Selector Bar
            item {
                DateNavigationBar(
                    currentDate = currentDate,
                    isToday = isToday,
                    onPreviousDay = { viewModel.setCurrentDate(currentDate.minusDays(1)) },
                    onNextDay = { viewModel.setCurrentDate(currentDate.plusDays(1)) },
                    onTodayClick = { viewModel.setCurrentDate(LocalDate.now()) }
                )
            }

            // 2. HERO Active / Idle Check-in Card
            item {
                HeroCheckInWidget(
                    isCheckedIn = isCheckedIn,
                    isLoading = isLoading,
                    activeRecord = activeRecord,
                    elapsedSeconds = currentElapsedSeconds,
                    pulseAlpha = pulseAlpha,
                    onToggleClick = {
                        if (isCheckedIn) {
                            showCheckOutDialog.value = true
                        } else {
                            viewModel.onCheckInClick()
                        }
                    }
                )
            }

            // 3. Quick Metrics Strip (Total time, target progress, entry count)
            item {
                DailyMetricsStrip(
                    totalMinutes = todayTotal,
                    recordCount = dailyStats?.recordCount ?: records.size
                )
            }

            // 4. Section Title & Add Retroactive Action Banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Günün Kayıtları",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable { showRetroactiveDialog.value = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Geçmiş Ekle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 5. Records List
            if (records.isEmpty()) {
                item {
                    EmptyRecordsCard(isToday = isToday) {
                        showRetroactiveDialog.value = true
                    }
                }
            } else {
                items(records, key = { it.id }) { record ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        RecordListItem(
                            record = record,
                            onDelete = { viewModel.onDeleteRecord(record) },
                            onEdit = { recordToEdit = record }
                        )
                    }
                }
            }

            // User notification banner
            userMessage?.let { msg ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }
    }

    if (showRetroactiveDialog.value) {
        RetroactiveEntryDialog(
            onDismiss = { showRetroactiveDialog.value = false },
            onConfirm = { checkIn, checkOut, targetDate, breakMinutes ->
                viewModel.onRetroactiveEntry(checkIn, checkOut, targetDate, breakMinutes)
                showRetroactiveDialog.value = false
            }
        )
    }

    if (showCheckOutDialog.value) {
        CheckOutLunchBreakDialog(
            elapsedSeconds = currentElapsedSeconds,
            onDismiss = { showCheckOutDialog.value = false },
            onConfirm = { breakMinutes ->
                viewModel.onCheckOutClick(breakMinutes)
                showCheckOutDialog.value = false
            }
        )
    }

    recordToEdit?.let { record ->
        com.workhourtracker.presentation.ui.components.EditRecordDialog(
            record = record,
            onDismiss = { recordToEdit = null },
            onSave = { updated ->
                viewModel.onUpdateRecord(updated)
                recordToEdit = null
            },
            onDelete = { toDelete ->
                viewModel.onDeleteRecord(toDelete)
                recordToEdit = null
            }
        )
    }
}

@Composable
private fun DateNavigationBar(
    currentDate: LocalDate,
    isToday: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousDay, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Önceki gün")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onTodayClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy, EEEE", Locale("tr"))),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isToday) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("BUGÜN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            IconButton(onClick = onNextDay, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Sonraki gün")
            }
        }
    }
}

@Composable
private fun HeroCheckInWidget(
    isCheckedIn: Boolean,
    isLoading: Boolean,
    activeRecord: com.workhourtracker.domain.model.AttendanceRecord?,
    elapsedSeconds: Long,
    pulseAlpha: Float,
    onToggleClick: () -> Unit
) {
    val bgGradient = if (isCheckedIn) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF065F46), // Deep Emerald
                Color(0xFF047857)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E3A8A), // Deep Blue
                Color(0xFF2563EB)
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgGradient)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Badge Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        Icons.Filled.FiberManualRecord,
                        contentDescription = null,
                        modifier = Modifier
                            .size(10.dp)
                            .alpha(if (isCheckedIn) pulseAlpha else 0.8f),
                        tint = if (isCheckedIn) Color(0xFF34D399) else Color(0xFF93C5FD)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCheckedIn) "MESAİ DEVAM EDİYOR" else "MESAİ DIŞI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timer or Call to Action
                if (isCheckedIn) {
                    val hours = elapsedSeconds / 3600
                    val minutes = (elapsedSeconds % 3600) / 60
                    val seconds = elapsedSeconds % 60
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    activeRecord?.let {
                        Text(
                            text = "Başlangıç: ${it.checkInDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    Text(
                        text = "İyi Çalışmalar!",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Çalışmaya başladığınızda butona dokunun",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Interactive Primary Action Button
                Button(
                    onClick = onToggleClick,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCheckedIn) Color(0xFFEF4444) else Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = if (isCheckedIn) Color.White else Color(0xFF2563EB)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isCheckedIn) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                tint = if (isCheckedIn) Color.White else Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCheckedIn) "Mesaiyi Bitir" else "Mesaiyi Başlat",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCheckedIn) Color.White else Color(0xFF2563EB)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyMetricsStrip(
    totalMinutes: Long,
    recordCount: Int
) {
    val targetMinutes = 8 * 60 // 8 hours daily target
    val progress = (totalMinutes.toFloat() / targetMinutes.toFloat()).coerceIn(0f, 1f)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bugünkü Çalışma",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (hours > 0) "${hours}s ${minutes}dk" else "${minutes}dk",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar toward 8-hour target
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (progress >= 1f) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "8 saatlik hedef: %${(progress * 100).toInt()}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$recordCount oturum",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyRecordsCard(isToday: Boolean, onAddClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isToday) "Bugün için henüz kayıt bulunmuyor" else "Bu tarihte kayıtlı mesai yok",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onAddClick,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Manuel Kayıt Ekle", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun CheckOutLunchBreakDialog(
    elapsedSeconds: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var hasLunchBreak by remember { mutableStateOf(elapsedSeconds >= 4 * 3600) }
    var breakMinutes by remember { mutableLongStateOf(60L) }

    val elapsedMinutes = (elapsedSeconds / 60).coerceAtLeast(0)
    val effectiveBreak = if (hasLunchBreak) breakMinutes else 0L
    val netMinutes = (elapsedMinutes - effectiveBreak).coerceAtLeast(0)

    val elapsedHours = elapsedMinutes / 60
    val elapsedRemMins = elapsedMinutes % 60
    val netHours = netMinutes / 60
    val netRemMins = netMinutes % 60

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mesaiyi Sonlandır",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Toplam süre: ${elapsedHours}s ${elapsedRemMins}dk",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Kapat")
                    }
                }

                // Lunch Break Toggle Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Restaurant,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Öğle Molası Düşülsün mü?",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Mesai süresinden çıkarılacak mola",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = hasLunchBreak,
                            onCheckedChange = { hasLunchBreak = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD97706))
                        )
                    }

                    if (hasLunchBreak) {
                        // Quick chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(30L, 45L, 60L, 90L).forEach { mins ->
                                val isSelected = breakMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) Color(0xFFD97706)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable { breakMinutes = mins }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (mins == 60L) "1 Saat" else "$mins dk",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Stepper (- / + 15 dk)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mola Süresi:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { breakMinutes = (breakMinutes - 15).coerceAtLeast(15L) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Azalt", modifier = Modifier.size(16.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$breakMinutes dakika",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                }

                                IconButton(
                                    onClick = { breakMinutes = (breakMinutes + 15).coerceAtMost(240L) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Artır", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Calculation preview
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFD97706).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mola: -${breakMinutes} dk",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Net Çalışma: ${netHours}s ${netRemMins}dk",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Vazgeç")
                    }

                    Button(
                        onClick = {
                            val finalBreak = if (hasLunchBreak) breakMinutes else 0L
                            onConfirm(finalBreak)
                        },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Çıkış Yap", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
