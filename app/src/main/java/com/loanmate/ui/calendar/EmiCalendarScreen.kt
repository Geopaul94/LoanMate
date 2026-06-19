package com.loanmate.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanmate.utils.CurrencyUtils
import com.loanmate.utils.EmiOccurrenceGenerator
import com.loanmate.viewmodel.DayKey
import com.loanmate.viewmodel.EmiCalendarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiCalendarScreen(
    onBack: () -> Unit,
    viewModel: EmiCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val now = Calendar.getInstance()
    var year by rememberSaveable { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var month by rememberSaveable { mutableIntStateOf(now.get(Calendar.MONTH)) }
    var selectedDay by rememberSaveable { mutableStateOf<DayKey?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EMI Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            MonthHeader(
                year = year,
                month = month,
                onPrev = {
                    if (month == 0) { month = 11; year -= 1 } else month -= 1
                },
                onNext = {
                    if (month == 11) { month = 0; year += 1 } else month += 1
                }
            )

            Spacer(Modifier.height(12.dp))
            WeekdayHeader()
            Spacer(Modifier.height(4.dp))
            CalendarGrid(
                year = year,
                month = month,
                today = DayKey(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)),
                occurrencesByDay = uiState.occurrencesByDay,
                onDayClick = { day -> selectedDay = day }
            )

            Spacer(Modifier.height(16.dp))

            // Day-of-month occurrences appear in a scrollable list below the calendar
            val selected = selectedDay
            if (selected != null) {
                val list = uiState.occurrencesByDay[selected].orEmpty()
                if (list.isNotEmpty()) {
                    DaySection(day = selected, occurrences = list, onClose = { selectedDay = null })
                }
            }

            if (uiState.occurrencesByDay.isEmpty() && !uiState.isLoading) {
                Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No upcoming EMIs. Add an active loan to see EMI dates here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(year: Int, month: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    val cal = Calendar.getInstance().apply { set(year, month, 1) }
    val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, "Previous month") }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "Next month") }
    }
}

@Composable
private fun WeekdayHeader() {
    val days = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { d ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(d, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    year: Int,
    month: Int,
    today: DayKey,
    occurrencesByDay: Map<DayKey, List<EmiOccurrenceGenerator.Occurrence>>,
    onDayClick: (DayKey) -> Unit
) {
    val cal = Calendar.getInstance().apply { set(year, month, 1) }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1  // Sun=0
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val totalSlots = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7  // round up to week

    Column {
        var slotIndex = 0
        while (slotIndex < totalSlots) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) {
                    val dayNum = slotIndex - firstDayOfWeek + 1
                    val inMonth = dayNum in 1..daysInMonth
                    val key = if (inMonth) DayKey(year, month, dayNum) else null
                    val isToday = key == today
                    val occ = key?.let { occurrencesByDay[it] }.orEmpty()
                    DayCell(
                        modifier = Modifier.weight(1f),
                        day = if (inMonth) dayNum else null,
                        isToday = isToday,
                        hasEvents = occ.isNotEmpty(),
                        eventCount = occ.size,
                        onClick = { key?.let { onDayClick(it) } }
                    )
                    slotIndex++
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    modifier: Modifier,
    day: Int?,
    isToday: Boolean,
    hasEvents: Boolean,
    eventCount: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .height(56.dp)
            .clickable(enabled = day != null && hasEvents, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (day == null) return@Box
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .then(
                        if (isToday) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (hasEvents) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(eventCount.coerceAtMost(3)) {
                        Box(modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(5.dp)
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape))
                    }
                    if (eventCount > 3) {
                        Text("+", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySection(
    day: DayKey,
    occurrences: List<EmiOccurrenceGenerator.Occurrence>,
    onClose: () -> Unit
) {
    val cal = Calendar.getInstance().apply { set(day.year, day.month, day.day) }
    val dateLabel = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(cal.time)
    val total = occurrences.sumOf { it.amount }

    Card(shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(dateLabel, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) { Text("Close") }
            }
            Text("Total due: ${CurrencyUtils.format(total)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(occurrences) { occ ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(occ.loanName, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium)
                            Text(occ.bankName, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(CurrencyUtils.format(occ.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
