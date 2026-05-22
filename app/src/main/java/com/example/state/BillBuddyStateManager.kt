package com.example.state

import android.content.Context
import android.util.Log
import com.example.database.BillBuddyDatabase
import com.example.database.BillEntity
import com.example.service.GeminiParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object BillBuddyStateManager {
    private const val TAG = "BillBuddyStateManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Current state of the active Stitch pop-up modal
    private val _activePopupState = MutableStateFlow<PopupState>(PopupState.Idle)
    val activePopupState: StateFlow<PopupState> = _activePopupState.asStateFlow()

    // Thread-safe initializer for Room Database reference
    private var database: BillBuddyDatabase? = null

    fun initialize(context: Context) {
        if (database == null) {
            database = BillBuddyDatabase.getDatabase(context.applicationContext)
        }
    }

    /**
     * Triggered by receiving a financial pattern SMS (via BroadcastReceiver) or simulated SMS list entry.
     */
    fun handleIncomingSMS(context: Context, sender: String, body: String) {
        Log.d(TAG, "Parsing incoming SMS: sender='$sender', body='$body'")
        initialize(context)

        // Change popup dialog progress to parsing
        _activePopupState.value = PopupState.Parsing(sender = sender, body = body)

        scope.launch {
            try {
                val parsed = GeminiParser.parseSMS(body)
                if (parsed != null) {
                    Log.d(TAG, "Successfully parsed transaction: amount=${parsed.amount}, merchant='${parsed.merchant}'")
                    
                    _activePopupState.value = PopupState.ParsedSuccess(
                        sender = sender,
                        body = body,
                        amount = parsed.amount,
                        merchant = parsed.merchant
                    )

                    // Persist to Room Database on IO dispatcher safely inside the same coroutine
                    withContext(Dispatchers.IO) {
                        try {
                            val entity = BillEntity(
                                smsSender = sender,
                                smsBody = body,
                                amount = parsed.amount,
                                merchant = parsed.merchant,
                                timestamp = System.currentTimeMillis()
                            )
                            database?.billDao()?.insertBill(entity)
                            Log.d(TAG, "Persisted parsed bill to SQLite history list")
                        } catch (dbEx: Exception) {
                            Log.e(TAG, "Failed block DB write operation", dbEx)
                        }
                    }
                } else {
                    Log.w(TAG, "Failed parsing SMS: Gemini returned null. Sender=$sender, Body=$body")
                    _activePopupState.value = PopupState.ParsedError(
                        sender = sender,
                        body = body,
                        error = "Gemini was unable to extract financial attributes from the message body."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during handling of financial SMS target stream", e)
                _activePopupState.value = PopupState.ParsedError(
                    sender = sender,
                    body = body,
                    error = e.localizedMessage ?: "Unexpected parsing exception occurred."
                )
            }
        }
    }

    fun dismissPopup() {
        _activePopupState.value = PopupState.Idle
    }

    /**
     * Direct split trigger for custom manually generated bills
     */
    fun triggerManualSplit(merchant: String, amount: Double, sender: String = "MANUAL_ENTRY") {
        _activePopupState.value = PopupState.ParsedSuccess(
            sender = sender,
            body = "Manually recorded transaction for ₹${"%.2f".format(amount)} split split split at $merchant.",
            amount = amount,
            merchant = merchant
        )
    }

    /**
     * Persist a custom, manually created bill directly
     */
    fun insertManualBill(context: Context, merchant: String, amount: Double, sender: String = "MANUAL_ENTRY") {
        initialize(context)
        scope.launch(Dispatchers.IO) {
            try {
                val entity = BillEntity(
                    smsSender = sender,
                    smsBody = "Manually recorded transaction for ₹${"%.2f".format(amount)} split split split at $merchant.",
                    amount = amount,
                    merchant = merchant,
                    timestamp = System.currentTimeMillis()
                )
                database?.billDao()?.insertBill(entity)
                Log.d(TAG, "Manually persisted bill to SQLite")
            } catch (dbEx: Exception) {
                Log.e(TAG, "Failed block DB write operation for manual bill", dbEx)
            }
        }
    }

    /**
     * Delete a historical transaction from the DB
     */
    fun deleteBill(context: Context, bill: BillEntity) {
        initialize(context)
        scope.launch(Dispatchers.IO) {
            try {
                database?.billDao()?.deleteBill(bill)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting bill", e)
            }
        }
    }

    /**
     * Clear all records
     */
    fun clearHistory(context: Context) {
        initialize(context)
        scope.launch(Dispatchers.IO) {
            try {
                database?.billDao()?.clearAllBills()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing history", e)
            }
        }
    }
}

sealed interface PopupState {
    object Idle : PopupState
    data class Parsing(val sender: String, val body: String) : PopupState
    data class ParsedSuccess(
        val sender: String,
        val body: String,
        val amount: Double,
        val merchant: String
    ) : PopupState
    data class ParsedError(
        val sender: String,
        val body: String,
        val error: String
    ) : PopupState
}
