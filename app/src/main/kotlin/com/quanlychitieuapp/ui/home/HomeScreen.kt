package com.quanlychitieuapp.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quanlychitieuapp.domain.model.Transaction
import com.quanlychitieuapp.domain.model.Wallet
import com.quanlychitieuapp.ui.theme.ErrorRed
import com.quanlychitieuapp.ui.theme.MoneyLarge
import com.quanlychitieuapp.ui.theme.MoneySmall
import com.quanlychitieuapp.ui.theme.PrimaryBlue
import com.quanlychitieuapp.ui.theme.PrimaryBlueDark
import com.quanlychitieuapp.ui.theme.SecondaryGreen
import com.quanlychitieuapp.ui.theme.AppShapes
import com.quanlychitieuapp.ui.theme.AppTheme
import com.quanlychitieuapp.ui.theme.cardShadow
import androidx.compose.ui.res.stringResource
import com.quanlychitieuapp.R
import com.quanlychitieuapp.utils.DateUtils
import com.quanlychitieuapp.utils.IconMapper
import com.quanlychitieuapp.utils.MoneyFormatter
import java.util.Calendar

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddSplitExpense: () -> Unit,
    onOpenChat: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onWalletClick: (Long?) -> Unit,
    onNavigateToDebtList: () -> Unit
) {
    val listState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val showExtended = !isScrolled && uiState.recentTransactions.isEmpty()

    Scaffold(
        floatingActionButton = {
            if (showExtended) {
                ExtendedFloatingActionButton(
                    onClick = onOpenChat,
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                    text = { Text(stringResource(R.string.home_quick_entry)) }
                )
            } else {
                FloatingActionButton(
                    onClick = onOpenChat,
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Chat, contentDescription = stringResource(R.string.home_add_expense_desc))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                // Header gradient card
                TotalBalanceCard(
                    totalBalance = uiState.totalBalance,
                    monthlyIncome = uiState.monthlyIncome,
                    monthlyExpense = uiState.monthlyExpense,
                    onAddExpense = onAddExpense,
                    onAddIncome = onAddIncome,
                    onAddSplitExpense = onAddSplitExpense
                )
            }

            item {
                // Debt Summary Widget
                Spacer(modifier = Modifier.height(16.dp))
                HomeDebtSummaryCard(
                    totalLending = uiState.totalLending,
                    totalBorrowing = uiState.totalBorrowing,
                    onClick = onNavigateToDebtList
                )
            }

            item {
                // Wallet list
                Spacer(modifier = Modifier.height(16.dp))
                WalletsSection(
                    wallets = uiState.wallets,
                    onWalletClick = onWalletClick
                )
            }

            item {
                // Recent transactions header
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.home_recent_transactions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (uiState.recentTransactions.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyTransactionsState()
                }
            } else {
                items(uiState.recentTransactions) { tx ->
                    TransactionItem(
                        transaction = tx,
                        onClick = { onTransactionClick(tx.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalBalanceCard(
    totalBalance: Long,
    monthlyIncome: Long,
    monthlyExpense: Long,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddSplitExpense: () -> Unit
) {
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.home_total_balance),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                MoneyFormatter.format(totalBalance),
                style = MoneyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.home_month_year, currentMonth, currentYear),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Monthly stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MonthlyStatChip(
                    label = stringResource(R.string.home_income),
                    amount = monthlyIncome,
                    isIncome = true,
                    modifier = Modifier.weight(1f)
                )
                MonthlyStatChip(
                    label = stringResource(R.string.home_expense),
                    amount = monthlyExpense,
                    isIncome = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = AppShapes.medium
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.home_expense), style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onAddSplitExpense,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = AppShapes.medium
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.home_split_expense), style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onAddIncome,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = AppShapes.medium
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.home_income), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun MonthlyStatChip(
    label: String,
    amount: Long,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                MoneyFormatter.format(amount),
                style = MoneySmall,
                color = if (isIncome) SecondaryGreen else ErrorRed
            )
        }
    }
}

@Composable
private fun WalletsSection(
    wallets: List<Wallet>,
    onWalletClick: (Long?) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.home_my_wallets),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = { onWalletClick(null) }) {
                Text(stringResource(R.string.home_view_all))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(wallets) { wallet ->
                WalletCard(wallet = wallet, onClick = { onWalletClick(wallet.id) })
            }
        }
    }
}

@Composable
private fun WalletCard(wallet: Wallet, onClick: () -> Unit) {
    Surface(
        shape = AppShapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .width(160.dp)
            .cardShadow(AppShapes.large)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = IconMapper.walletIconToVector(wallet.iconKey),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                wallet.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                MoneyFormatter.format(wallet.balance),
                style = MoneySmall,
                color = if (wallet.balance >= 0) MaterialTheme.colorScheme.onSurface else ErrorRed
            )
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .cardShadow()
            .clickable(onClick = onClick),
        shape = AppShapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (transaction.type == "expense")
                                ErrorRed.copy(alpha = 0.1f)
                            else
                                SecondaryGreen.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconMapper.categoryIconToVector(transaction.categoryKey),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (transaction.type == "expense") ErrorRed else SecondaryGreen
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        IconMapper.categoryKeyToName(transaction.categoryKey),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (transaction.description.isNotEmpty()) {
                        Text(
                            transaction.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                    Text(
                        DateUtils.formatDate(transaction.transactionDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Text(
                if (transaction.type == "expense") "-${MoneyFormatter.format(transaction.amount)}"
                else "+${MoneyFormatter.format(transaction.amount)}",
                style = MoneySmall,
                color = if (transaction.type == "expense") ErrorRed else SecondaryGreen
            )
        }
    }
}

@Composable
private fun EmptyTransactionsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        com.quanlychitieuapp.ui.components.EmptyStateIllustration(
            icon = Icons.Outlined.ReceiptLong
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.home_empty_transactions),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            stringResource(R.string.home_empty_transactions_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
@Composable
fun HomeDebtSummaryCard(
    totalLending: Long,
    totalBorrowing: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_lending),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTheme.extendedColors.lending
                )
                Text(
                    text = MoneyFormatter.formatVND(totalLending),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Divider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    text = stringResource(R.string.home_borrowing),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTheme.extendedColors.borrowing
                )
                Text(
                    text = MoneyFormatter.formatVND(totalBorrowing),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
