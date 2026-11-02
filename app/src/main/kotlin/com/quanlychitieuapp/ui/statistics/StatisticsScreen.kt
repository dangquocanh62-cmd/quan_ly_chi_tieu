package com.quanlychitieuapp.ui.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quanlychitieuapp.R
import com.quanlychitieuapp.domain.model.CategoryStat
import com.quanlychitieuapp.ui.theme.AppShapes
import com.quanlychitieuapp.ui.theme.ErrorRed
import com.quanlychitieuapp.ui.theme.MoneySmall
import com.quanlychitieuapp.ui.theme.SecondaryGreen
import com.quanlychitieuapp.utils.IconMapper
import com.quanlychitieuapp.utils.MoneyFormatter
import kotlin.math.min

// Chart color palette (Thematic Navy/Teal/Warm)
private val chartColors = listOf(
    Color(0xFF1B3A5C), // Navy Base (1)
    Color(0xFF0F6E56), // Teal Base (2)
    Color(0xFFD85A30), // Warm Red (3)
    Color(0xFFBA7517), // Warning Orange (4)
    Color(0xFF5B396B), // Deep Plum/Purple (5) - analogous, high contrast
    Color(0xFF3A638C), // Lighter Slate Blue (6) - tonal shift from Navy
    Color(0xFFB34261), // Rose/Pinkish Red (7) - separates from Orange/Red
    Color(0xFFD1A02E), // Mustard/Gold (8) - lighter than Orange
    Color(0xFF607345)  // Olive/Sage Green (9) - separates from Teal
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    onPeriodChange: (ChartPeriod) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.statistics_title), fontWeight = FontWeight.SemiBold) })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                // Period selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChartPeriod.values().forEach { period ->
                        FilterChip(
                            selected = uiState.period == period,
                            onClick = { onPeriodChange(period) },
                            label = { 
                                Text(
                                    text = stringResource(period.displayNameRes),
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            } else if (uiState.totalExpense == 0L && uiState.totalIncome == 0L) {
                item { EmptyStatsState() }
            } else {
                item {
                    // Summary cards
                    StatsSummaryRow(uiState)
                }

                if (uiState.categoryStats.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.statistics_expense_by_category),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        // Pie chart
                        PieChart(
                            data = uiState.categoryStats,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(16.dp)
                        )
                    }

                    items(uiState.categoryStats) { stat ->
                        CategoryStatRow(stat, uiState.categoryStats.indexOf(stat))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSummaryRow(uiState: StatisticsUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = stringResource(R.string.statistics_income),
            amount = uiState.totalIncome,
            color = SecondaryGreen,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.statistics_expense),
            amount = uiState.totalExpense,
            color = ErrorRed,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.statistics_remaining),
            amount = uiState.net,
            color = if (uiState.net >= 0) SecondaryGreen else ErrorRed,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(label: String, amount: Long, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = AppShapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                MoneyFormatter.format(kotlin.math.abs(amount)),
                style = MoneySmall,
                color = color
            )
        }
    }
}

/**
 * Custom Compose Canvas pie chart - no external library required
 */
@Composable
private fun PieChart(data: List<CategoryStat>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    val total = data.sumOf { it.amount }.toFloat()
    if (total == 0f) return

    // Animation: draw gradually
    var animationPlayed by remember { mutableStateOf(false) }
    val animProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "pieChartAnim"
    )
    LaunchedEffect(Unit) { animationPlayed = true }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasSize = min(this.size.width, this.size.height)
            val radius = canvasSize / 2f
            val strokeWidth = canvasSize / 4f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            var startAngle = -90f
            data.forEachIndexed { index, stat ->
                val sweep = (stat.amount.toFloat() / total) * 360f * animProgress
                val color = chartColors[index % chartColors.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius + strokeWidth / 2, center.y - radius + strokeWidth / 2),
                    size = Size(radius * 2 - strokeWidth, radius * 2 - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
        // Center label: total expense
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.statistics_total_expense),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                MoneyFormatter.format(total.toLong()),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CategoryStatRow(stat: CategoryStat, colorIndex: Int) {
    val color = chartColors[colorIndex % chartColors.size]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = IconMapper.categoryIconToVector(stat.categoryKey),
            contentDescription = stringResource(R.string.statistics_category_desc),
            modifier = Modifier.size(18.dp),
            tint = color
        )
        Spacer(Modifier.width(4.dp))
        Text(
            IconMapper.categoryKeyToName(stat.categoryKey),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            MoneyFormatter.format(stat.amount),
            style = MoneySmall,
            color = ErrorRed
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "${"%.1f".format(stat.percentage)}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun EmptyStatsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyStateIllustration(icon = Icons.Outlined.BarChart)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.statistics_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.statistics_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

/**
 * Reusable empty state illustration — Canvas + Icon
 */
@Composable
private fun EmptyStateIllustration(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = primary.copy(alpha = 0.08f),
                radius = w * 0.48f,
                center = Offset(w * 0.5f, h * 0.5f)
            )
            drawCircle(
                color = primary.copy(alpha = 0.05f),
                radius = w * 0.3f,
                center = Offset(w * 0.7f, h * 0.3f)
            )
            drawCircle(
                color = primary.copy(alpha = 0.06f),
                radius = w * 0.2f,
                center = Offset(w * 0.25f, h * 0.7f)
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = primary.copy(alpha = 0.4f)
        )
    }
}
