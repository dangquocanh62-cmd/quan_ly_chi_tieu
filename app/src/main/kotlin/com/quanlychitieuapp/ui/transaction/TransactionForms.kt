package com.quanlychitieuapp.ui.transaction

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quanlychitieuapp.R
import com.quanlychitieuapp.data.local.entity.CategoryKeys
import com.quanlychitieuapp.data.repository.WalletRepository
import com.quanlychitieuapp.data.repository.TransactionRepository
import com.quanlychitieuapp.domain.model.Wallet
import com.quanlychitieuapp.domain.service.TransactionService
import com.quanlychitieuapp.ui.theme.AppShapes
import com.quanlychitieuapp.ui.theme.SecondaryGreen
import com.quanlychitieuapp.utils.DateUtils
import com.quanlychitieuapp.utils.IconMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    type: String,
    prefilledWalletId: Long? = null,
    walletRepository: WalletRepository,
    transactionService: TransactionService,
    onSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val wallets by walletRepository.getAllWallets().collectAsState(initial = emptyList())

    var amount by remember { mutableStateOf("") }
    var selectedCategoryKey by remember {
        mutableStateOf(
            if (type == "expense") CategoryKeys.FOOD else CategoryKeys.SALARY
        )
    }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    // Auto-navigate after success animation
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(700)
            onSuccess()
        }
    }

    LaunchedEffect(wallets, prefilledWalletId) {
        if (selectedWallet == null) {
            val prefilled = wallets.find { it.id == prefilledWalletId }
            selectedWallet = prefilled ?: wallets.firstOrNull { it.isDefault } ?: wallets.firstOrNull()
        }
    }

    val expenseCategories = listOf(
        CategoryKeys.FOOD, CategoryKeys.SHOPPING, CategoryKeys.TRANSPORT,
        CategoryKeys.ENTERTAINMENT, CategoryKeys.EDUCATION, CategoryKeys.LIVING,
        CategoryKeys.HEALTH, CategoryKeys.TELECOM, CategoryKeys.OTHER_EXPENSE
    )
    val incomeCategories = listOf(
        CategoryKeys.SALARY, CategoryKeys.BONUS, CategoryKeys.FREELANCE, CategoryKeys.OTHER_INCOME
    )
    val categories = if (type == "expense") expenseCategories else incomeCategories

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (type == "expense") stringResource(R.string.transaction_add_expense_title) else stringResource(R.string.transaction_add_income_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.transaction_back_desc))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = com.quanlychitieuapp.utils.AmountInputFilter.filter(it) },
                label = { Text(stringResource(R.string.transaction_amount_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = error != null
            )

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Category
            Text(stringResource(R.string.transaction_category_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            CategoryGrid(
                categories = categories,
                selectedCategoryKey = selectedCategoryKey,
                onSelect = { selectedCategoryKey = it }
            )

            // Wallet selector
            if (wallets.isNotEmpty()) {
                Text(stringResource(R.string.transaction_wallet_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                var walletExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = walletExpanded,
                    onExpandedChange = { walletExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedWallet?.let {
                            it.name
                        } ?: stringResource(R.string.transaction_select_wallet),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) },
                        shape = AppShapes.medium
                    )
                    ExposedDropdownMenu(
                        expanded = walletExpanded,
                        onDismissRequest = { walletExpanded = false }
                    ) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = IconMapper.walletIconToVector(wallet.iconKey),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(wallet.name)
                                    }
                                },
                                onClick = {
                                    selectedWallet = wallet
                                    walletExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.transaction_note_optional)) },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.medium,
                maxLines = 2
            )

            // Submit button
            Button(
                onClick = {
                    val amountLong = amount.replace(",", "").replace(".", "").toLongOrNull()
                    if (amountLong == null || amountLong <= 0) {
                        error = context.getString(R.string.transaction_error_invalid_amount_add)
                        return@Button
                    }
                    val wallet = selectedWallet ?: run {
                        error = context.getString(R.string.transaction_error_missing_wallet)
                        return@Button
                    }
                    error = null
                    isSubmitting = true
                    coroutineScope.launch {
                        val result = if (type == "expense") {
                            transactionService.addExpense(
                                amountLong, selectedCategoryKey,
                                wallet.id, description, System.currentTimeMillis()
                            )
                        } else {
                            transactionService.addIncome(
                                amountLong, selectedCategoryKey,
                                wallet.id, description, System.currentTimeMillis()
                            )
                        }
                        isSubmitting = false
                        result.fold(
                            onSuccess = { showSuccess = true },
                            onFailure = { e -> error = e.message }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (type == "expense") stringResource(R.string.transaction_save_expense) else stringResource(R.string.transaction_save_income))
                }
            }
        }
    }

    // Success overlay
    AnimatedVisibility(
        visible = showSuccess,
        enter = fadeIn(tween(300)) + scaleIn(tween(400)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.transaction_success_desc),
                modifier = Modifier.size(72.dp),
                tint = SecondaryGreen
            )
        }
    }
    } // Box
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    transactionId: Long,
    walletRepository: WalletRepository,
    transactionRepository: TransactionRepository,
    transactionService: TransactionService,
    onSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val wallets by walletRepository.getAllWallets().collectAsState(initial = emptyList())
    var transaction by remember { mutableStateOf<com.quanlychitieuapp.domain.model.Transaction?>(null) }

    var amount by remember { mutableStateOf("") }
    var selectedCategoryKey by remember { mutableStateOf("food") }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(700)
            onSuccess()
        }
    }

    LaunchedEffect(transactionId) {
        transaction = transactionRepository.getTransactionById(transactionId)
        transaction?.let { tx ->
            amount = tx.amount.toString()
            selectedCategoryKey = tx.categoryKey
            description = tx.description
            selectedWallet = walletRepository.getWalletById(tx.walletId)
        }
    }

    val tx = transaction
    if (tx == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val categories = if (tx.type == "expense") {
        listOf(
            CategoryKeys.FOOD, CategoryKeys.SHOPPING, CategoryKeys.TRANSPORT,
            CategoryKeys.ENTERTAINMENT, CategoryKeys.EDUCATION, CategoryKeys.LIVING,
            CategoryKeys.HEALTH, CategoryKeys.TELECOM, CategoryKeys.OTHER_EXPENSE
        )
    } else {
        listOf(CategoryKeys.SALARY, CategoryKeys.BONUS, CategoryKeys.FREELANCE, CategoryKeys.OTHER_INCOME)
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transaction_edit_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.transaction_back_desc))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = com.quanlychitieuapp.utils.AmountInputFilter.filter(it) },
                label = { Text(stringResource(R.string.transaction_amount_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Text(stringResource(R.string.transaction_category_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            CategoryGrid(
                categories = categories,
                selectedCategoryKey = selectedCategoryKey,
                onSelect = { selectedCategoryKey = it }
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.transaction_note_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.medium,
                maxLines = 2
            )

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val newAmount = amount.replace(",", "").replace(".", "").toLongOrNull()
                    if (newAmount == null || newAmount <= 0) {
                        error = context.getString(R.string.transaction_error_invalid_amount_edit)
                        return@Button
                    }
                    val wallet = selectedWallet ?: run {
                        error = context.getString(R.string.transaction_error_missing_wallet)
                        return@Button
                    }
                    error = null
                    isSubmitting = true
                    coroutineScope.launch {
                        transactionService.editTransaction(
                            old = tx,
                            newAmount = newAmount,
                            newCategoryKey = selectedCategoryKey,
                            newWalletId = wallet.id,
                            newDescription = description,
                            newDate = tx.transactionDate
                        ).fold(
                            onSuccess = { showSuccess = true },
                            onFailure = { e -> error = e.message; isSubmitting = false }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                Text(stringResource(R.string.transaction_save_changes))
            }
        }
    }

    // Success overlay
    AnimatedVisibility(
        visible = showSuccess,
        enter = fadeIn(tween(300)) + scaleIn(tween(400)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.transaction_success_desc),
                modifier = Modifier.size(72.dp),
                tint = SecondaryGreen
            )
        }
    }
    } // close Box
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CategoryGrid(
    categories: List<String>,
    selectedCategoryKey: String,
    onSelect: (String) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { catKey ->
            FilterChip(
                selected = selectedCategoryKey == catKey,
                onClick = { onSelect(catKey) },
                label = { 
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            imageVector = IconMapper.categoryIconToVector(catKey),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = IconMapper.categoryKeyToName(catKey),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            )
        }
    }
}
