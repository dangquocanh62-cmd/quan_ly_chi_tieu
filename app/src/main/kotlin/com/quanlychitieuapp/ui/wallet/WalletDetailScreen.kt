package com.quanlychitieuapp.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quanlychitieuapp.R
import com.quanlychitieuapp.domain.model.CashFlowDirection
import com.quanlychitieuapp.domain.model.CashFlowItem
import com.quanlychitieuapp.domain.model.CashFlowSource
import com.quanlychitieuapp.ui.theme.AppShapes
import com.quanlychitieuapp.ui.theme.ErrorRed
import com.quanlychitieuapp.ui.theme.MoneyLarge
import com.quanlychitieuapp.ui.theme.MoneySmall
import com.quanlychitieuapp.ui.theme.PrimaryBlue
import com.quanlychitieuapp.ui.theme.SecondaryGreen
import com.quanlychitieuapp.ui.theme.cardShadow
import com.quanlychitieuapp.utils.DateUtils
import com.quanlychitieuapp.utils.IconMapper
import com.quanlychitieuapp.utils.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailScreen(
    viewModel: WalletDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: (Long) -> Unit,
    onCashFlowClick: (Long, CashFlowSource) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.wallet?.name ?: stringResource(R.string.wallet_detail_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.wallet_back_desc))
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.wallet != null) {
                FloatingActionButton(
                    onClick = { onNavigateToAddTransaction(uiState.wallet!!.id) },
                    containerColor = PrimaryBlue,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.wallet_add_transaction_desc))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Wallet Card Header
            uiState.wallet?.let { wallet ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = IconMapper.walletIconToVector(wallet.iconKey),
                        contentDescription = stringResource(R.string.wallet_icon_desc),
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        MoneyFormatter.format(wallet.balance),
                        style = MoneyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Date Filter Pagination
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.wallet_previous_month_desc))
                }
                Text(
                    stringResource(R.string.wallet_month_year, uiState.currentMonth + 1, uiState.currentYear),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.wallet_next_month_desc))
                }
            }

            // Transactions List
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.wallet_no_cashflow),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp), // Space for FAB
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.items) { cf ->
                        CashFlowItemComponent(
                            item = cf,
                            onClick = { onCashFlowClick(cf.sourceId, cf.sourceType) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CashFlowItemComponent(
    item: CashFlowItem,
    onClick: () -> Unit
) {
    val isOut = item.direction == CashFlowDirection.OUT
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Category icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isOut)
                                ErrorRed.copy(alpha = 0.1f)
                            else
                                SecondaryGreen.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconMapper.categoryIconToVector(item.iconKey),
                        contentDescription = stringResource(R.string.wallet_transaction_category_desc),
                        modifier = Modifier.size(22.dp),
                        tint = if (isOut) ErrorRed else SecondaryGreen
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    item.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        DateUtils.formatDate(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Text(
                (if (isOut) "-" else "+") + MoneyFormatter.format(item.amount),
                style = MoneySmall,
                color = if (isOut) ErrorRed else SecondaryGreen
            )
        }
    }
}
