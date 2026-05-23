package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.state.BillBuddyStateManager

class SMSReceiver : BroadcastReceiver() {
    private val TAG = "SMSReceiver"

    // Set of common domestic financial keywords in Indian landscape (debit, spent, ₹, INR, rs etc.)
    private val financialPatterns = listOf(
        "debited", "spent", "inr", "₹", "rs.", "paid", "transfer", "sent to", "credited", "charged"
    )

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                Log.d(TAG, "SMS broadcast filter match triggered!")
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages != null) {
                    for (message in messages) {
                        val sender = message.displayOriginatingAddress ?: "Unknown Sender"
                        val body = message.displayMessageBody ?: continue
                        
                        Log.d(TAG, "SMS Content: Sender=$sender, Body='$body'")

                        // Check if sms body matches any financial pattern (case insensitive)
                        val bodyLower = body.lowercase()
                        val matchesPattern = financialPatterns.any { pattern -> bodyLower.contains(pattern) }

                        if (matchesPattern) {
                            Log.i(TAG, "Financial patterns matched in message body! Initiating Gemini parsing...")
                            BillBuddyStateManager.handleIncomingSMS(context, sender, body)
                        } else {
                            Log.v(TAG, "Skip SMS: No financial patterns matched.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming SMS broadcast", e)
        }
    }
}
