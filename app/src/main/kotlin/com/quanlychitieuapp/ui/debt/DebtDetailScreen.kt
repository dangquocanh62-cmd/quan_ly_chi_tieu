package com.quanlychitieuapp.ui.debt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quanlychitieuapp.R
import com.quanlychitieuapp.domain.model.Debt
import com.quanlychitieuapp.domain.model.DebtPayment
import com.quanlychitieuapp.domain.model.DebtType
import com.quanlychitieuapp.ui.theme.AppTheme
import com.quanlychitieuapp.utils.DateUtils
import com.quanlychitieuapp.utils.MoneyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    debtId: Long,
    viewModel: DebtViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToPayment: (Long) -> Unit // Màn hình nhỏ để nhập Payment
) {
    var debt by remember { mutableStateOf<Debt?>(null) }
    var payments by remember { mutableStateOf<List<DebtPayment>>(emptyList()) }
    var personName by remember { mutableStateOf("") }
    var splitExpenseDesc by remember { mutableStateOf<String?>(null) }
    var splitExpenseTotal by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    LaunchedEffect(debtId) {
        val result = viewModel.getDebtWithDetails(debtId)
        debt = result.first
        personName = result.second?.name ?: ""
        payments = result.third
        
        result.first?.splitExpenseId?.let { splitId ->
            val splitResult = viewModel.getSplitExpenseDetails(splitId)
            splitResult?.let {
                splitExpenseDesc = it.first.description
                splitExpenseTotal = it.first.totalAmount
            }
        }
    }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val currentDebt = debt
    if (currentDebt == null) {
        // Loading
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debt_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.debt_back_desc))
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(debtId) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.debt_edit_action))
                    }
                    IconButton(onClick = { 
                        if (currentDebt.paidAmount > 0) {
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.debt_error_delete_locked))
                            }
                        } else {
                            showDeleteConfirm = true 
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.debt_delete_action))
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
            // Debt Info Hero
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                    Text(
                        text = if (currentDebt.type == DebtType.LENDING) stringResource(R.string.debt_detail_lending_person, personName) else stringResource(R.string.debt_detail_borrowing_person, personName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.debt_detail_principal))
                        Text(MoneyFormatter.formatVND(currentDebt.principalAmount), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.debt_detail_paid))
                        Text(MoneyFormatter.formatVND(currentDebt.paidAmount), color = AppTheme.extendedColors.lending)
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.debt_detail_remaining), fontWeight = FontWeight.Bold)
                        Text(MoneyFormatter.formatVND(currentDebt.remainingAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }

                    if (currentDebt.dueDate != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.debt_detail_due_date, DateUtils.formatDate(currentDebt.dueDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (currentDebt.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (!currentDebt.description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.debt_detail_note, currentDebt.description), style = MaterialTheme.typography.bodySmall)
                    }

                    if (currentDebt.splitExpenseId != null && splitExpenseDesc != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.debt_detail_split_source), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.debt_detail_split_desc, splitExpenseDesc ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.debt_detail_split_total, MoneyFormatter.formatVND(splitExpenseTotal ?: 0L)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

            if (currentDebt.remainingAmount > 0) {
                Button(
                    onClick = { onNavigateToPayment(debtId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(if (currentDebt.type == DebtType.LENDING) stringResource(R.string.debt_detail_receive_action) else stringResource(R.string.debt_detail_pay_action))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.debt_detail_payment_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(payments) { payment ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(DateUtils.formatDate(payment.paymentDate), style = MaterialTheme.typography.bodyMedium)
                                if (!payment.note.isNullOrEmpty()) {
                                    Text(payment.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                MoneyFormatter.formatVND(payment.amount),
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.extendedColors.lending
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.debt_delete_confirm_title)) },
            text = { Text(stringResource(R.string.debt_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDebt(debtId, onSuccess = {
                        showDeleteConfirm = false
                        onNavigateBack()
                    }, onError = { err ->
                        showDeleteConfirm = false
                        scope.launch { snackbarHostState.showSnackbar(context.getString(err)) }
                    })
                }) {
                    Text(stringResource(R.string.debt_delete_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.debt_cancel_action))
                }
            }
        )
    }
}
