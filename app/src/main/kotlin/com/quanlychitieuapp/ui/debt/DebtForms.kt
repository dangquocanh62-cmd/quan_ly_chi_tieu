package com.quanlychitieuapp.ui.debt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quanlychitieuapp.R
import com.quanlychitieuapp.domain.model.Debt
import com.quanlychitieuapp.domain.model.DebtType
import com.quanlychitieuapp.domain.model.Wallet
import com.quanlychitieuapp.utils.AmountInputFilter
import com.quanlychitieuapp.utils.MoneyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtScreen(
    viewModel: DebtViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var personName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var description by remember { mutableStateOf("") }
    var isLending by remember { mutableStateOf(true) } // true: Cho vay, false: Đi vay

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(uiState.wallets) {
        if (selectedWallet == null && uiState.wallets.isNotEmpty()) {
            selectedWallet = uiState.wallets.first()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debt_add_title)) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = isLending,
                    onClick = { isLending = true },
                    label = { Text(stringResource(R.string.debt_status_lending_title)) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = !isLending,
                    onClick = { isLending = false },
                    label = { Text(stringResource(R.string.debt_status_borrowing_title)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            
            val operationText = if (isLending) stringResource(R.string.debt_lending_wallet_hint) else stringResource(R.string.debt_borrowing_wallet_hint)
            
            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it },
                label = { Text(stringResource(R.string.debt_person_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = AmountInputFilter.filter(it) },
                label = { Text(stringResource(R.string.debt_amount_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Text(operationText, style = MaterialTheme.typography.bodyMedium)
            
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedWallet?.name ?: stringResource(R.string.debt_select_wallet),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.debt_linked_wallet_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    uiState.wallets.forEach { wallet ->
                        DropdownMenuItem(
                            text = { Text(wallet.name) },
                            onClick = {
                                selectedWallet = wallet
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.debt_note_optional)) },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val pName = personName.trim()
                    val amt = amount.toLongOrNull()
                    val wId = selectedWallet?.id

                    if (pName.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.debt_error_empty_name)) }
                        return@Button
                    }
                    if (amt == null || amt <= 0) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.debt_error_invalid_amount)) }
                        return@Button
                    }
                    if (wId == null) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.debt_error_empty_wallet)) }
                        return@Button
                    }

                    viewModel.addDebt(
                        personName = pName,
                        type = if(isLending) DebtType.LENDING else DebtType.BORROWING,
                        amount = amt,
                        walletId = wId,
                        dueDate = null,
                        description = description,
                        onSuccess = { 
                            Toast.makeText(context, context.getString(R.string.debt_success_add), Toast.LENGTH_SHORT).apply {
                                setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 150)
                            }.show()
                            onNavigateBack() 
                        },
                        onError = { err -> scope.launch { snackbarHostState.showSnackbar(context.getString(err)) } }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.debt_save_action))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDebtScreen(
    debtId: Long,
    viewModel: DebtViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var debt by remember { mutableStateOf<Debt?>(null) }
    
    var amount by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var description by remember { mutableStateOf("") }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    LaunchedEffect(debtId) {
        val (d, _, _) = viewModel.getDebtWithDetails(debtId)
        debt = d
        if (d != null) {
            amount = d.principalAmount.toString()
            description = d.description ?: ""
            selectedWallet = uiState.wallets.find { it.id == d.walletId }
        }
    }
    
    val currentDebt = debt
    if (currentDebt == null) return

    val isLocked = currentDebt.paidAmount > 0

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debt_edit_title)) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLocked) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        stringResource(R.string.debt_edit_locked_warning),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { if (!isLocked) amount = AmountInputFilter.filter(it) },
                label = { Text(stringResource(R.string.debt_principal_amount_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                readOnly = isLocked
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded && !isLocked, // disable expanding 
                onExpandedChange = { if (!isLocked) expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedWallet?.name ?: stringResource(R.string.debt_select_wallet),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.debt_linked_wallet_label)) },
                    trailingIcon = { if (!isLocked) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded && !isLocked,
                    onDismissRequest = { expanded = false }
                ) {
                    uiState.wallets.forEach { wallet ->
                        DropdownMenuItem(
                            text = { Text(wallet.name) },
                            onClick = {
                                selectedWallet = wallet
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.debt_note_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val amt = amount.toLongOrNull()
                    val wId = selectedWallet?.id

                    if (amt == null || amt <= 0) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.debt_error_invalid_amount)) }
                        return@Button
                    }
                    if (wId == null) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.debt_error_empty_wallet)) }
                        return@Button
                    }

                    viewModel.editDebt(
                        debtId = debtId,
                        principalAmount = amt,
                        walletId = wId,
                        dueDate = currentDebt.dueDate,
                        description = description,
                        onSuccess = { 
                            Toast.makeText(context, context.getString(R.string.debt_success_update), Toast.LENGTH_SHORT).apply {
                                setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 150)
                            }.show()
                            onNavigateBack() 
                        },
                        onError = { err -> scope.launch { snackbarHostState.showSnackbar(context.getString(err)) } }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.debt_update_action))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtPaymentScreen(
    debtId: Long,
    viewModel: DebtViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var debt by remember { mutableStateOf<Debt?>(null) }
    
    var amount by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var note by remember { mutableStateOf("") }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    LaunchedEffect(debtId) {
        val (d, _, _) = viewModel.getDebtWithDetails(debtId)
        debt = d
    }

    LaunchedEffect(uiState.wallets) {
        if (selectedWallet == null && uiState.wallets.isNotEmpty()) {
            selectedWallet = uiState.wallets.first()
        }
    }
    
    val currentDebt = debt
    if (currentDebt == null) return

    val isLending = currentDebt.type == DebtType.LENDING
    val maxAmount = currentDebt.remainingAmount

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isLending) stringResource(R.string.debt_payment_lending_title) else stringResource(R.string.debt_payment_borrowing_title)) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if (isLending) stringResource(R.string.debt_payment_lending_remaining) else stringResource(R.string.debt_payment_borrowing_remaining))
                    Text(MoneyFormatter.formatVND(maxAmount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = AmountInputFilter.filter(it) },
                    label = { Text(stringResource(R.string.debt_payment_amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { amount = maxAmount.toString() }) {
                    Text(stringResource(R.string.debt_payment_settle_action), fontWeight = FontWeight.Bold)
                }
            }

            // Explicit instructions as requested by User rule:
            Text(
                if (isLending) stringResource(R.string.debt_payment_receive_wallet_hint) else stringResource(R.string.debt_payment_pay_wallet_hint), 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedWallet?.name ?: stringResource(R.string.debt_select_wallet),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.debt_payment_wallet_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    uiState.wallets.forEach { wallet ->
                        DropdownMenuItem(
                            text = { Text(wallet.name) },
                            onClick = {
                                selectedWallet = wallet
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.debt_note_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val amt = amount.toLongOrNull()
                    val wId = selectedWallet?.id

                    if (amt == null || amt <= 0) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.debt_error_invalid_amount)) }
                        return@Button
                    }
                    if (amt > maxAmount) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.debt_error_payment_exceed, MoneyFormatter.formatVND(maxAmount))) }
                        return@Button
                    }
                    if (wId == null) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.debt_error_empty_wallet)) }
                        return@Button
                    }

                    viewModel.recordPayment(
                        debtId = debtId,
                        amount = amt,
                        walletId = wId,
                        note = note,
                        type = currentDebt.type,
                        onSuccess = { 
                            Toast.makeText(context, context.getString(R.string.debt_success_payment), Toast.LENGTH_SHORT).apply {
                                setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 150)
                            }.show()
                            onNavigateBack() 
                        },
                        onError = { err -> scope.launch { snackbarHostState.showSnackbar(context.getString(err)) } }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.debt_payment_complete_action))
            }
        }
    }
}
