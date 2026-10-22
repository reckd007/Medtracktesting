package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.MedTrackDatabase
import com.example.data.model.DoseLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationActionReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MedActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1L)
        val medicationName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_NAME) ?: "Medication"
        val scheduledTime = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_TIME) ?: "08:00"
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE) ?: ""
        val form = intent.getStringExtra(NotificationHelper.EXTRA_FORM) ?: "Pill"
        val instructions = intent.getStringExtra(NotificationHelper.EXTRA_INSTRUCTIONS) ?: ""

        Log.d(TAG, "Action received: $action for $medicationName ($scheduledTime, notifId=$notificationId)")

        // Stop continuous ringing audio & vibration on any user alarm action
        if (action == NotificationHelper.ACTION_TAKE_DOSE ||
            action == NotificationHelper.ACTION_STOP_ALARM ||
            action == NotificationHelper.ACTION_SNOOZE_DOSE ||
            action == NotificationHelper.ACTION_DISMISS_ALARM
        ) {
            AlarmPlaybackController.stopPlaybackOnly(context)
            AlarmRingtoneService.stopAlarm(context)
        }

        // Dismiss the notification upon user interaction
        if (notificationId != -1) {
            NotificationHelper.cancelNotification(context, notificationId)
        }

        when (action) {
            NotificationHelper.ACTION_TAKE_DOSE -> {
                Log.d(TAG, "User clicked 'Mark as Taken' for $medicationName ($scheduledTime)")
                AlarmPlaybackController.stopActiveAlarm(context, reason = "Marked as Taken")

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = MedTrackDatabase.getDatabase(context)
                        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                        // 1. Decrement stock
                        if (medicationId > 0) {
                            db.medicationDao().decrementStock(medicationId)

                            // Check if medication is now at or below refill threshold
                            val updatedMed = db.medicationDao().getMedicationById(medicationId)
                            if (updatedMed != null && updatedMed.stockRemaining <= updatedMed.refillThreshold) {
                                Log.w(TAG, "Medication ${updatedMed.name} stock (${updatedMed.stockRemaining}) reached refill threshold. Triggering refill alert.")
                                NotificationHelper.showRefillNotification(
                                    context = context,
                                    medicationId = updatedMed.id,
                                    medicationName = updatedMed.name,
                                    stockRemaining = updatedMed.stockRemaining,
                                    refillThreshold = updatedMed.refillThreshold,
                                    form = updatedMed.form,
                                    dosage = updatedMed.dosage
                                )
                            }
                        }

                        // 2. Insert or update DoseLogEntity
                        val existingLog = db.doseLogDao().getLogForDose(medicationId, todayDate, scheduledTime)
                        if (existingLog != null) {
                            db.doseLogDao().updateLog(
                                existingLog.copy(
                                    status = "TAKEN",
                                    loggedTimestamp = System.currentTimeMillis(),
                                    notes = "Recorded via push notification 'Mark as Taken' action"
                                )
                            )
                        } else {
                            db.doseLogDao().insertLog(
                                DoseLogEntity(
                                    medicationId = medicationId,
                                    medicationName = medicationName,
                                    scheduledTime = scheduledTime,
                                    dateString = todayDate,
                                    loggedTimestamp = System.currentTimeMillis(),
                                    status = "TAKEN",
                                    notes = "Recorded via push notification 'Mark as Taken' action"
                                )
                            )
                        }
                        Log.d(TAG, "Successfully recorded dose as TAKEN in database for $medicationName")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error recording dose as TAKEN", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            NotificationHelper.ACTION_STOP_ALARM -> {
                Log.d(TAG, "User clicked STOP for $medicationName alarm - silenced audio, dose remains pending")
                AlarmPlaybackController.stopActiveAlarm(context, reason = "Stopped via Action")
            }

            NotificationHelper.ACTION_DISMISS_ALARM -> {
                Log.d(TAG, "User dismissed notification for $medicationName - silenced audio, dose remains pending")
                AlarmPlaybackController.stopActiveAlarm(context, reason = "Notification Dismissed")
            }

            NotificationHelper.ACTION_SNOOZE_DOSE -> {
                Log.d(TAG, "User clicked Snooze 10m for $medicationName ($scheduledTime)")
                AlarmPlaybackController.stopActiveAlarm(context, reason = "Snoozed")
                MedicationAlarmScheduler.scheduleSnoozeAlarm(
                    context = context,
                    medicationId = medicationId,
                    medicationName = medicationName,
                    dosage = dosage,
                    form = form,
                    scheduledTime = scheduledTime,
                    instructions = instructions,
                    snoozeMinutes = 10
                )
            }

            NotificationHelper.ACTION_REFILL_STOCK -> {
                val refillAmount = intent.getIntExtra(NotificationHelper.EXTRA_REFILL_AMOUNT, 30)
                Log.d(TAG, "User clicked Log Refill (+${refillAmount}) for $medicationName (ID $medicationId)")
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = MedTrackDatabase.getDatabase(context)
                        db.medicationDao().addStock(medicationId, refillAmount)
                        Log.i(TAG, "Added $refillAmount stock to medication ID $medicationId in Room DB.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding stock on refill action", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
