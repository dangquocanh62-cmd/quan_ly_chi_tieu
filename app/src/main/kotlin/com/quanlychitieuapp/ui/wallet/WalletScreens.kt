package com.quanlychitieuapp.ui.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quanlychitieuapp.R
import com.quanlychitieuapp.data.repository.WalletRepository
import com.quanlychitieuapp.domain.model.Wallet
import com.quanlychitieuapp.domain.service.TransferService
import com.quanlychitieuapp.ui.theme.AppShapes
import com.quanlychitieuapp.ui.theme.ErrorRed
import com.quanlychitieuapp.ui.theme.MoneyLarge
import com.quanlychitieuapp.ui.theme.MoneySmall
import com.quanlychitieuapp.ui.theme.PrimaryBlue
import com.quanlychitieuapp.ui.theme.cardShadow
import com.quanlychitieuapp.utils.IconMapper
import com.quanlychitieuapp.utils.MoneyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletListScreen(
    uiState: WalletListUiState,
    onSetDefaultWallet: (Long) -> Unit,
    onDeleteWallet: (Wallet) -> Unit,
    onDismissDeleteError: () -> Unit,
    onNavigateBack: () -> Unit,
    onTransfer: () -> Unit,
    onWalletClick: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wallet_my_wallets), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.wallet_back_desc))
                    }
                },
                actions = {
                    IconButton(onClick = onTransfer) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.wallet_transfer_desc))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 16.dp)
                ) {
                    Text(
                        stringResource(R.string.wallet_total_assets), 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        MoneyFormatter.format(uiState.totalBalance),
                        style = MoneyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(uiState.wallets) { wallet ->
                WalletCard(
                    wallet = wallet,
                    onSetDefault = { onSetDefaultWallet(wallet.id) },
                    onDelete = { onDeleteWallet(wallet) },
                    onClick = { onWalletClick(wallet.id) }
                )
            }
        }
    }

    uiState.deleteErrorRes?.let { errRes ->
        AlertDialog(
            onDismissRequest = onDismissDeleteError,
            title = { Text(stringResource(R.string.wallet_delete_error_title)) },
            text = { Text(stringResource(errRes)) },
            confirmButton = {
                TextButton(onClick = onDismissDeleteError) { Text(stringResource(R.string.wallet_ok)) }
            }
        )
    }
}

@Composable
private fun WalletCard(wallet: Wallet, onSetDefault: () -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        shape = AppShapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.cardShadow(AppShapes.large),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = IconMapper.walletIconToVector(wallet.iconKey),
                    contentDescription = stringResource(R.string.wallet_icon_desc),
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(wallet.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        IconMapper.walletTypeToName(wallet.type),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        MoneyFormatter.format(wallet.balance),
                        style = MoneySmall,
                        color = if (wallet.balance >= 0) MaterialTheme.colorScheme.onSurface else ErrorRed
                    )
                    if (wallet.isDefault) {
                        Text(
                            stringResource(R.string.wallet_default),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (!wallet.isDefault) {
                    IconButton(onClick = onSetDefault) {
                        Icon(Icons.Default.Star, contentDescription = stringResource(R.string.wallet_set_default_desc), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    }
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.wallet_delete_desc), tint = ErrorRed.copy(alpha = 0.7f))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.wallet_delete_confirm_title, wallet.name)) },
            text = { Text(stringResource(R.string.wallet_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) { Text(stringResource(R.string.wallet_delete_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.wallet_cancel_action)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    walletRepository: WalletRepository,
    transferService: TransferService,
    onSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val wallets by walletRepository.getAllWallets().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var fromWallet by remember { mutableStateOf<Wallet?>(null) }
    var toWallet by remember { mutableStateOf<Wallet?>(null) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(wallets) {
        if (fromWallet == null) fromWallet = wallets.firstOrNull { it.isDefault } ?: wallets.firstOrNull()
        if (toWallet == null) toWallet = wallets.getOrNull(1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wallet_transfer_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.wallet_back_desc))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // From wallet
            var fromExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = fromExpanded, onExpandedChange = { fromExpanded = it }) {
                OutlinedTextField(
                    value = fromWallet?.let { it.name } ?: stringResource(R.string.wallet_select_source),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    label = { Text(stringResource(R.string.wallet_from_wallet)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(fromExpanded) }
                )
                ExposedDropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                    wallets.forEach { wallet ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = IconMapper.walletIconToVector(wallet.iconKey),
                                        contentDescription = stringResource(R.string.wallet_icon_desc),
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("${wallet.name} (${MoneyFormatter.format(wallet.balance)})")
                                }
                            },
                            onClick = { fromWallet = wallet; fromExpanded = false }
                        )
                    }
                }
            }

            // To wallet
            var toExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = toExpanded, onExpandedChange = { toExpanded = it }) {
                OutlinedTextField(
                    value = toWallet?.let { it.name } ?: stringResource(R.string.wallet_select_destination),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    label = { Text(stringResource(R.string.wallet_to_wallet)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(toExpanded) }
                )
                ExposedDropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                    wallets.forEach { wallet ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = IconMapper.walletIconToVector(wallet.iconKey),
                                        contentDescription = stringResource(R.string.wallet_icon_desc),
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(wallet.name)
                                }
                            },
                            onClick = { toWallet = wallet; toExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = com.quanlychitieuapp.utils.AmountInputFilter.filter(it) },
                label = { Text(stringResource(R.string.wallet_amount_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.wallet_note_optional)) },
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(error!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val amountLong = amount.replace(",", "").replace(".", "").toLongOrNull()
                    val from = fromWallet
                    val to = toWallet
                    if (amountLong == null || amountLong <= 0) { error = context.getString(R.string.wallet_error_invalid_amount); return@Button }
                    if (from == null || to == null) { error = context.getString(R.string.wallet_error_missing_wallets); return@Button }
                    if (from.id == to.id) { error = context.getString(R.string.wallet_error_same_wallet); return@Button }
                    error = null
                    isSubmitting = true
                    coroutineScope.launch {
                        transferService.transfer(from.id, to.id, amountLong, description, System.currentTimeMillis())
                            .fold(
                                onSuccess = { onSuccess() },
                                onFailure = { e -> error = e.message; isSubmitting = false }
                            )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.wallet_transfer_action))
            }
        }
    }
}
