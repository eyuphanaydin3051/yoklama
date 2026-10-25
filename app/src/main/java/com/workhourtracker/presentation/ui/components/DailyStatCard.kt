package com.workhourtracker.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workhourtracker.domain.model.AttendanceRecord
import com.workhourtracker.domain.model.DailyStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DailyStatCard(
    date: LocalDate,
    stats: DailyStats,
    dayRecords: List<AttendanceRecord> = emptyList(),
    onEditRecord: (AttendanceRecord) -> Unit = {},
    onDeleteRecord: (AttendanceRecord) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val isToday = date == LocalDate.now()
    val isWeekend = date.dayOfWeek.value in 6..7
    val dayOfWeekName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("tr")).uppercase()
    val dayOfMonth = date.dayOfMonth
    val totalMinutes = stats.totalMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    val hasWorked = totalMinutes > 0 || stats.recordCount > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Date Badge + Details
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Calendar Day Box
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primary else if (hasWorked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dayOfWeekName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dayOfMonth.toString(),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr"))),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isToday) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("BUGÜN", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        if (stats.recordCount > 0) {
                            val firstIn = stats.firstCheckIn?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--"
                            val lastOut = stats.lastCheckOut?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--"
                            Text(
                                text = "Giriş: $firstIn • Çıkış: $lastOut (${stats.recordCount} oturum)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = if (isWeekend) "Hafta Sonu" else "Kayıt yok",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Right: Duration Pill & Expand Arrow
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasWorked) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (hours >= 8) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (hours > 0) "${hours}s ${minutes}dk" else "${minutes}dk",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hours >= 8) Color(0xFF047857) else MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = "--",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
                        contentDescription = if (expanded) "Kapat" else "Aç",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded detail: Individual shifts with edit & delete
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
                    Text(
                        text = "Günün Oturumları (Düzenlemek veya silmek için dokunun):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (dayRecords.isEmpty()) {
                        Text(
                            text = "Bu güne ait ayrıntılı kayıt bulunamadı.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        dayRecords.forEach { record ->
                            RecordListItem(
                                record = record,
                                onDelete = { onDeleteRecord(record) },
                                onEdit = { onEditRecord(record) }
                            )
                        }
                    }
                }
            }
        }
    }
}
