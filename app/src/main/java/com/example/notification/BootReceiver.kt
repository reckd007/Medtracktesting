package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.MedTrackDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MedBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Device rebooted or package replaced ($action). Rescheduling all medication intake alarms.")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = MedTrackDatabase.getDatabase(context)
                    val medications = db.medicationDao().getAllMedicationsSync()
                    MedicationAlarmScheduler.rescheduleAllAlarms(context, medications)
                    MedicationWorkScheduler.rescheduleAllMedicationWork(context, medications)
                    RefillScheduler.schedulePeriodicRefillCheck(context)
                    Log.d(TAG, "Successfully restored ${medications.size} medication alarms, WorkManager tasks, and periodic refill checks after boot.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule medication alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
