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
            return@withContext null
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
                    Log.e(TAG, "Response body raw detail: ${response.body?.string()}")
                    return@withContext null
                }

                val responseBodyStr = response.body?.string() ?: return@withContext null
                Log.d(TAG, "Gemini parsing answer payload: $responseBodyStr")

                val rootObj = JSONObject(responseBodyStr)
                val candidates = rootObj.optJSONArray("candidates") ?: return@withContext null
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
            Log.e(TAG, "Failed calling Gemini API parsing flow", e)
        }
        return@withContext null
    }
}
