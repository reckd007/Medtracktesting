package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.MedTrackDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by periodic AlarmManager ticks, system broadcasts,
 * or UI action buttons to run the medication quantity refill check.
 */
class RefillCheckReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "RefillCheckReceiver"
        const val ACTION_CHECK_REFILLS = "com.example.notification.ACTION_CHECK_REFILLS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Received broadcast intent: $action. Initiating Room DB refill evaluation...")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MedTrackDatabase.getDatabase(context)
                val medications = db.medicationDao().getAllMedicationsSync()
                var count = 0

                for (med in medications) {
                    if (med.stockRemaining <= med.refillThreshold) {
                        count++
                        NotificationHelper.showRefillNotification(
                            context = context,
                            medicationId = med.id,
                            medicationName = med.name,
                            stockRemaining = med.stockRemaining,
                            refillThreshold = med.refillThreshold,
                            form = med.form,
                            dosage = med.dosage
                        )
                    }
                }
                Log.i(TAG, "Completed refill check in background receiver: evaluated ${medications.size} meds, sent $count alerts.")
            } catch (e: Exception) {
                Log.e(TAG, "Error executing refill check from receiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
