package com.quanlychitieuapp.ui.debt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.quanlychitieuapp.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quanlychitieuapp.ui.theme.AppTheme
import com.quanlychitieuapp.utils.MoneyFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtListScreen(
    viewModel: DebtViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit, // This now takes personId
    onNavigateToAdd: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debt_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.debt_back_desc))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.debt_add_desc))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DebtSummaryCard(
                    title = stringResource(R.string.debt_summary_lending),
                    amount = uiState.isLendingTotal,
                    color = AppTheme.extendedColors.lending,
                    modifier = Modifier.weight(1f)
                )
                DebtSummaryCard(
                    title = stringResource(R.string.debt_summary_borrowing),
                    amount = uiState.isBorrowingTotal,
                    color = AppTheme.extendedColors.borrowing,
                    modifier = Modifier.weight(1f)
                )
            }

            // Filter Chips
            ScrollableTabRow(
                selectedTabIndex = DebtFilter.entries.indexOf(uiState.currentFilter),
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                DebtFilter.entries.forEachIndexed { index, filter ->
                    Tab(
                        selected = uiState.currentFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        text = { Text(stringResource(filter.titleRes)) }
                    )
                }
            }

            // List of Persons
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.personSummaries, key = { it.personId }) { summary ->
                    PersonDebtCard(
                        summary = summary,
                        onClick = { onNavigateToDetail(summary.personId) }
                    )
                }
                if (uiState.personSummaries.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            com.quanlychitieuapp.ui.components.EmptyStateIllustration(
                                icon = Icons.Outlined.AccountBox
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.debt_empty_data),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.debt_empty_message),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DebtSummaryCard(title: String, amount: Long, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MoneyFormatter.formatVND(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun PersonDebtCard(summary: PersonDebtSummary, onClick: () -> Unit) {
    val isNetLending = summary.netDebt > 0
    val isNetBorrowing = summary.netDebt < 0
    val netColor = if (isNetLending) AppTheme.extendedColors.lending else if (isNetBorrowing) AppTheme.extendedColors.borrowing else MaterialTheme.colorScheme.onSurfaceVariant
    val netText = if (isNetLending) stringResource(R.string.debt_status_they_owe) else if (isNetBorrowing) stringResource(R.string.debt_status_we_owe) else stringResource(R.string.debt_status_settled)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.personName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (summary.lendingTotal > 0) {
                    Text(
                        text = stringResource(R.string.debt_lending_total_prefix, MoneyFormatter.formatVND(summary.lendingTotal)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (summary.borrowingTotal > 0) {
                    Text(
                        text = stringResource(R.string.debt_borrowing_total_prefix, MoneyFormatter.formatVND(summary.borrowingTotal)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = netText,
                    style = MaterialTheme.typography.labelMedium,
                    color = netColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = MoneyFormatter.formatVND(abs(summary.netDebt)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = netColor
                )
            }
        }
    }
}
