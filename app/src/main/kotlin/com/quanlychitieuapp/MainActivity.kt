package com.quanlychitieuapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.quanlychitieuapp.ai.GeminiParser
import com.quanlychitieuapp.ai.LocalParser
import com.quanlychitieuapp.data.repository.TransactionRepository
import com.quanlychitieuapp.data.repository.WalletRepository
import com.quanlychitieuapp.domain.service.TransactionService
import com.quanlychitieuapp.domain.service.TransferService
import com.quanlychitieuapp.notification.ReminderScheduler
import com.quanlychitieuapp.ui.Screen
import com.quanlychitieuapp.ui.chat.ChatScreen
import com.quanlychitieuapp.ui.chat.ChatViewModel
import com.quanlychitieuapp.ui.home.HomeScreen
import com.quanlychitieuapp.ui.home.HomeViewModel
import com.quanlychitieuapp.ui.theme.QuanLyChiTieuTheme
import com.quanlychitieuapp.ui.transaction.TransactionListScreen
import com.quanlychitieuapp.ui.transaction.TransactionListViewModel
import com.quanlychitieuapp.ui.budget.BudgetScreen
import com.quanlychitieuapp.ui.budget.BudgetViewModel
import com.quanlychitieuapp.ui.debt.DebtListScreen
import com.quanlychitieuapp.ui.debt.DebtDetailScreen
import com.quanlychitieuapp.ui.debt.DebtViewModel
import com.quanlychitieuapp.ui.statistics.StatisticsScreen
import com.quanlychitieuapp.ui.statistics.StatisticsViewModel
import com.quanlychitieuapp.ui.settings.SettingsScreen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        val app = application as QuanLyChiTieuApp
        val db = app.database
        val settingsRepo = app.settingsRepository
        val geminiKeyRepo = app.geminiKeyRepository

        // Initialize repositories
        val walletRepo = WalletRepository(db.walletDao())
        val transactionRepo = TransactionRepository(db.transactionDao(), db.walletDao())
        val debtRepo = com.quanlychitieuapp.data.repository.DebtRepository(db)
        val splitExpenseRepo = com.quanlychitieuapp.data.repository.SplitExpenseRepository(db)

        // Initialize services
        val transactionService = TransactionService(db.transactionDao(), db.walletDao())
        val transferService = TransferService(db)
        val splitExpenseService = com.quanlychitieuapp.domain.service.SplitExpenseService(db, transactionService, debtRepo)

        // Initialize parsers
        val localParser = LocalParser()
        val geminiParser = GeminiParser(this) {
            geminiKeyRepo.getApiKey()
        }

        setContent {
            // Read dark mode preference
            QuanLyChiTieuTheme(darkTheme = false) {
                MainNavigation(
                    walletRepo = walletRepo,
                    transactionRepo = transactionRepo,
                    transactionService = transactionService,
                    transferService = transferService,
                    localParser = localParser,
                    geminiParser = geminiParser,
                    debtRepo = debtRepo,
                    splitExpenseRepo = splitExpenseRepo,
                    splitExpenseService = splitExpenseService,
                    db = db,
                    settingsRepo = settingsRepo,
                    geminiKeyRepo = geminiKeyRepo
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    walletRepo: WalletRepository,
    transactionRepo: TransactionRepository,
    transactionService: TransactionService,
    transferService: TransferService,
    localParser: LocalParser,
    geminiParser: GeminiParser,
    debtRepo: com.quanlychitieuapp.data.repository.DebtRepository,
    splitExpenseRepo: com.quanlychitieuapp.data.repository.SplitExpenseRepository,
    splitExpenseService: com.quanlychitieuapp.domain.service.SplitExpenseService,
    db: com.quanlychitieuapp.data.local.AppDatabase,
    settingsRepo: com.quanlychitieuapp.data.repository.SettingsRepository,
    geminiKeyRepo: com.quanlychitieuapp.data.repository.GeminiKeyRepository
) {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem("Trang chủ", Icons.Default.Home, Screen.Home.route),
        BottomNavItem("Giao dịch", Icons.Default.Receipt, Screen.Transactions.route),
        BottomNavItem("Thống kê", Icons.Default.BarChart, Screen.Statistics.route),
        BottomNavItem("Ngân sách", Icons.Default.AccountBalance, Screen.Budget.route),
        BottomNavItem("Cài đặt", Icons.Default.Settings, Screen.Settings.route)
    )

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { 
                                Text(
                                    text = item.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            alwaysShowLabel = false,
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = com.quanlychitieuapp.ui.theme.PrimaryBlue.copy(alpha = 0.2f),
                                selectedIconColor = com.quanlychitieuapp.ui.theme.PrimaryBlue,
                                selectedTextColor = com.quanlychitieuapp.ui.theme.PrimaryBlue
                            ),
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val vm = remember {
                    HomeViewModel(walletRepo, transactionRepo, debtRepo)
                }
                val uiState by vm.uiState.collectAsState()
                HomeScreen(
                    uiState = uiState,
                    onAddExpense = { navController.navigate(Screen.AddTransaction.createRoute("expense")) },
                    onAddIncome = { navController.navigate(Screen.AddTransaction.createRoute("income")) },
                    onAddSplitExpense = { navController.navigate("split_expense/add") },
                    onOpenChat = { navController.navigate(Screen.Chat.route) },
                    onTransactionClick = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                    onWalletClick = { id -> 
                        if (id == null) navController.navigate(Screen.WalletList.route)
                        else navController.navigate(Screen.WalletDetail.createRoute(id))
                    },
                    onNavigateToDebtList = { navController.navigate(Screen.DebtList.route) }
                )
            }

            composable(Screen.Chat.route) {
                val settings by settingsRepo.userSettingsFlow.collectAsState(initial = com.quanlychitieuapp.data.repository.UserSettings(true, 21, 0, false, 1_000_000L))
                val isGeminiEnabled = settings.geminiEnabled
                val debtThreshold = settings.debtThreshold
                val applicationContext = LocalContext.current.applicationContext
                
                val vm = remember(isGeminiEnabled, debtThreshold, applicationContext) {
                    ChatViewModel(
                        localParser = localParser,
                        geminiParser = geminiParser,
                        transactionService = transactionService,
                        walletRepository = walletRepo,
                        debtRepository = debtRepo,
                        splitExpenseService = splitExpenseService,
                        isGeminiEnabled = isGeminiEnabled,
                        debtConfirmationThreshold = debtThreshold,
                        applicationContext = applicationContext
                    )
                }
                val uiState by vm.uiState.collectAsState()
                ChatScreen(
                    uiState = uiState,
                    onSendMessage = { vm.processInput(it) },
                    onConfirm = { vm.confirmPending() },
                    onCancel = { vm.cancelPending() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Transactions.route) {
                val vm = remember {
                    TransactionListViewModel(transactionRepo, transactionService)
                }
                val uiState by vm.uiState.collectAsState()
                TransactionListScreen(
                    uiState = uiState,
                    onFilterChange = { vm.applyFilter(it) },
                    onSearch = { vm.search(it) },
                    onEditTransaction = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                    onDeleteTransaction = { tx -> vm.deleteTransaction(tx) },
                    onAddTransaction = { navController.navigate(Screen.AddTransaction.createRoute("expense")) }
                )
            }

            composable(Screen.Statistics.route) {
                val vm = remember {
                    StatisticsViewModel(transactionRepo)
                }
                val uiState by vm.uiState.collectAsState()
                StatisticsScreen(
                    uiState = uiState,
                    onPeriodChange = { vm.changePeriod(it) }
                )
            }

            composable(Screen.Budget.route) {
                val vm = remember {
                    BudgetViewModel(db)
                }
                val uiState by vm.uiState.collectAsState()
                BudgetScreen(
                    uiState = uiState,
                    onAddBudget = { vm.showAddDialog() },
                    onDeleteBudget = { vm.deleteBudget(it) },
                    onDismissDialog = { vm.dismissDialog() },
                    onConfirmBudget = { categoryKey, amount -> vm.saveBudget(categoryKey, amount) }
                )
            }

            composable(Screen.Settings.route) {
                val vm = remember {
                    com.quanlychitieuapp.ui.settings.SettingsViewModel(
                        settingsRepo, geminiKeyRepo, navController.context.applicationContext
                    )
                }
                SettingsScreen(
                    viewModel = vm,
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) }
                )
            }

            composable(Screen.Backup.route) {
                com.quanlychitieuapp.ui.settings.BackupScreen(
                    db = db,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DebtList.route) {
                val vm = remember { com.quanlychitieuapp.ui.debt.DebtViewModel(debtRepo, walletRepo, splitExpenseRepo) }
                com.quanlychitieuapp.ui.debt.DebtListScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { id -> navController.navigate(Screen.PersonDebtDetail.createRoute(id)) },
                    onNavigateToAdd = { navController.navigate(Screen.AddDebt.route) }
                )
            }

            composable(
                route = Screen.DebtDetail.route,
                arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id")
                if (id == null) {
                    navController.popBackStack()
                    return@composable
                }
                val vm = remember { com.quanlychitieuapp.ui.debt.DebtViewModel(debtRepo, walletRepo, splitExpenseRepo) }
                com.quanlychitieuapp.ui.debt.DebtDetailScreen(
                    debtId = id,
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { debtId -> navController.navigate(Screen.EditDebt.createRoute(debtId)) },
                    onNavigateToPayment = { debtId -> navController.navigate(Screen.AddDebtPayment.createRoute(debtId)) }
                )
            }

            composable(Screen.AddDebt.route) {
                val vm = remember { com.quanlychitieuapp.ui.debt.DebtViewModel(debtRepo, walletRepo, splitExpenseRepo) }
                com.quanlychitieuapp.ui.debt.AddDebtScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditDebt.route,
                arguments = listOf(androidx.navigation.navArgument("debtId") { type = androidx.navigation.NavType.LongType })
            ) { backStackEntry ->
                val debtId = backStackEntry.arguments?.getLong("debtId")
                if (debtId == null) {
                    navController.popBackStack()
                    return@composable
                }
                val vm = remember { com.quanlychitieuapp.ui.debt.DebtViewModel(debtRepo, walletRepo, splitExpenseRepo) }
                com.quanlychitieuapp.ui.debt.EditDebtScreen(
                    debtId = debtId,
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AddDebtPayment.route,
                arguments = listOf(androidx.navigation.navArgument("debtId") { type = androidx.navigation.NavType.LongType })
            ) { backStackEntry ->
                val debtId = backStackEntry.arguments?.getLong("debtId")
                if (debtId == null) {
                    navController.popBackStack()
                    return@composable
                }
                val vm = remember { com.quanlychitieuapp.ui.debt.DebtViewModel(debtRepo, walletRepo, splitExpenseRepo) }
                com.quanlychitieuapp.ui.debt.AddDebtPaymentScreen(
                    debtId = debtId,
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.PersonDebtDetail.route,
                arguments = listOf(androidx.navigation.navArgument("personId") { type = androidx.navigation.NavType.LongType })
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getLong("personId")
                if (personId == null) {
                    navController.popBackStack()
                    return@composable
                }
                val vm = remember { com.quanlychitieuapp.ui.debt.DebtViewModel(debtRepo, walletRepo, splitExpenseRepo) }
                com.quanlychitieuapp.ui.debt.PersonDebtDetailScreen(
                    personId = personId,
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDebtDetail = { id -> navController.navigate(Screen.DebtDetail.createRoute(id)) }
                )
            }

            composable(
                route = Screen.AddTransaction.route,
                arguments = listOf(
                    androidx.navigation.navArgument("type") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("walletId") { 
                        type = androidx.navigation.NavType.StringType
                        nullable = true 
                    }
                )
            ) { backStack ->
                val type = backStack.arguments?.getString("type") ?: "expense"
                val walletIdParam = backStack.arguments?.getString("walletId")?.toLongOrNull()
                
                com.quanlychitieuapp.ui.transaction.AddTransactionScreen(
                    type = type,
                    prefilledWalletId = walletIdParam,
                    walletRepository = walletRepo,
                    transactionService = transactionService,
                    onSuccess = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.EditTransaction.route) { backStack ->
                val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                com.quanlychitieuapp.ui.transaction.EditTransactionScreen(
                    transactionId = id,
                    walletRepository = walletRepo,
                    transactionRepository = transactionRepo,
                    transactionService = transactionService,
                    onSuccess = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.WalletList.route) {
                val vm = remember {
                    com.quanlychitieuapp.ui.wallet.WalletListViewModel(walletRepo, transferService)
                }
                val uiState by vm.uiState.collectAsState()
                com.quanlychitieuapp.ui.wallet.WalletListScreen(
                    uiState = uiState,
                    onSetDefaultWallet = { walletId -> vm.setDefaultWallet(walletId) },
                    onDeleteWallet = { wallet -> vm.deleteWallet(wallet) },
                    onDismissDeleteError = { vm.dismissDeleteError() },
                    onNavigateBack = { navController.popBackStack() },
                    onTransfer = { navController.navigate(Screen.Transfer.route) },
                    onWalletClick = { id -> navController.navigate(Screen.WalletDetail.createRoute(id)) }
                )
            }

            composable(
                route = Screen.WalletDetail.route,
                arguments = listOf(androidx.navigation.navArgument("walletId") { type = androidx.navigation.NavType.LongType })
            ) { backStackEntry ->
                val walletId = backStackEntry.arguments?.getLong("walletId")
                if (walletId == null) {
                    navController.popBackStack()
                    return@composable
                }
                val vm = remember { 
                    com.quanlychitieuapp.ui.wallet.WalletDetailViewModel(walletId, walletRepo, db) 
                }
                com.quanlychitieuapp.ui.wallet.WalletDetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddTransaction = { wId -> 
                        navController.navigate(Screen.AddTransaction.createRoute("expense", wId)) 
                    },
                    onCashFlowClick = { sourceId, sourceType ->
                        when (sourceType) {
                            com.quanlychitieuapp.domain.model.CashFlowSource.TRANSACTION -> {
                                navController.navigate(Screen.EditTransaction.createRoute(sourceId))
                            }
                            com.quanlychitieuapp.domain.model.CashFlowSource.DEBT,
                            com.quanlychitieuapp.domain.model.CashFlowSource.DEBT_PAYMENT -> {
                                navController.navigate(Screen.DebtDetail.createRoute(sourceId))
                            }
                            com.quanlychitieuapp.domain.model.CashFlowSource.TRANSFER -> {
                                // Transfer Detail is not yet implemented
                            }
                        }
                    }
                )
            }

            composable(Screen.Transfer.route) {
                com.quanlychitieuapp.ui.wallet.TransferScreen(
                    walletRepository = walletRepo,
                    transferService = transferService,
                    onSuccess = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }



            composable("split_expense/add") {
                com.quanlychitieuapp.ui.split.SplitExpenseScreen(
                    walletRepository = walletRepo,
                    splitExpenseService = splitExpenseService,
                    onSuccess = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
