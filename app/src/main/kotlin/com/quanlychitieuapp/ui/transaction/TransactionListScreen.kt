package com.quanlychitieuapp.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quanlychitieuapp.R
import com.quanlychitieuapp.domain.model.Transaction
import com.quanlychitieuapp.ui.theme.AppShapes
import com.quanlychitieuapp.ui.theme.ErrorRed
import com.quanlychitieuapp.ui.theme.PrimaryBlue
import com.quanlychitieuapp.ui.theme.SecondaryGreen
import com.quanlychitieuapp.ui.theme.cardShadow
import com.quanlychitieuapp.utils.DateUtils
import com.quanlychitieuapp.utils.IconMapper
import com.quanlychitieuapp.utils.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    uiState: TransactionListUiState,
    onFilterChange: (TransactionFilter) -> Unit,
    onSearch: (String) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onAddTransaction: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transaction_title), fontWeight = FontWeight.SemiBold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.transaction_add_desc))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    onSearch(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.transaction_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = AppShapes.pill,
                singleLine = true
            )

            // Filter chips
            FilterRow(
                filter = uiState.filter,
                onFilterChange = onFilterChange
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.transactions.isEmpty()) {
                EmptyState()
            } else {
                // Group by date
                val grouped = uiState.transactions.groupBy {
                    DateUtils.formatDate(it.transactionDate)
                }

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    grouped.forEach { (date, txList) ->
                        item {
                            Text(
                                date,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        items(txList) { tx ->
                            TransactionRow(
                                transaction = tx,
                                onEdit = { onEditTransaction(tx.id) },
                                onDelete = { showDeleteConfirm = tx }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteConfirm?.let { tx ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(stringResource(R.string.transaction_delete_confirm_title)) },
            text = { Text(stringResource(R.string.transaction_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTransaction(tx)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) { Text(stringResource(R.string.transaction_delete_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text(stringResource(R.string.transaction_cancel_action)) }
            }
        )
    }
}

@Composable
private fun FilterRow(filter: TransactionFilter, onFilterChange: (TransactionFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = filter.type == null,
            onClick = { onFilterChange(filter.copy(type = null)) },
            label = { Text(stringResource(R.string.transaction_filter_all)) }
        )
        FilterChip(
            selected = filter.type == "expense",
            onClick = { onFilterChange(filter.copy(type = "expense")) },
            label = { Text(stringResource(R.string.transaction_filter_expense)) }
        )
        FilterChip(
            selected = filter.type == "income",
            onClick = { onFilterChange(filter.copy(type = "income")) },
            label = { Text(stringResource(R.string.transaction_filter_income)) }
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .cardShadow()
            .clickable { onEdit() },
        shape = AppShapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (transaction.type == "expense") ErrorRed.copy(0.1f)
                            else SecondaryGreen.copy(0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconMapper.categoryIconToVector(transaction.categoryKey),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (transaction.type == "expense") ErrorRed else SecondaryGreen
                    )
                }
                Spacer(Modifier.width(12.dp))
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
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (transaction.type == "expense") "-${MoneyFormatter.format(transaction.amount)}"
                    else "+${MoneyFormatter.format(transaction.amount)}",
                    style = com.quanlychitieuapp.ui.theme.MoneySmall,
                    color = if (transaction.type == "expense") ErrorRed else SecondaryGreen
                )
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.transaction_options_desc),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.transaction_edit_action))
                                }
                            },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = ErrorRed)
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.transaction_delete_action), color = ErrorRed)
                                }
                            },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        com.quanlychitieuapp.ui.components.EmptyStateIllustration(
            icon = Icons.Outlined.Receipt
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.transaction_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.transaction_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}
