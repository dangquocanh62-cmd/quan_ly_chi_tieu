package com.quanlychitieuapp.ui.split

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quanlychitieuapp.R
import com.quanlychitieuapp.data.local.entity.CategoryKeys
import com.quanlychitieuapp.data.repository.WalletRepository
import com.quanlychitieuapp.domain.model.Wallet
import com.quanlychitieuapp.domain.service.SplitExpenseService
import com.quanlychitieuapp.ui.theme.SecondaryGreen
import com.quanlychitieuapp.ui.theme.AppShapes
import com.quanlychitieuapp.ui.transaction.CategoryGrid
import com.quanlychitieuapp.utils.AmountInputFilter
import com.quanlychitieuapp.utils.IconMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitExpenseScreen(
    walletRepository: WalletRepository,
    splitExpenseService: SplitExpenseService,
    onSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val wallets by walletRepository.getAllWallets().collectAsState(initial = emptyList())

    var totalAmount by remember { mutableStateOf("") }
    var selectedCategoryKey by remember { mutableStateOf(CategoryKeys.FOOD) }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var description by remember { mutableStateOf("") }
    
    // Participants State: First is always owner
    var splitMethod by remember { mutableStateOf("equal") } // "equal" or "custom"
    
    data class ParticipantInputState(
        val id: String = java.util.UUID.randomUUID().toString(),
        var name: String,
        var isOwner: Boolean = false,
        var customAmount: String = ""
    )
    val ownerNameStr = stringResource(R.string.split_owner_default_name)
    val participants = remember { mutableStateListOf(
        ParticipantInputState(name = ownerNameStr, isOwner = true)
    ) }

    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    
    var showConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(700)
            onSuccess()
        }
    }

    LaunchedEffect(wallets) {
        if (selectedWallet == null) {
            selectedWallet = wallets.firstOrNull { it.isDefault } ?: wallets.firstOrNull()
        }
    }

    val expenseCategories = listOf(
        CategoryKeys.FOOD, CategoryKeys.SHOPPING, CategoryKeys.TRANSPORT,
        CategoryKeys.ENTERTAINMENT, CategoryKeys.EDUCATION, CategoryKeys.LIVING,
        CategoryKeys.HEALTH, CategoryKeys.TELECOM, CategoryKeys.OTHER_EXPENSE
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.split_title), fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.split_back_desc))
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
                // Info Section
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { totalAmount = AmountInputFilter.filter(it) },
                    label = { Text(stringResource(R.string.split_total_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Text(stringResource(R.string.split_category), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                CategoryGrid(
                    categories = expenseCategories,
                    selectedCategoryKey = selectedCategoryKey,
                    onSelect = { selectedCategoryKey = it }
                )

                // Wallet selector
                if (wallets.isNotEmpty()) {
                    var walletExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = walletExpanded,
                        onExpandedChange = { walletExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedWallet?.name ?: stringResource(R.string.split_wallet_placeholder),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = AppShapes.medium,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = walletExpanded,
                            onDismissRequest = { walletExpanded = false }
                        ) {
                            wallets.forEach { wallet ->
                                DropdownMenuItem(
                                    text = { Text(wallet.name) },
                                    onClick = { selectedWallet = wallet; walletExpanded = false }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.split_note)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.medium,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // Participants Section
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.split_participants_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { participants.add(ParticipantInputState(name = "")) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.split_add_person))
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = splitMethod == "equal",
                        onClick = { splitMethod = "equal" },
                        label = { Text(stringResource(R.string.split_method_equal)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = splitMethod == "custom",
                        onClick = { splitMethod = "custom" },
                        label = { Text(stringResource(R.string.split_method_custom)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                participants.forEachIndexed { index, participant ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = participant.name,
                            onValueChange = { newVal ->
                                participants[index] = participant.copy(name = newVal)
                            },
                            label = { Text(if (participant.isOwner) stringResource(R.string.split_owner_name) else stringResource(R.string.split_participant_name)) },
                            modifier = Modifier.weight(1f),
                            shape = AppShapes.medium,
                            readOnly = participant.isOwner,
                            singleLine = true
                        )
                        if (splitMethod == "custom") {
                            OutlinedTextField(
                                value = participant.customAmount,
                                onValueChange = { newVal ->
                                    participants[index] = participant.copy(customAmount = AmountInputFilter.filter(newVal))
                                },
                                label = { Text(stringResource(R.string.split_amount)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = AppShapes.medium,
                                singleLine = true
                            )
                        }
                        if (!participant.isOwner) {
                            FilledTonalIconButton(
                                onClick = { participants.removeAt(index) },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = AppShapes.medium,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.split_action_delete))
                            }
                        } else {
                            // Empty box for alignment
                            Spacer(Modifier.size(48.dp))
                        }
                    }
                }

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }

                Button(
                    onClick = {
                        val amountLong = totalAmount.replace(",", "").replace(".", "").toLongOrNull() ?: 0L
                        if (amountLong <= 0) {
                            error = context.getString(R.string.split_err_amount)
                            return@Button
                        }
                        if (participants.any { !it.isOwner && it.name.isBlank() }) {
                            error = context.getString(R.string.split_err_name)
                            return@Button
                        }
                        if (splitMethod == "custom") {
                            val sum = participants.sumOf { 
                                it.customAmount.replace(",", "").replace(".", "").toLongOrNull() ?: 0L 
                            }
                            if (sum != amountLong) {
                                error = context.getString(R.string.split_err_sum_mismatch, sum, amountLong)
                                return@Button
                            }
                        }
                        error = null
                        showConfirmation = true
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    enabled = !isSubmitting
                ) {
                    Text(stringResource(R.string.split_action_confirm))
                }
            }
        }

        if (showConfirmation) {
            AlertDialog(
                onDismissRequest = { showConfirmation = false },
                title = { Text(stringResource(R.string.split_dialog_title)) },
                text = {
                    Column {
                        val amountLong = totalAmount.replace(",", "").replace(".", "").toLongOrNull() ?: 0L
                        Text(stringResource(R.string.split_dialog_amount, totalAmount))
                        Text(stringResource(R.string.split_dialog_wallet, selectedWallet?.name ?: ""))
                        Spacer(Modifier.height(8.dp))
                        
                        val serviceInputs = participants.map {
                            SplitExpenseService.ParticipantInput(
                                personName = if (it.isOwner) null else it.name,
                                manuallyEnteredShare = if (splitMethod == "custom") it.customAmount.replace(",", "").replace(".", "").toLongOrNull() ?: 0L else null
                            )
                        }
                        
                        // Fake calc to show logic
                        var ownerShare = 0L
                        var othersShare = 0L
                        if (splitMethod == "equal") {
                            val base = amountLong / participants.size
                            val rem = amountLong % participants.size
                            ownerShare = base + rem
                            othersShare = base
                        } else {
                            ownerShare = participants.find { it.isOwner }?.customAmount?.replace(",", "")?.replace(".", "")?.toLongOrNull() ?: 0L
                        }
                        
                        participants.forEach { 
                            val amt = if(splitMethod == "custom") {
                                it.customAmount.replace(",", "").replace(".", "").toLongOrNull() ?: 0L
                            } else {
                                if (it.isOwner) ownerShare else othersShare
                            }
                            if (it.isOwner) {
                                Text(stringResource(R.string.split_dialog_person_owner, com.quanlychitieuapp.utils.MoneyFormatter.format(amt)))
                            } else {
                                Text(stringResource(R.string.split_dialog_person, it.name, com.quanlychitieuapp.utils.MoneyFormatter.format(amt)))
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.split_dialog_debt, com.quanlychitieuapp.utils.MoneyFormatter.format(amountLong - ownerShare)), color = SecondaryGreen, fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showConfirmation = false
                        isSubmitting = true
                        coroutineScope.launch {
                            val amountLong = totalAmount.replace(",", "").replace(".", "").toLongOrNull() ?: 0L
                            val inputs = participants.map {
                                SplitExpenseService.ParticipantInput(
                                    personName = if (it.isOwner) null else it.name,
                                    manuallyEnteredShare = if (splitMethod == "custom") it.customAmount.replace(",", "").replace(".", "").toLongOrNull() ?: 0L else null
                                )
                            }
                            
                            val res = splitExpenseService.createSplitExpense(
                                totalAmount = amountLong,
                                categoryKey = selectedCategoryKey,
                                walletId = selectedWallet!!.id,
                                description = description,
                                splitMethod = splitMethod,
                                participants = inputs,
                                transactionDate = System.currentTimeMillis()
                            )
                            isSubmitting = false
                            res.fold(
                                onSuccess = { showSuccess = true },
                                onFailure = { error = it.message }
                            )
                        }
                    }) {
                        Text(stringResource(R.string.split_action_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmation = false }) {
                        Text(stringResource(R.string.split_action_edit))
                    }
                }
            )
        }

        // Success overlay
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.split_success_desc),
                    modifier = Modifier.size(72.dp),
                    tint = SecondaryGreen
                )
            }
        }
    }
}
