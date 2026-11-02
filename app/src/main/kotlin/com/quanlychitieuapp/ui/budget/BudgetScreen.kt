package com.quanlychitieuapp.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.quanlychitieuapp.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quanlychitieuapp.data.local.entity.CategoryKeys
import com.quanlychitieuapp.domain.model.Budget
import com.quanlychitieuapp.ui.theme.AppShapes
import com.quanlychitieuapp.ui.theme.ErrorRed
import com.quanlychitieuapp.ui.theme.PrimaryBlue
import com.quanlychitieuapp.ui.theme.SecondaryGreen
import com.quanlychitieuapp.ui.theme.WarningOrange
import com.quanlychitieuapp.ui.theme.cardShadow
import com.quanlychitieuapp.utils.IconMapper
import com.quanlychitieuapp.utils.MoneyFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    uiState: BudgetUiState,
    onAddBudget: () -> Unit,
    onDeleteBudget: (Budget) -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmBudget: (String, Long) -> Unit
) {
    val month = Calendar.getInstance().get(Calendar.MONTH) + 1
    val year = Calendar.getInstance().get(Calendar.YEAR)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.budget_title, month, year), fontWeight = FontWeight.SemiBold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddBudget, shape = CircleShape) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.budget_add_desc))
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.budgets.isEmpty()) {
            EmptyBudgetState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.budgets) { budget ->
                    BudgetCard(
                        budget = budget,
                        onDelete = { onDeleteBudget(budget) }
                    )
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddBudgetDialog(
            onDismiss = onDismissDialog,
            onConfirm = onConfirmBudget
        )
    }
}

@Composable
private fun BudgetCard(budget: Budget, onDelete: () -> Unit) {
    val progressColor = when {
        budget.isOverBudget -> ErrorRed
        budget.isNearLimit -> WarningOrange
        else -> SecondaryGreen
    }

    Surface(
        shape = AppShapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.cardShadow(AppShapes.large)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = IconMapper.categoryIconToVector(budget.categoryKey),
                        contentDescription = stringResource(R.string.budget_category_desc),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            IconMapper.categoryKeyToName(budget.categoryKey),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.budget_this_month),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.budget_delete_action), tint = ErrorRed)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (budget.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${MoneyFormatter.format(budget.spentAmount)} / ${MoneyFormatter.format(budget.amountLimit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    "${budget.percentage}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            // Alert banner
            if (budget.isOverBudget) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = AppShapes.small,
                    color = ErrorRed.copy(alpha = 0.1f)
                ) {
                    Text(
                        stringResource(R.string.budget_alert_over, IconMapper.categoryKeyToName(budget.categoryKey)),
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (budget.isNearLimit) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = AppShapes.small,
                    color = WarningOrange.copy(alpha = 0.1f)
                ) {
                    Text(
                        stringResource(R.string.budget_alert_near, budget.percentage, IconMapper.categoryKeyToName(budget.categoryKey)),
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningOrange,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetDialog(onDismiss: () -> Unit, onConfirm: (String, Long) -> Unit) {
    val categories = listOf(
        CategoryKeys.FOOD, CategoryKeys.SHOPPING, CategoryKeys.TRANSPORT,
        CategoryKeys.ENTERTAINMENT, CategoryKeys.EDUCATION, CategoryKeys.LIVING,
        CategoryKeys.HEALTH, CategoryKeys.TELECOM, CategoryKeys.OTHER_EXPENSE
    )
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var amount by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.budget_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Category dropdown
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = IconMapper.categoryKeyToName(selectedCategory),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text(stringResource(R.string.budget_category_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = IconMapper.categoryIconToVector(cat),
                                            contentDescription = stringResource(R.string.budget_category_desc),
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(IconMapper.categoryKeyToName(cat))
                                    }
                                },
                                onClick = { selectedCategory = cat; expanded = false }
                            )
                        }
                    }
                }
                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = com.quanlychitieuapp.utils.AmountInputFilter.filter(it) },
                    label = { Text(stringResource(R.string.budget_limit_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amountLong = amount.replace(",", "").replace(".", "").toLongOrNull()
                if (amountLong != null && amountLong > 0) {
                    onConfirm(selectedCategory, amountLong)
                }
            }) { Text(stringResource(R.string.budget_save_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.budget_cancel_action)) }
        }
    )
}

@Composable
private fun EmptyBudgetState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        com.quanlychitieuapp.ui.components.EmptyStateIllustration(
            icon = Icons.Outlined.Savings
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.budget_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.budget_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}
