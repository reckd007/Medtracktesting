package com.example.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.data.db.MedTrackDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Background Service that inspects medication quantities in the Room database
 * against user-defined thresholds (medication.refillThreshold).
 *
 * When an inventory quantity is at or below the user-specified threshold,
 * it triggers a high-priority push notification informing the user that a
 * refill is required, complete with 1-tap "Log Refill (+30)" and "Open Cabinet" actions.
 */
class RefillCheckBackgroundService : Service() {

    companion object {
        const val TAG = "RefillCheckService"
        const val ACTION_CHECK_REFILLS = "com.example.notification.ACTION_CHECK_REFILLS"

        fun start(context: Context) {
            val intent = Intent(context, RefillCheckBackgroundService::class.java).apply {
                action = ACTION_CHECK_REFILLS
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot startService directly, falling back to RefillCheckReceiver: ${e.message}")
                val broadcastIntent = Intent(context, RefillCheckReceiver::class.java).apply {
                    action = ACTION_CHECK_REFILLS
                }
                context.sendBroadcast(broadcastIntent)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "RefillCheckBackgroundService started (startId=$startId). Evaluating Room database medication stock...")

        serviceScope.launch {
            try {
                val db = MedTrackDatabase.getDatabase(applicationContext)
                val allMedications = db.medicationDao().getAllMedicationsSync()
                Log.d(TAG, "Found ${allMedications.size} medications in Room database.")

                var lowStockAlertsDispatched = 0

                for (med in allMedications) {
                    val remaining = med.stockRemaining
                    val threshold = med.refillThreshold

                    // Check quantity against user-defined threshold
                    if (remaining <= threshold) {
                        lowStockAlertsDispatched++
                        Log.w(
                            TAG,
                            "Refill required for '${med.name}': remaining=$remaining, threshold=$threshold, form='${med.form}'"
                        )

                        NotificationHelper.showRefillNotification(
                            context = applicationContext,
                            medicationId = med.id,
                            medicationName = med.name,
                            stockRemaining = remaining,
                            refillThreshold = threshold,
                            form = med.form,
                            dosage = med.dosage
                        )
                    } else {
                        Log.d(
                            TAG,
                            "Medication '${med.name}' is adequately stocked: $remaining remaining (threshold: $threshold)."
                        )
                    }
                }

                Log.i(
                    TAG,
                    "Refill evaluation complete. Dispatched $lowStockAlertsDispatched refill push notifications."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error checking medication quantities in Room database: ${e.message}", e)
            } finally {
                // Automatically stop service when background task finishes
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "RefillCheckBackgroundService destroyed.")
    }
}
