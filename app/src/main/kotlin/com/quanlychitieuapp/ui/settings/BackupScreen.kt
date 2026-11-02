package com.quanlychitieuapp.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quanlychitieuapp.R
import com.quanlychitieuapp.data.backup.BackupResult
import com.quanlychitieuapp.data.backup.BackupService
import com.quanlychitieuapp.data.local.AppDatabase
import com.quanlychitieuapp.ui.theme.AppShapes
import com.quanlychitieuapp.ui.theme.ErrorRed
import com.quanlychitieuapp.ui.theme.PrimaryBlue
import com.quanlychitieuapp.ui.theme.SecondaryGreen
import com.quanlychitieuapp.ui.theme.cardShadow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    db: AppDatabase,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backupService = remember { BackupService(context, db) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf<Uri?>(null) }

    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Export JSON launcher
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                isLoading = true
                val result = backupService.exportToJson(it)
                isLoading = false
                when (result) {
                    is BackupResult.Success -> { isSuccess = true; statusMessage = result.message }
                    is BackupResult.Error -> { isSuccess = false; statusMessage = result.message }
                }
            }
        }
    }

    // Export CSV launcher
    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                isLoading = true
                val result = backupService.exportToCsv(it)
                isLoading = false
                when (result) {
                    is BackupResult.Success -> { isSuccess = true; statusMessage = result.message }
                    is BackupResult.Error -> { isSuccess = false; statusMessage = result.message }
                }
            }
        }
    }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { showImportDialog = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.backup_back_desc))
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            statusMessage?.let { msg ->
                Surface(
                    shape = AppShapes.medium,
                    color = if (isSuccess) SecondaryGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f)
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(16.dp),
                        color = if (isSuccess) SecondaryGreen else ErrorRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.backup_section_export), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            ActionCard(
                icon = Icons.Default.Archive,
                title = stringResource(R.string.backup_export_json),
                description = stringResource(R.string.backup_export_json_desc),
                buttonLabel = stringResource(R.string.backup_export_json),
                onClick = {
                    exportJsonLauncher.launch("chiTieu_backup_$dateStr.json")
                }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 50.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

            ActionCard(
                icon = Icons.Default.InsertChart,
                title = stringResource(R.string.backup_export_csv),
                description = stringResource(R.string.backup_export_csv_desc),
                buttonLabel = stringResource(R.string.backup_export_csv),
                onClick = {
                    exportCsvLauncher.launch("chiTieu_transactions_$dateStr.csv")
                }
            )

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.backup_section_import), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            ActionCard(
                icon = Icons.Default.Folder,
                title = stringResource(R.string.backup_import_json),
                description = stringResource(R.string.backup_import_json_desc),
                buttonLabel = stringResource(R.string.backup_choose_json),
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }
            )

            Spacer(Modifier.height(16.dp))
            Surface(
                shape = AppShapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_note_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.backup_note_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    // Import mode dialog
    showImportDialog?.let { uri ->
        AlertDialog(
            onDismissRequest = { showImportDialog = null },
            title = { Text(stringResource(R.string.backup_import_mode_title)) },
            text = {
                Text(stringResource(R.string.backup_import_mode_desc))
            },
            confirmButton = {
                TextButton(onClick = {
                    val importUri = uri
                    showImportDialog = null
                    coroutineScope.launch {
                        isLoading = true
                        val result = backupService.importFromJson(importUri, merge = false)
                        isLoading = false
                        when (result) {
                            is BackupResult.Success -> { isSuccess = true; statusMessage = result.message }
                            is BackupResult.Error -> { isSuccess = false; statusMessage = result.message }
                        }
                    }
                }) { Text(stringResource(R.string.backup_import_replace), color = ErrorRed) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showImportDialog = null }) { Text(stringResource(R.string.backup_action_cancel)) }
                    TextButton(onClick = {
                        val importUri = uri
                        showImportDialog = null
                        coroutineScope.launch {
                            isLoading = true
                            val result = backupService.importFromJson(importUri, merge = true)
                            isLoading = false
                            when (result) {
                                is BackupResult.Success -> { isSuccess = true; statusMessage = result.message }
                                is BackupResult.Error -> { isSuccess = false; statusMessage = result.message }
                            }
                        }
                    }) { Text(stringResource(R.string.backup_import_merge)) }
                }
            }
        )
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Surface(
        color = androidx.compose.ui.graphics.Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(onClick = onClick) {
                Text(buttonLabel)
            }
        }
    }
}
