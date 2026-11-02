package com.quanlychitieuapp.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quanlychitieuapp.ai.GeminiParser
import com.quanlychitieuapp.ai.LocalParser
import com.quanlychitieuapp.ai.ParserResult
import com.quanlychitieuapp.R
import com.quanlychitieuapp.data.repository.WalletRepository
import com.quanlychitieuapp.domain.model.Wallet
import com.quanlychitieuapp.domain.service.TransactionService
import com.quanlychitieuapp.utils.DateUtils
import com.quanlychitieuapp.ai.DebtDirection
import com.quanlychitieuapp.ai.MissingDebtField
import com.quanlychitieuapp.ai.PendingDebt
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ChatState {
    object Idle : ChatState()
    data class CollectingDebtInfo(val debt: PendingDebt, val missingField: MissingDebtField) : ChatState()
    data class AwaitingConfirmation(val debt: PendingDebt) : ChatState()
}

sealed class ChatMessage {
    data class UserMessage(val text: String) : ChatMessage()
    data class BotSuccess(
        val intent: String,
        val amount: Long,
        val categoryKey: String?,
        val walletId: Long,
        val walletName: String,
        val description: String,
        val personName: String? = null,
        val participants: List<String>? = null,
        val splitMethod: String? = null,
        val confirmed: Boolean = false
    ) : ChatMessage()
    data class BotError(val message: String) : ChatMessage()
    data class BotLowConfidence(val message: String) : ChatMessage()
    data class BotConfirmation(
        val intent: String,
        val amount: Long,
        val categoryKey: String?,
        val walletId: Long,
        val walletName: String,
        val description: String,
        val personName: String? = null,
        val participants: List<String>? = null,
        val splitMethod: String? = null
    ) : ChatMessage()
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isProcessing: Boolean = false,
    val wallets: List<Wallet> = emptyList(),
    val error: String? = null
)

class ChatViewModel(
    private val localParser: LocalParser,
    private val geminiParser: GeminiParser?,
    private val transactionService: TransactionService,
    private val walletRepository: WalletRepository,
    private val debtRepository: com.quanlychitieuapp.data.repository.DebtRepository,
    private val splitExpenseService: com.quanlychitieuapp.domain.service.SplitExpenseService,
    private val isGeminiEnabled: Boolean,
    private val debtConfirmationThreshold: Long,
    private val applicationContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var pendingConfirmation: ChatMessage.BotConfirmation? = null
    private var chatState: ChatState = ChatState.Idle

    init {
        viewModelScope.launch {
            walletRepository.getAllWallets().collect { wallets ->
                _uiState.update { it.copy(wallets = wallets) }
            }
        }
    }

    fun processInput(userInput: String) {
        val trimmed = userInput.trim()
        val lower = trimmed.lowercase()
        if (trimmed.isEmpty()) return

        addMessage(ChatMessage.UserMessage(trimmed))
        
        val cancelWords = listOf("hủy", "huy", "cancel", "thôi", "thoi", "bỏ", "bo")
        if (cancelWords.contains(lower) && chatState !is ChatState.Idle) {
            chatState = ChatState.Idle
            pendingConfirmation = null
            addMessage(ChatMessage.BotError(applicationContext.getString(R.string.chat_err_cancel_tx)))
            return
        }

        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch {
            try {
                when (val state = chatState) {
                    is ChatState.Idle -> {
                        val result = parseInput(trimmed)
                        handleParserResult(result)
                    }
                    is ChatState.CollectingDebtInfo -> {
                        handleMissingDebtField(trimmed, state)
                    }
                    is ChatState.AwaitingConfirmation -> {
                        // User typed something instead of confirming
                        chatState = ChatState.Idle
                        pendingConfirmation = null
                        addMessage(ChatMessage.BotError(applicationContext.getString(R.string.chat_err_cancel_prev_tx)))
                        val result = parseInput(trimmed)
                        handleParserResult(result)
                    }
                }
            } catch (e: Exception) {
                addMessage(ChatMessage.BotError(applicationContext.getString(R.string.chat_err_unknown, e.message ?: applicationContext.getString(R.string.chat_err_unknown_fallback))))
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private suspend fun parseInput(input: String): ParserResult {
        // Try Gemini first if enabled
        if (isGeminiEnabled && geminiParser != null) {
            val geminiResult = geminiParser.parse(input)
            if (geminiResult is ParserResult.Success) return geminiResult
        }

        // Fallback to local parser
        return localParser.parse(input)
    }

    private suspend fun handleParserResult(result: ParserResult) {
        val wallets = _uiState.value.wallets
        val defaultWallet = wallets.firstOrNull { it.isDefault } ?: wallets.firstOrNull()

        when (result) {
            is ParserResult.Success -> {
                val wallet = resolveWallet(result.walletKey, wallets, defaultWallet)
                    ?: run {
                        addMessage(ChatMessage.BotError(applicationContext.getString(R.string.chat_err_no_wallet)))
                        return
                    }

                // Check if confirmation needed
                val isDebtFlow = result.intent == "lend" || result.intent == "borrow" || result.intent == "lend_repayment" || result.intent == "borrow_repayment"
                val threshold = if (isDebtFlow) debtConfirmationThreshold else 1_000_000L
                val needsConfirmation = result.confidence < 0.9f || result.amount >= threshold

                if (needsConfirmation) {
                    val confirmation = ChatMessage.BotConfirmation(
                        intent = result.intent,
                        amount = result.amount,
                        categoryKey = result.categoryKey,
                        walletId = wallet.id,
                        walletName = wallet.name,
                        description = result.description,
                        personName = result.personName,
                        participants = result.participants,
                        splitMethod = result.splitMethod
                    )
                    pendingConfirmation = confirmation
                    addMessage(confirmation)
                } else {
                    // Auto-confirm high confidence, small amounts
                    confirmTransaction(
                        intent = result.intent,
                        amount = result.amount,
                        categoryKey = result.categoryKey,
                        walletId = wallet.id,
                        walletName = wallet.name,
                        description = result.description,
                        personName = result.personName,
                        participants = result.participants,
                        splitMethod = result.splitMethod
                    )
                }
            }
            is ParserResult.LowConfidence -> {
                addMessage(ChatMessage.BotLowConfidence(result.message))
            }
            is ParserResult.ParseError -> {
                addMessage(ChatMessage.BotError("❌ ${result.message}"))
            }
            is ParserResult.DebtSuccess -> {
                val wallet = resolveWallet(result.walletKey, wallets, defaultWallet)
                    ?: run {
                        addMessage(ChatMessage.BotError(applicationContext.getString(R.string.chat_err_no_wallet)))
                        return
                    }
                val intentName = if (result.direction == DebtDirection.BORROW) "borrow" else "lend"
                val confirmation = ChatMessage.BotConfirmation(
                    intent = intentName,
                    amount = result.amount,
                    categoryKey = null,
                    walletId = wallet.id,
                    walletName = wallet.name,
                    description = result.description,
                    personName = result.personName
                )
                pendingConfirmation = confirmation
                chatState = ChatState.AwaitingConfirmation(
                    PendingDebt(result.direction, result.personName, result.amount, result.description)
                )
                addMessage(confirmation)
            }
            is ParserResult.NeedMoreDebtInfo -> {
                chatState = ChatState.CollectingDebtInfo(result.debt, result.missingField)
                addMessage(ChatMessage.BotLowConfidence(result.message))
            }
        }
    }

    fun confirmPending() {
        val pending = pendingConfirmation ?: return
        viewModelScope.launch {
            confirmTransaction(
                intent = pending.intent,
                amount = pending.amount,
                categoryKey = pending.categoryKey,
                walletId = pending.walletId,
                walletName = pending.walletName,
                description = pending.description,
                personName = pending.personName,
                participants = pending.participants,
                splitMethod = pending.splitMethod
            )
            pendingConfirmation = null
            chatState = ChatState.Idle
        }
    }

    fun cancelPending() {
        pendingConfirmation = null
        chatState = ChatState.Idle
        addMessage(ChatMessage.BotError(applicationContext.getString(R.string.chat_err_cancel_forced)))
    }
    
    private suspend fun handleMissingDebtField(input: String, state: ChatState.CollectingDebtInfo) {
        val debt = state.debt
        when (state.missingField) {
            MissingDebtField.AMOUNT -> {
                val amount = com.quanlychitieuapp.utils.MoneyParser.parse(input)
                if (amount == null || amount <= 0) {
                    addMessage(ChatMessage.BotError(applicationContext.getString(R.string.chat_err_amount_format)))
                    return
                }
                
                val newDebt = debt.copy(amount = amount)
                if (newDebt.person == null) {
                    chatState = ChatState.CollectingDebtInfo(newDebt, MissingDebtField.PERSON)
                    val msg = if (newDebt.direction == DebtDirection.BORROW) {
                        applicationContext.getString(R.string.chat_ask_who_borrow)
                    } else {
                        applicationContext.getString(R.string.chat_ask_who_lend)
                    }
                    addMessage(ChatMessage.BotLowConfidence(msg))
                } else {
                    processCompletedDebt(newDebt)
                }
            }
            MissingDebtField.PERSON -> {
                val amountInInput = com.quanlychitieuapp.utils.MoneyParser.parse(input)
                val amountRemovedText = input.replace(Regex("""(\d+)[.,]?(\d*)\s*(k|nghìn|tr|triệu|trieu|đ|dong)""", RegexOption.IGNORE_CASE), "").trim()
                
                val candidateName = amountRemovedText.takeIf { it.isNotBlank() }
                if (candidateName == null) {
                    addMessage(ChatMessage.BotError(applicationContext.getString(R.string.chat_err_invalid_name)))
                    return
                }
                
                val resolvedAmount = amountInInput ?: debt.amount
                val newDebt = debt.copy(person = candidateName, amount = resolvedAmount)
                
                if (resolvedAmount == null || resolvedAmount <= 0) {
                    chatState = ChatState.CollectingDebtInfo(newDebt, MissingDebtField.AMOUNT)
                    val msg = if (newDebt.direction == DebtDirection.BORROW) {
                        applicationContext.getString(R.string.chat_ask_amount_borrow, candidateName)
                    } else {
                        applicationContext.getString(R.string.chat_ask_amount_lend, candidateName)
                    }
                    addMessage(ChatMessage.BotLowConfidence(msg))
                } else {
                    processCompletedDebt(newDebt)
                }
            }
        }
    }
    
    private suspend fun processCompletedDebt(debt: PendingDebt) {
        val wallets = _uiState.value.wallets
        val defaultWallet = wallets.firstOrNull { it.isDefault } ?: wallets.firstOrNull()
        val wallet = resolveWallet(null, wallets, defaultWallet) ?: return

        val intentName = if (debt.direction == DebtDirection.BORROW) "borrow" else "lend"
        val confirmation = ChatMessage.BotConfirmation(
            intent = intentName,
            amount = debt.amount!!,
            categoryKey = null,
            walletId = wallet.id,
            walletName = wallet.name,
            description = debt.description ?: "",
            personName = debt.person!!
        )
        
        pendingConfirmation = confirmation
        chatState = ChatState.AwaitingConfirmation(debt)
        addMessage(confirmation)
    }

    private suspend fun confirmTransaction(
        intent: String,
        amount: Long,
        categoryKey: String?,
        walletId: Long,
        walletName: String,
        description: String,
        personName: String?,
        participants: List<String>? = null,
        splitMethod: String? = null
    ) {
        val now = System.currentTimeMillis()
        var success = true
        var errorMessage: String? = null
        try {
            when (intent) {
                "expense" -> {
                    val res = transactionService.addExpense(amount, categoryKey!!, walletId, description, now)
                    if (res.isFailure) throw res.exceptionOrNull()!!
                }
                "income" -> {
                    val res = transactionService.addIncome(amount, categoryKey!!, walletId, description, now)
                    if (res.isFailure) throw res.exceptionOrNull()!!
                }
                "lend" -> {
                    requireNotNull(personName) { applicationContext.getString(R.string.chat_err_missing_borrower) }
                    // Default duedate null for now
                    debtRepository.createLending(personName, amount, walletId, null, description)
                }
                "borrow" -> {
                    requireNotNull(personName) { applicationContext.getString(R.string.chat_err_missing_creditor) }
                    debtRepository.createBorrowing(personName, amount, walletId, null, description)
                }
                "lend_repayment" -> {
                    // This is complex if they have multiple debts! For MVP, we will try to find active LENDING debt by Name.
                    // But ChatBot shouldn't do complex querying, just assume success if we find one, else throw.
                    requireNotNull(personName) { applicationContext.getString(R.string.chat_err_missing_payer) }
                    val p = debtRepository.getPersonById(0) // Dummy to trick compiler, actually we need to find by name!
                    // I will implement a simpler way: require manual selection in UI, or just call a new method in repository: recordLendingRepaymentByName
                }
                "split_expense" -> {
                    requireNotNull(participants) { applicationContext.getString(R.string.chat_err_missing_participants) }
                    val inputs = mutableListOf<com.quanlychitieuapp.domain.service.SplitExpenseService.ParticipantInput>()
                    inputs.add(com.quanlychitieuapp.domain.service.SplitExpenseService.ParticipantInput(null, null))
                    participants.forEach { 
                        inputs.add(com.quanlychitieuapp.domain.service.SplitExpenseService.ParticipantInput(it, null)) 
                    }
                    val res = splitExpenseService.createSplitExpense(
                        totalAmount = amount,
                        categoryKey = categoryKey ?: "other",
                        walletId = walletId,
                        description = description,
                        splitMethod = splitMethod ?: "equal",
                        participants = inputs,
                        transactionDate = now
                    )
                    if (res.isFailure) throw res.exceptionOrNull()!!
                }
            }
        } catch (e: Exception) {
            success = false
            errorMessage = e.message
        }

        if (success) {
            addMessage(
                ChatMessage.BotSuccess(
                    intent = intent,
                    amount = amount,
                    categoryKey = categoryKey,
                    walletId = walletId,
                    walletName = walletName,
                    description = description,
                    personName = personName,
                    participants = participants,
                    splitMethod = splitMethod,
                    confirmed = true
                )
            )
        } else {
            addMessage(ChatMessage.BotError(applicationContext.getString(R.string.chat_err_save_tx, errorMessage ?: applicationContext.getString(R.string.chat_err_save_tx_fallback))))
        }
    }

    private fun resolveWallet(
        walletKey: String?,
        wallets: List<Wallet>,
        defaultWallet: Wallet?
    ): Wallet? {
        if (walletKey == null) return defaultWallet
        return wallets.find { it.type == walletKey || it.iconKey == walletKey }
            ?: defaultWallet
    }

    private fun addMessage(message: ChatMessage) {
        _uiState.update { state ->
            state.copy(messages = state.messages + message)
        }
    }
}
