package com.quanlychitieuapp.ui.debt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quanlychitieuapp.R
import com.quanlychitieuapp.domain.model.Debt
import com.quanlychitieuapp.domain.model.DebtType
import com.quanlychitieuapp.domain.model.PaymentStatus
import com.quanlychitieuapp.ui.theme.AppTheme
import com.quanlychitieuapp.utils.DateUtils
import com.quanlychitieuapp.utils.MoneyFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDebtDetailScreen(
    personId: Long,
    viewModel: DebtViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDebtDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val allPersonDebts = uiState.debts.filter { it.personId == personId }
    if (allPersonDebts.isEmpty()) {
        // Fallback if data not ready or person deleted
        return
    }

    val personName = allPersonDebts.first().personName
    val lentDebts = allPersonDebts.filter { it.type == DebtType.LENDING }
    val borrowedDebts = allPersonDebts.filter { it.type == DebtType.BORROWING }

    val lentTotal = lentDebts.sumOf { it.remainingAmount }
    val borrowedTotal = borrowedDebts.sumOf { it.remainingAmount }
    val netDebt = lentTotal - borrowedTotal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debt_person_profile_title, personName)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.debt_back_desc))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Net Debt Card
            val isNetLending = netDebt > 0
            val isNetBorrowing = netDebt < 0
            val netColor = if (isNetLending) AppTheme.extendedColors.lending else if (isNetBorrowing) AppTheme.extendedColors.borrowing else MaterialTheme.colorScheme.onSurfaceVariant
            val netText = if (isNetLending) stringResource(R.string.debt_person_net_they_owe) else if (isNetBorrowing) stringResource(R.string.debt_person_net_we_owe) else stringResource(R.string.debt_person_net_settled)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(netText, style = MaterialTheme.typography.titleMedium, color = netColor)
                Spacer(modifier = Modifier.height(8.dp))
                if (netDebt != 0L) {
                    Text(
                        MoneyFormatter.formatVND(abs(netDebt)),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = netColor
                    )
                }
            }

            // Split into Section
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (lentDebts.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.debt_person_history_lending, personName, MoneyFormatter.formatVND(lentTotal)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.extendedColors.lending
                        )
                    }
                    items(lentDebts, key = { it.id }) { debt ->
                        DebtHistoryItemCard(debt = debt, onClick = { onNavigateToDebtDetail(debt.id) })
                    }
                }

                if (borrowedDebts.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.debt_person_history_borrowing, personName, MoneyFormatter.formatVND(borrowedTotal)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.extendedColors.borrowing
                        )
                    }
                    items(borrowedDebts, key = { it.id }) { debt ->
                        DebtHistoryItemCard(debt = debt, onClick = { onNavigateToDebtDetail(debt.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun DebtHistoryItemCard(debt: Debt, onClick: () -> Unit) {
    val isLending = debt.type == DebtType.LENDING
    val color = if (isLending) AppTheme.extendedColors.lending else AppTheme.extendedColors.borrowing
    
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
                    text = MoneyFormatter.formatVND(debt.principalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (debt.dueDate != null) {
                        Text(
                            text = stringResource(R.string.debt_person_due_date, DateUtils.formatDate(debt.dueDate)),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (debt.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.debt_person_created_at, DateUtils.formatDate(debt.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!debt.description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = debt.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (debt.paymentStatus == PaymentStatus.PAID) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.debt_paid_desc),
                            tint = AppTheme.extendedColors.lending,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.debt_status_paid),
                            style = MaterialTheme.typography.labelMedium,
                            color = AppTheme.extendedColors.lending
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.debt_status_remaining_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = MoneyFormatter.formatVND(debt.remainingAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
