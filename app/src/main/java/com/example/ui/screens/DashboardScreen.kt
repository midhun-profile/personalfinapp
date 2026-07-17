package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import com.example.ui.FinanceViewModel
import com.example.ui.theme.getCategoryColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: FinanceViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val limit by viewModel.budgetLimit.collectAsState()
    val symbol by viewModel.currencySymbol.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    val scrollState = rememberScrollState()

    // Calculations
    val totalExpensesSum = expenses.sumOf { it.amount }
    val remainingBudget = (limit - totalExpensesSum).coerceAtLeast(0.0)
    val spendPercentage = if (limit > 0) (totalExpensesSum / limit).toFloat() else 0f

    val totalSaved = goals.sumOf { it.currentAmount }
    val activeGoalsCount = goals.count { it.status == "active" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("dashboard_screen_root")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Finance Dashboard",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Month & Year Dynamic pill
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    val currentMonthYear = SimpleDateFormat("MMMM yyyy", Locale.US).format(Calendar.getInstance().time)
                    Text(
                        text = currentMonthYear.uppercase(Locale.US),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1),
                        letterSpacing = 1.sp
                    )
                }
            }

            // Sync pill/indicator trigger
            IconButton(
                onClick = { if (syncState != "OFFLINE") viewModel.syncNow() },
                modifier = Modifier
                    .background(Color(0xFF1E293B), CircleShape)
                    .border(1.dp, Color(0xFF334155), CircleShape)
                    .testTag("dashboard_sync_icon_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync Now",
                    tint = if (syncState == "CONNECTED") Color(0xFF10B981) else Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Remaining Budget Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_budget_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // Premium dark slate card
            border = BorderStroke(1.dp, Color(0xFF334155)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.0f)) {
                    Text(
                        text = "REMAINING BUDGET",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$symbol${String.format(Locale.US, "%,.2f", remainingBudget)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Spent $symbol${String.format(Locale.US, "%.0f", totalExpensesSum)} of $symbol${String.format(Locale.US, "%.0f", limit)} Limit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Circular Progress Ring
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val animatedPercentage by animateFloatAsState(
                        targetValue = spendPercentage.coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 1000),
                        label = "progress"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Track
                        drawCircle(
                            color = Color(0xFF334155), // Slate-700 Track from design
                            radius = size.minDimension / 2 - 8,
                            style = Stroke(width = 8.dp.toPx())
                        )
                        // Fill indicator
                        drawArc(
                            color = Color(0xFF6366F1), // Indigo Fill Accent
                            startAngle = -90f,
                            sweepAngle = animatedPercentage * 360f,
                            useCenter = false,
                            size = Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()),
                            topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Text(
                        text = "${(spendPercentage * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metrics Bento Grid (Row layout matching Bento look)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bento Item 1: Total Saved
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .testTag("bento_total_saved"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(24.dp), // rounded-3xl
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Saved",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape)
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "$symbol${String.format(Locale.US, "%,.2f", totalSaved)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }

            // Bento Item 2: Active Goals
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .testTag("bento_active_goals"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(24.dp), // rounded-3xl
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Goals",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f), CircleShape)
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = if (activeGoalsCount < 10) "0$activeGoalsCount Goals" else "$activeGoalsCount Goals",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Data Visualizations Header
        Text(
            text = "Spending Analytics",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Category Pie Chart / Donut Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("spending_distribution_donut_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(24.dp), // rounded-3xl
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Category Distribution",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(16.dp))

                val categorySums = expenses.groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
                    .toList()
                    .sortedByDescending { it.second }

                if (categorySums.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recorded expenditures to map distribution.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                } else {
                    var selectedSliceIndex by remember { mutableStateOf(-1) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Interactive Donut Canvas
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .aspectRatio(1.0f)
                                .pointerInput(categorySums) {
                                    detectTapGestures { offset ->
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val x = offset.x - center.x
                                        val y = offset.y - center.y
                                        var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
                                        if (angle < 0) angle += 360f

                                        var currentAngle = 0f
                                        var foundIndex = -1
                                        for (i in categorySums.indices) {
                                            val sweep = (categorySums[i].second / totalExpensesSum).toFloat() * 360f
                                            val endAngle = currentAngle + sweep
                                            if (angle in currentAngle..endAngle) {
                                                foundIndex = i
                                                break
                                            }
                                            currentAngle = endAngle
                                        }
                                        selectedSliceIndex = if (selectedSliceIndex == foundIndex) -1 else foundIndex
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                var currentAngle = 0f
                                for (i in categorySums.indices) {
                                    val cat = categorySums[i].first
                                    val sum = categorySums[i].second
                                    val sweep = (sum / totalExpensesSum).toFloat() * 360f
                                    val isSelected = selectedSliceIndex == i
                                    val strokeWidth = if (isSelected) 22.dp.toPx() else 14.dp.toPx()

                                    drawArc(
                                        color = getCategoryColor(cat),
                                        startAngle = currentAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                    )
                                    currentAngle += sweep
                                }
                            }

                            // Center text
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (selectedSliceIndex != -1) {
                                    Text(
                                        text = categorySums[selectedSliceIndex].first,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "$symbol${String.format(Locale.US, "%.0f", categorySums[selectedSliceIndex].second)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        text = "Total Spent",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "$symbol${String.format(Locale.US, "%.0f", totalExpensesSum)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Category Legends Flow Column
                        Column(
                            modifier = Modifier.weight(1.0f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categorySums.take(5).forEachIndexed { index, pair ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            selectedSliceIndex = if (selectedSliceIndex == index) -1 else index
                                        }
                                        .background(
                                            if (selectedSliceIndex == index) Color(0xFF334155) else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(getCategoryColor(pair.first), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${pair.first} (${(pair.second / totalExpensesSum * 100).toInt()}%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6-Month Trend Area Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("expenditures_trend_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(24.dp), // rounded-3xl
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "6-Month Spending Trend",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Build trend data
                val trendPoints = getSixMonthTrendData(expenses)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val paddingX = 40.dp.toPx()
                        val paddingY = 20.dp.toPx()
                        val width = size.width - 2 * paddingX
                        val height = size.height - 2 * paddingY

                        val maxVal = trendPoints.maxOfOrNull { it.second }?.coerceAtLeast(100.0) ?: 100.0

                        val coordinates = trendPoints.mapIndexed { idx, pair ->
                            val x = paddingX + idx * (width / 5f)
                            val y = paddingY + height - ((pair.second / maxVal) * height).toFloat()
                            Offset(x, y)
                        }

                        // Draw background lines and labels
                        val numGridLines = 3
                        for (i in 0..numGridLines) {
                            val gridY = paddingY + i * (height / numGridLines)
                            drawLine(
                                color = Color(0xFF334155), // Slate-700 Grid lines
                                start = Offset(paddingX, gridY),
                                end = Offset(paddingX + width, gridY),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Area Path
                        if (coordinates.isNotEmpty()) {
                            val areaPath = Path().apply {
                                moveTo(coordinates.first().x, paddingY + height)
                                for (coord in coordinates) {
                                    lineTo(coord.x, coord.y)
                                }
                                lineTo(coordinates.last().x, paddingY + height)
                                close()
                            }
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF6366F1).copy(alpha = 0.35f),
                                        Color(0xFF1E293B).copy(alpha = 0.0f)
                                    ),
                                    startY = paddingY,
                                    endY = paddingY + height
                                )
                            )

                            // Line Path
                            val linePath = Path().apply {
                                moveTo(coordinates.first().x, coordinates.first().y)
                                for (i in 1 until coordinates.size) {
                                    lineTo(coordinates[i].x, coordinates[i].y)
                                }
                            }
                            drawPath(
                                path = linePath,
                                color = Color(0xFF6366F1),
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Points
                            for (coord in coordinates) {
                                drawCircle(
                                    color = Color.White,
                                    radius = 5.dp.toPx(),
                                    center = coord
                                )
                                drawCircle(
                                    color = Color(0xFF6366F1),
                                    radius = 3.dp.toPx(),
                                    center = coord
                                )
                            }
                        }
                    }
                }

                // Month text legends below canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (tp in trendPoints) {
                        Text(
                            text = tp.first.uppercase(Locale.US),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

// Generate last 6 months trend list
private fun getSixMonthTrendData(expenses: List<Expense>): List<Pair<String, Double>> {
    val result = mutableListOf<Pair<String, Double>>()
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, -5)

    val monthFormatter = SimpleDateFormat("MMM", Locale.US)
    val yearMonthFormatter = SimpleDateFormat("yyyy-MM", Locale.US)

    for (i in 0..5) {
        val monthLabel = monthFormatter.format(cal.time)
        val yearMonthKey = yearMonthFormatter.format(cal.time) // "2026-07"

        // Sum up expenses matching this year-month key
        val sum = expenses.filter { it.date.startsWith(yearMonthKey) }.sumOf { it.amount }
        result.add(monthLabel to sum)
        cal.add(Calendar.MONTH, 1)
    }

    return result
}
