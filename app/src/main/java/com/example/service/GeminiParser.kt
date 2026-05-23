package com.example.service

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ParsedBill(
    val amount: Double,
    val merchant: String
)

object GeminiParser {
    private const val TAG = "GeminiParser"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun parseSMS(smsBody: String): ParsedBill? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or uses default placeholder! Key: $apiKey")
            return@withContext parseSMSLocally(smsBody)
        }

        // Prompt designed for strict financial extraction
        val prompt = """
            You are a strict financial transaction parser. Extract the absolute total transaction amount and the merchant/recipient name from this financial SMS text.
            If the amount has currency symbols (like ₹, INR, $, USD, Rs.), extract only the raw numerical value as a double (e.g. 150.0).
            If no merchant/recipient name is clearly mentioned, detect the bank name, upi payee, vpa, or merchant name. Return ONLY a valid JSON object matching the requested schema.
            
            SMS Text: "$smsBody"
        """.trimIndent()

        // Create generation request matching Gemini's responseMimeType JSON schema definition
        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", contentsArray)

            val generationConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                
                // Define responseSchema with exact amount and merchant fields
                val responseSchema = JSONObject().apply {
                    put("type", "OBJECT")
                    val properties = JSONObject().apply {
                        put("amount", JSONObject().apply {
                            put("type", "NUMBER")
                            put("description", "The numerical value of the money debited, spent, or paid.")
                        })
                        put("merchant", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "The merchant name, upi payee name, store name, or bank identity.")
                        })
                    }
                    put("properties", properties)
                    put("required", JSONArray().apply {
                        put("amount")
                        put("merchant")
                    })
                }
                put("responseSchema", responseSchema)
                put("temperature", 0.1)
            }
            put("generationConfig", generationConfig)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API request failed with status key: ${response.code} ${response.message}")
                    Log.w(TAG, "Attempting local fallback parsing after non-successful API code...")
                    return@withContext parseSMSLocally(smsBody)
                }

                val responseBodyStr = response.body?.string() ?: return@withContext parseSMSLocally(smsBody)
                Log.d(TAG, "Gemini parsing answer payload: $responseBodyStr")

                val rootObj = JSONObject(responseBodyStr)
                val candidates = rootObj.optJSONArray("candidates") ?: return@withContext parseSMSLocally(smsBody)
                if (candidates.length() > 0) {
                    val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        Log.d(TAG, "Extracted parsed content inner text: $text")
                        
                        val resultObj = JSONObject(text.trim())
                        val amount = resultObj.optDouble("amount", 0.0)
                        val merchant = resultObj.optString("merchant", "Unknown Merchant")
                        
                        return@withContext ParsedBill(amount = amount, merchant = merchant)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini API parsing flow, performing offline local regex parsing...", e)
            return@withContext parseSMSLocally(smsBody)
        }
        return@withContext parseSMSLocally(smsBody)
    }

    /**
     * Highly robust offline fallback transaction parser.
     * Uses state-of-the-art Android local pattern parsing to guarantee up-time.
     */
    fun parseSMSLocally(smsBody: String): ParsedBill {
        Log.i(TAG, "Executing local offline fallback transaction parser...")
        
        // 1. Extract Amount
        var amount = 0.0
        val amountPatterns = listOf(
            Regex("""(?i)(?:rs\.?|inr|₹|INR|Rs)\s*([\d,]+(?:\.\d{1,2})?)"""), // e.g. Rs. 500, Rs 500.50, ₹450
            Regex("""([\d,]+(?:\.\d{1,2})?)\s*(?:rs\.?|inr|₹)"""),        // e.g. 500 Rs, 450 INR
            Regex("""(?i)(?:debited|credited|spent|paid|transfer)\s+(?:of\s+)?(?:rs\.?|inr|₹)?\s*([\d,]+(?:\.\d{1,2})?)""")
        )
        
        for (pattern in amountPatterns) {
            val match = pattern.find(smsBody)
            if (match != null) {
                try {
                    val amtStr = match.groupValues[1].replace(",", "")
                    val parsedAmt = amtStr.toDoubleOrNull()
                    if (parsedAmt != null && parsedAmt > 0.0) {
                        amount = parsedAmt
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error matching local amount chunk", e)
                }
            }
        }
        
        // Fallback to searching any isolated floating/double digits
        if (amount == 0.0) {
            val doublePattern = Regex("""\b\d+[\.,]\d{1,2}\b""")
            val match = doublePattern.find(smsBody)
            if (match != null) {
                amount = match.value.replace(",", "").toDoubleOrNull() ?: 0.0
            }
        }

        // 2. Extract Merchant / Payee info
        var merchant = "Unknown Merchant"
        val merchantPatterns = listOf(
            Regex("""(?i)(?:sent\s+to|paid\s+to|transfer\s+to|credited\s+by|transfer\s+from|ref\s+to|at|with)\s+([a-zA-Z0-9\s.\-_*]+)"""),
            Regex("""(?i)\b(?:by|to|from)\s+([a-zA-Z0-9\s.\-_*]{3,25})\b""")
        )

        for (pattern in merchantPatterns) {
            val match = pattern.find(smsBody)
            if (match != null) {
                var candidate = match.groupValues[1].trim()
                
                // Truncate at common separators/conjunctions
                val stopWords = listOf(
                    " on ", " via ", " ref ", " bal ", " using ", " a/c ", " account ", 
                    " active ", " for ", " towards ", " successful", " successfully", 
                    " completed", " done", " processed"
                )
                for (stopWord in stopWords) {
                    val idx = candidate.indexOf(stopWord, ignoreCase = true)
                    if (idx != -1) {
                        candidate = candidate.substring(0, idx).trim()
                    }
                }
                
                // Strip trailing punctuation
                while (candidate.endsWith(".") || candidate.endsWith(",") || candidate.endsWith("!") || candidate.endsWith("*")) {
                    candidate = candidate.substring(0, candidate.length - 1).trim()
                }

                if (candidate.isNotEmpty() && !candidate.contains("rs", ignoreCase = true) && !candidate.contains("inr", ignoreCase = true)) {
                    merchant = candidate
                    break
                }
            }
        }

        // Clean up merchant name (if too long or empty)
        if (merchant == "Unknown Merchant" || merchant.length > 40) {
            val spaceIndex = smsBody.indexOf(" ")
            merchant = if (spaceIndex > 0) smsBody.substring(0, spaceIndex).trim() else "Transaction"
        }
        
        // Sanitize return value
        val finalMerchant = merchant.replace(Regex("[^a-zA-Z0-9\\s.\\-*]"), "").trim()
        val displayMerchant = if (finalMerchant.length > 2) finalMerchant else "UPI Merchant"

        return ParsedBill(amount = amount, merchant = displayMerchant)
    }
}
