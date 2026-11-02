package com.quanlychitieuapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quanlychitieuapp.R
import com.quanlychitieuapp.ui.theme.AppShapes
import com.quanlychitieuapp.ui.theme.PrimaryBlue
import com.quanlychitieuapp.ui.theme.cardShadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToBackup: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.userSettings

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold) })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SectionHeader(Icons.Filled.AccountBalanceWallet, stringResource(R.string.settings_section_finance))
                SettingsItem(
                    icon = Icons.Filled.AttachMoney,
                    title = stringResource(R.string.settings_currency),
                    subtitle = stringResource(R.string.settings_currency_vnd)
                )
                HorizontalDivider(modifier = Modifier.padding(start = 50.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsItem(
                    icon = Icons.Filled.Security,
                    title = stringResource(R.string.settings_debt_threshold),
                    subtitle = stringResource(R.string.settings_debt_threshold_desc, com.quanlychitieuapp.utils.MoneyFormatter.formatVND(settings.debtThreshold)),
                    onClick = { showThresholdDialog = true }
                )
            }

            item {
                SectionHeader(Icons.Filled.SmartToy, stringResource(R.string.settings_section_gemini))
                SettingsSwitchItem(
                    icon = Icons.Filled.SmartToy,
                    title = stringResource(R.string.settings_gemini_enable),
                    subtitle = stringResource(R.string.settings_gemini_desc),
                    checked = settings.geminiEnabled,
                    onCheckedChange = { viewModel.updateGeminiEnabled(it) }
                )
                if (settings.geminiEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(start = 50.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsItem(
                        icon = Icons.Filled.Key,
                        title = stringResource(R.string.settings_gemini_api_key),
                        subtitle = if (uiState.hasApiKey) stringResource(R.string.settings_gemini_api_key_set) else stringResource(R.string.settings_gemini_api_key_unset),
                        onClick = { showApiKeyDialog = true }
                    )
                }
            }

            item {
                SectionHeader(Icons.Filled.Schedule, stringResource(R.string.settings_section_reminder))
                SettingsSwitchItem(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.settings_reminder_daily),
                    subtitle = stringResource(R.string.settings_reminder_daily_desc),
                    checked = settings.reminderEnabled,
                    onCheckedChange = { viewModel.updateReminderEnabled(it) }
                )
                if (settings.reminderEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(start = 50.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsItem(
                        icon = Icons.Filled.AccessTime,
                        title = stringResource(R.string.settings_reminder_time),
                        subtitle = String.format("%02d:%02d", settings.reminderHour, settings.reminderMinute),
                        onClick = { showTimePicker = true }
                    )
                }
            }

            item {
                SectionHeader(Icons.Filled.Storage, stringResource(R.string.settings_section_data))
                SettingsItem(
                    icon = Icons.Filled.CloudUpload,
                    title = stringResource(R.string.settings_backup_restore),
                    subtitle = stringResource(R.string.settings_backup_restore_desc),
                    onClick = onNavigateToBackup
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = AppShapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.settings_version, "1.0.0"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            stringResource(R.string.settings_gemini_model, "gemini-2.0-flash"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    if (showThresholdDialog) {
        var inputStr by remember { mutableStateOf(settings.debtThreshold.toString()) }
        AlertDialog(
            onDismissRequest = { showThresholdDialog = false },
            title = { Text(stringResource(R.string.settings_threshold_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = inputStr,
                    onValueChange = { inputStr = com.quanlychitieuapp.utils.AmountInputFilter.filter(it) },
                    label = { Text(stringResource(R.string.settings_amount_vnd)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newVal = inputStr.toLongOrNull()
                    if (newVal != null && newVal >= 0) {
                        viewModel.updateDebtThreshold(newVal)
                    }
                    showThresholdDialog = false
                }) {
                    Text(stringResource(R.string.settings_action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showThresholdDialog = false }) { Text(stringResource(R.string.settings_action_cancel)) }
            }
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.settings_time_picker_title)) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateReminderTime(
                        hour = timePickerState.hour,
                        minute = timePickerState.minute
                    )
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.settings_action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.settings_action_cancel)) }
            }
        )
    }

    if (showApiKeyDialog) {
        var keyInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text(stringResource(R.string.settings_gemini_api_key)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.settings_gemini_security_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text(stringResource(R.string.settings_api_key_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (keyInput.isNotBlank()) {
                        viewModel.saveGeminiApiKey(keyInput.trim())
                    }
                    showApiKeyDialog = false
                }) { Text(stringResource(R.string.settings_action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) { Text(stringResource(R.string.settings_action_cancel)) }
            }
        )
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = androidx.compose.ui.graphics.Color.Transparent,
        onClick = onClick ?: {},
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            if (onClick != null) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
