package com.quanlychitieuapp.ai

import com.quanlychitieuapp.data.local.entity.CategoryKeys
import com.quanlychitieuapp.utils.MoneyParser

/**
 * LocalParser - rule-based natural language expense parser.
 * 
 * Works 100% offline. No API required.
 * Falls back to LowConfidence if not enough info.
 * 
 * Examples:
 *   "ăn sáng 35k"     -> expense, food, 35000
 *   "cafe 40k"         -> expense, food, 40000
 *   "grab 50k"         -> expense, transport, 50000
 *   "mua áo 300k"      -> expense, shopping, 300000
 *   "lương 8tr"        -> income, salary, 8000000
 *   "nhận thưởng 2tr"  -> income, bonus, 2000000
 */
class LocalParser {

    // Expense keyword mappings (keyword -> category key)
    private val expenseKeywords: Map<String, String> = mapOf(
        // Food
        "ăn" to CategoryKeys.FOOD,
        "bữa" to CategoryKeys.FOOD,
        "cơm" to CategoryKeys.FOOD,
        "phở" to CategoryKeys.FOOD,
        "bún" to CategoryKeys.FOOD,
        "cafe" to CategoryKeys.FOOD,
        "cà phê" to CategoryKeys.FOOD,
        "coffee" to CategoryKeys.FOOD,
        "trà" to CategoryKeys.FOOD,
        "sữa" to CategoryKeys.FOOD,
        "bánh" to CategoryKeys.FOOD,
        "đồ ăn" to CategoryKeys.FOOD,
        "ăn uống" to CategoryKeys.FOOD,
        "nhà hàng" to CategoryKeys.FOOD,
        "quán" to CategoryKeys.FOOD,
        "pizza" to CategoryKeys.FOOD,
        "burger" to CategoryKeys.FOOD,
        "phô mai" to CategoryKeys.FOOD,
        "lẩu" to CategoryKeys.FOOD,
        "tiệm" to CategoryKeys.FOOD,
        "ăn tối" to CategoryKeys.FOOD,
        "ăn trưa" to CategoryKeys.FOOD,
        "ăn sáng" to CategoryKeys.FOOD,
        "snack" to CategoryKeys.FOOD,
        "nước" to CategoryKeys.FOOD,

        // Transport
        "grab" to CategoryKeys.TRANSPORT,
        "be" to CategoryKeys.TRANSPORT,
        "gojek" to CategoryKeys.TRANSPORT,
        "taxi" to CategoryKeys.TRANSPORT,
        "xe" to CategoryKeys.TRANSPORT,
        "xăng" to CategoryKeys.TRANSPORT,
        "xăng xe" to CategoryKeys.TRANSPORT,
        "bus" to CategoryKeys.TRANSPORT,
        "xe buýt" to CategoryKeys.TRANSPORT,
        "tàu" to CategoryKeys.TRANSPORT,
        "metro" to CategoryKeys.TRANSPORT,
        "ship" to CategoryKeys.TRANSPORT,
        "giao hàng" to CategoryKeys.TRANSPORT,
        "vé xe" to CategoryKeys.TRANSPORT,
        "vé tàu" to CategoryKeys.TRANSPORT,
        "vé máy bay" to CategoryKeys.TRANSPORT,
        "máy bay" to CategoryKeys.TRANSPORT,
        "đậu xe" to CategoryKeys.TRANSPORT,
        "bãi đậu" to CategoryKeys.TRANSPORT,
        "parkfee" to CategoryKeys.TRANSPORT,
        "uber" to CategoryKeys.TRANSPORT,

        // Shopping
        "mua" to CategoryKeys.SHOPPING,
        "shopping" to CategoryKeys.SHOPPING,
        "quần áo" to CategoryKeys.SHOPPING,
        "áo" to CategoryKeys.SHOPPING,
        "quần" to CategoryKeys.SHOPPING,
        "giày" to CategoryKeys.SHOPPING,
        "dép" to CategoryKeys.SHOPPING,
        "túi" to CategoryKeys.SHOPPING,
        "online" to CategoryKeys.SHOPPING,
        "shopee" to CategoryKeys.SHOPPING,
        "lazada" to CategoryKeys.SHOPPING,
        "tiki" to CategoryKeys.SHOPPING,
        "điện thoại" to CategoryKeys.SHOPPING,
        "laptop" to CategoryKeys.SHOPPING,
        "máy tính" to CategoryKeys.SHOPPING,
        "đồ" to CategoryKeys.SHOPPING,

        // Entertainment
        "game" to CategoryKeys.ENTERTAINMENT,
        "cinema" to CategoryKeys.ENTERTAINMENT,
        "phim" to CategoryKeys.ENTERTAINMENT,
        "rạp" to CategoryKeys.ENTERTAINMENT,
        "karaoke" to CategoryKeys.ENTERTAINMENT,
        "du lịch" to CategoryKeys.ENTERTAINMENT,
        "giải trí" to CategoryKeys.ENTERTAINMENT,
        "concert" to CategoryKeys.ENTERTAINMENT,
        "nhạc" to CategoryKeys.ENTERTAINMENT,
        "chơi" to CategoryKeys.ENTERTAINMENT,
        "spotify" to CategoryKeys.ENTERTAINMENT,
        "netflix" to CategoryKeys.ENTERTAINMENT,

        // Health
        "thuốc" to CategoryKeys.HEALTH,
        "bác sĩ" to CategoryKeys.HEALTH,
        "khám" to CategoryKeys.HEALTH,
        "bệnh viện" to CategoryKeys.HEALTH,
        "y tế" to CategoryKeys.HEALTH,
        "gym" to CategoryKeys.HEALTH,
        "fitness" to CategoryKeys.HEALTH,
        "thể thao" to CategoryKeys.HEALTH,
        "vitamin" to CategoryKeys.HEALTH,

        // Education
        "học" to CategoryKeys.EDUCATION,
        "sách" to CategoryKeys.EDUCATION,
        "khóa học" to CategoryKeys.EDUCATION,
        "trường" to CategoryKeys.EDUCATION,
        "học phí" to CategoryKeys.EDUCATION,
        "gia sư" to CategoryKeys.EDUCATION,
        "tài liệu" to CategoryKeys.EDUCATION,

        // Living
        "điện" to CategoryKeys.LIVING,
        "nước" to CategoryKeys.LIVING,
        "internet" to CategoryKeys.LIVING,
        "wifi" to CategoryKeys.LIVING,
        "nhà" to CategoryKeys.LIVING,
        "thuê nhà" to CategoryKeys.LIVING,
        "phòng" to CategoryKeys.LIVING,
        "quản lý" to CategoryKeys.LIVING,

        // Telecom
        "điện thoại" to CategoryKeys.TELECOM,
        "nạp tiền" to CategoryKeys.TELECOM,
        "sim" to CategoryKeys.TELECOM,
        "data" to CategoryKeys.TELECOM,
        "4g" to CategoryKeys.TELECOM,
        "5g" to CategoryKeys.TELECOM
    )

    // Income keyword mappings
    private val incomeKeywords: Map<String, String> = mapOf(
        "lương" to CategoryKeys.SALARY,
        "salary" to CategoryKeys.SALARY,
        "thu nhập" to CategoryKeys.SALARY,

        "thưởng" to CategoryKeys.BONUS,
        "nhận thưởng" to CategoryKeys.BONUS,
        "thưởng cuối năm" to CategoryKeys.BONUS,
        "bonus" to CategoryKeys.BONUS,

        "freelance" to CategoryKeys.FREELANCE,
        "dự án" to CategoryKeys.FREELANCE,
        "hợp đồng" to CategoryKeys.FREELANCE,
        "làm thêm" to CategoryKeys.FREELANCE,

        "nhận" to CategoryKeys.OTHER_INCOME,
        "tiền" to CategoryKeys.OTHER_INCOME,
        "chuyển" to CategoryKeys.OTHER_INCOME,
        "hoàn" to CategoryKeys.OTHER_INCOME,
        "hoàn tiền" to CategoryKeys.OTHER_INCOME
    )

    // Strong income signals that override ambiguous overlap
    private val strongIncomeSignals = setOf("lương", "thưởng", "nhận thưởng", "salary", "bonus", "freelance")

    /**
     * Parse natural language input.
     * Returns ParserResult.Success with confidence, or LowConfidence if unsure.
     */
    fun parse(input: String): ParserResult {
        val lower = input.lowercase().trim()

        val amount = com.quanlychitieuapp.utils.MoneyParser.parse(lower)

        val walletKey = extractWalletHint(lower)
        val (description, amountRemovedText) = buildDescription(lower, amount ?: -1L)

        // Rule-based generic parsing for debt
        val debtMatch = parseDebtGrammar(amountRemovedText)
        if (debtMatch != null) {
            val direction = debtMatch.first
            val person = debtMatch.second
            
            val pendingDebt = PendingDebt(direction, person, amount, description)
            if (amount == null || amount <= 0L) {
                return ParserResult.NeedMoreDebtInfo(pendingDebt, MissingDebtField.AMOUNT, "Bạn ${if(direction == DebtDirection.BORROW) "vay" else "cho"} ${person ?: "ai"} bao nhiêu?")
            }
            if (person == null) {
                return ParserResult.NeedMoreDebtInfo(pendingDebt, MissingDebtField.PERSON, "Bạn muốn ${if(direction == DebtDirection.BORROW) "vay" else "cho mượn"} ai?")
            }
            
            return ParserResult.DebtSuccess(
                direction = direction,
                amount = amount,
                personName = person,
                walletKey = walletKey,
                description = description,
                confidence = 0.9f
            )
        }

        // If not a debt and amount is missing, we fail
        if (amount == null || amount <= 0L) return ParserResult.LowConfidence()

        val splitMatch = parseSplitIntent(amountRemovedText)
        if (splitMatch != null) {
            val (participants, splitConfidence) = splitMatch
            if (splitConfidence > 0.6f) {
                val expenseMatch = findBestMatch(lower, expenseKeywords)
                val categoryKey = expenseMatch?.second ?: CategoryKeys.OTHER_EXPENSE
                return ParserResult.Success(
                    intent = "split_expense",
                    amount = amount,
                    categoryKey = categoryKey,
                    walletKey = walletKey,
                    description = description.replace(Regex("""chia(?: cho)?\s+.*"""), "").trim(),
                    date = "today",
                    confidence = splitConfidence,
                    participants = participants,
                    splitMethod = "equal"
                )
            }
        }

        // Removed old debt parsing as we now use parseDebtGrammar

        // Check income signals first (strong signals)
        val incomeMatch = findBestMatch(lower, incomeKeywords)
        val expenseMatch = findBestMatch(lower, expenseKeywords)

        // Determine intent
        val (intent, categoryKey, confidence) = when {
            incomeMatch != null && (expenseMatch == null || isStrongIncomeSignal(lower)) -> {
                Triple("income", incomeMatch.second, 0.85f)
            }
            expenseMatch != null -> {
                Triple("expense", expenseMatch.second, 0.85f)
            }
            incomeMatch != null -> {
                Triple("income", incomeMatch.second, 0.7f)
            }
            else -> {
                // No keywords matched - low confidence
                return ParserResult.LowConfidence()
            }
        }

        // Confidence threshold check
        if (confidence < 0.6f) return ParserResult.LowConfidence()

        return ParserResult.Success(
            intent = intent,
            amount = amount,
            categoryKey = categoryKey,
            walletKey = walletKey,
            description = description,
            date = "today",
            confidence = confidence
        )
    }

    /**
     * Find best matching keyword in text.
     * Returns the keyword and its category key, preferring longer matches.
     */
    private fun findBestMatch(text: String, keywords: Map<String, String>): Pair<String, String>? {
        return keywords.entries
            .filter { text.contains(it.key) }
            .maxByOrNull { it.key.length }
            ?.let { Pair(it.key, it.value) }
    }

    private fun isStrongIncomeSignal(text: String): Boolean =
        strongIncomeSignals.any { text.contains(it) }

    private fun extractWalletHint(text: String): String? = when {
        text.contains("momo") || text.contains("mô mô") -> "momo"
        text.contains("ngân hàng") || text.contains("atm") || text.contains("banking") -> "bank"
        text.contains("tiền mặt") || text.contains("cash") -> "cash"
        else -> null
    }

    private fun buildDescription(text: String, amount: Long): Pair<String, String> {
        val cleaned = text
            .replace(Regex("""(\d+)[.,]?(\d*)\s*(k|nghìn|tr|triệu|trieu|đ|dong)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\d{4,}"""), "")
            .trim()
        val desc = if (cleaned.length > 3) cleaned.take(100) else ""
        return Pair(desc, cleaned)
    }

    private val pronouns = listOf("t", "tôi", "tao", "mình", "em", "con", "minh")

    private fun parseSplitIntent(amountRemovedText: String): Pair<List<String>, Float>? {
        val chiaMatch = Regex("""chia(?: cho)?\s+(.*)""").find(amountRemovedText)
        if (chiaMatch != null) {
            val namesStr = chiaMatch.groupValues[1]
            val names = namesStr.split(Regex("[,;]|\\b(?:và|va)\\b"))
                .map { it.trim() }
                .filter { it.isNotEmpty() && !pronouns.contains(it) }
            if (names.isNotEmpty()) {
                return Pair(names, 0.9f)
            }
        }
        return null
    }

    private fun parseDebtGrammar(text: String): Pair<DebtDirection, String?>? {
        val pronounsSelf = listOf("t", "tôi", "tao", "mình", "em", "con")
        val selfRegex = "(?:${pronounsSelf.joinToString("|")})"
        val borrowRegex = "(?:vay|mượn|no|nợ)"

        val p56 = Regex("^(?:$selfRegex\\s+)?cho\\s+(.+?)\\s+$borrowRegex$").find(text)
        if (p56 != null) {
            val person = p56.groupValues[1].trim()
            return Pair(DebtDirection.LEND, person.takeIf { it.isNotBlank() })
        }

        val p4 = Regex("^(.+?)\\s+cho(?:\\s+$selfRegex)?\\s+$borrowRegex$").find(text)
        if (p4 != null) {
            val person = p4.groupValues[1].trim()
            return Pair(DebtDirection.BORROW, person.takeIf { it.isNotBlank() })
        }

        val p3 = Regex("^(.+?)\\s+$borrowRegex\\s+$selfRegex$").find(text)
        if (p3 != null) {
            val person = p3.groupValues[1].trim()
            return Pair(DebtDirection.LEND, person.takeIf { it.isNotBlank() })
        }

        val p12 = Regex("^(?:$selfRegex\\s+)?$borrowRegex\\s+(.+)$").find(text)
        if (p12 != null) {
            val person = p12.groupValues[1].trim()
            if (person.isNotBlank() && !pronounsSelf.contains(person.lowercase())) {
                return Pair(DebtDirection.BORROW, person)
            }
        }

        // Trường hợp: "<person> vay", "<person> mượn", "<person> nợ" -> LEND (họ nợ mình)
        val p7 = Regex("^(.+?)\\s+$borrowRegex$").find(text)
        if (p7 != null) {
            val person = p7.groupValues[1].trim()
            if (person.isNotBlank() && !pronounsSelf.contains(person.lowercase())) {
                return Pair(DebtDirection.LEND, person)
            }
        }

        if (Regex("^(?:$selfRegex\\s+)?$borrowRegex$").matches(text)) {
            return Pair(DebtDirection.BORROW, null)
        }
        
        if (Regex("^(?:$selfRegex\\s+)?cho\\s+$borrowRegex$").matches(text)) {
            return Pair(DebtDirection.LEND, null)
        }

        return null
    }
}
