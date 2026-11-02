package com.quanlychitieuapp.ai

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * GeminiParser - AI-powered NLP using Gemini API.
 * 
 * CRITICAL RULES:
 * - Gemini is OPTIONAL. App works without it.
 * - API key NEVER hardcoded or logged.
 * - Validate ALL Gemini output before any DB write.
 * - Fallback to LocalParser on any error.
 * - Model: gemini-2.0-flash
 */
class GeminiParser(
    private val context: Context,
    private val apiKeyProvider: () -> String?   // Injected, never hardcoded
) {

    companion object {
        private const val TAG = "GeminiParser"
        private const val MODEL = "gemini-2.0-flash"
        private const val API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val TIMEOUT_MS = 15_000L

        private val VALID_INTENTS = setOf("expense", "income", "split_expense", "debt")
        private val VALID_CATEGORIES = setOf(
            "food", "shopping", "transport", "entertainment",
            "education", "living", "health", "telecom", "other",
            "salary", "bonus", "freelance", "other_income"
        )

        /**
         * Build the prompt for Gemini
         */
        fun buildPrompt(userInput: String): String = """
            Phân tích đoạn văn sau và trả về JSON:
            
            Input: "$userInput"
            
            Trả về CHÍNH XÁC JSON này (không thêm markdown, không thêm giải thích):
            {
              "intent": "expense" hoặc "income" hoặc "split_expense" hoặc "debt",
              "direction": "borrow" hoặc "lend" (nếu intent = debt, hoặc null nếu không phải),
              "person": tên người nếu intent=debt (có thể null nếu câu hỏi không chỉ định),
              "amount": số tiền VND dạng số nguyên, không float (có thể null nếu câu chưa có),
              "category": một trong: food/shopping/transport/entertainment/education/living/health/telecom/other/salary/bonus/freelance/other_income,
              "wallet": "cash" hoặc "bank" hoặc "momo" hoặc null,
              "description": mô tả ngắn,
              "date": "today" hoặc "yesterday",
              "confidence": số từ 0.0 đến 1.0,
              "participants": mảng chứa danh sách tên người tham gia nếu intent=split_expense
            }
            
            Quy tắc:
            - amount phải là số nguyên dương. Nếu không có số tiền, amount để null.
            - Nếu không chắc, để confidence < 0.6
            - Không tự đoán nếu không rõ intent
        """.trimIndent()
    }

    /**
     * Parse input using Gemini API.
     * Returns null if Gemini unavailable or fails (caller should fallback).
     */
    suspend fun parse(userInput: String): ParserResult? {
        val apiKey = apiKeyProvider() ?: run {
            Log.d(TAG, "Gemini API key not configured")
            return null
        }

        return withTimeoutOrNull(TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                try {
                    val response = callGeminiApi(userInput, apiKey)
                    response?.let { parseGeminiResponse(it) }
                } catch (e: Exception) {
                    Log.w(TAG, "Gemini parse failed: ${e::class.simpleName}")
                    null
                }
            }
        }
    }

    private fun callGeminiApi(userInput: String, apiKey: String): String? {
        val url = URL("$API_BASE/$MODEL:generateContent?key=$apiKey")
        val requestBody = buildRequestBody(userInput)

        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Gemini HTTP error: $responseCode")
                return null
            }

            return BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { reader ->
                reader.readText()
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun buildRequestBody(userInput: String): String {
        val prompt = buildPrompt(userInput)
        val escapedPrompt = Gson().toJson(prompt)
        return """
            {
              "contents": [{
                "parts": [{"text": $escapedPrompt}]
              }],
              "generationConfig": {
                "temperature": 0.1,
                "maxOutputTokens": 512
              }
            }
        """.trimIndent()
    }

    /**
     * Parse and VALIDATE Gemini response JSON.
     * Kotlin validates EVERYTHING before trusting any output.
     */
    internal fun parseGeminiResponse(rawResponse: String): ParserResult? {
        return try {
            val gson = Gson()
            val responseObj = gson.fromJson(rawResponse, JsonObject::class.java)

            // Extract text from Gemini response structure
            val textContent = responseObj
                ?.getAsJsonArray("candidates")
                ?.firstOrNull()
                ?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.firstOrNull()
                ?.asJsonObject
                ?.get("text")
                ?.asString
                ?: return null

            parseJsonOutput(textContent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Gemini response: ${e.message}")
            null
        }
    }

    /**
     * Validate and parse the JSON output from Gemini (or mock).
     * Used in both production and tests.
     */
    fun parseJsonOutput(jsonText: String): ParserResult? {
        return try {
            val gson = Gson()
            // Clean potential markdown code blocks
            val cleaned = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val obj = gson.fromJson(cleaned, JsonObject::class.java)

            // Validate required fields
            val intent = obj.get("intent")?.asString?.lowercase()?.trim()
            val amount = obj.get("amount")?.takeIf { !it.isJsonNull }?.asLong
            val category = obj.get("category")?.takeIf { !it.isJsonNull }?.asString?.lowercase()?.trim()
            val confidence = obj.get("confidence")?.asFloat ?: 0.5f

            if (intent == null || intent !in VALID_INTENTS) {
                Log.d(TAG, "Invalid intent: $intent")
                return null
            }

            if (confidence < 0.6f) {
                return ParserResult.LowConfidence()
            }

            val wallet = obj.get("wallet")?.takeIf { !it.isJsonNull }?.asString
            val description = obj.get("description")?.takeIf { !it.isJsonNull }?.asString ?: ""
            val date = obj.get("date")?.takeIf { !it.isJsonNull }?.asString ?: "today"
            
            if (intent == "debt") {
                val directionStr = obj.get("direction")?.takeIf { !it.isJsonNull }?.asString?.lowercase()
                val person = obj.get("person")?.takeIf { !it.isJsonNull }?.asString
                
                val direction = if (directionStr == "lend") DebtDirection.LEND else DebtDirection.BORROW
                
                val pendingDebt = PendingDebt(direction, person, amount, description)
                
                if (amount == null || amount <= 0L) {
                    return ParserResult.NeedMoreDebtInfo(pendingDebt, MissingDebtField.AMOUNT, "Bạn ${if(direction == DebtDirection.BORROW) "vay" else "cho"} ${person ?: "người này"} bao nhiêu?")
                }
                if (person == null) {
                    return ParserResult.NeedMoreDebtInfo(pendingDebt, MissingDebtField.PERSON, "Bạn muốn ${if(direction == DebtDirection.BORROW) "vay" else "cho mượn"} ai?")
                }
                
                return ParserResult.DebtSuccess(
                    direction = direction,
                    amount = amount,
                    personName = person,
                    walletKey = wallet,
                    description = description,
                    confidence = confidence
                )
            } else {
                if (amount == null || amount <= 0L) {
                    return ParserResult.LowConfidence()
                }
                
                if (category == null || category !in VALID_CATEGORIES) {
                    Log.d(TAG, "Invalid category: $category")
                    return null
                }
                
                val participants = if (obj.has("participants") && !obj.get("participants").isJsonNull) {
                    obj.getAsJsonArray("participants").map { it.asString }
                } else null

                return ParserResult.Success(
                    intent = intent,
                    amount = amount,
                    categoryKey = category,
                    walletKey = wallet,
                    description = description,
                    date = date,
                    confidence = confidence,
                    participants = participants,
                    splitMethod = if (participants != null) "equal" else null
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "JSON parsing error: ${e.message}")
            null
        }
    }
}
