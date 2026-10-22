package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Manages periodic scheduling of the medication refill quantity checks
 * using Android's JobScheduler and AlarmManager.
 */
object RefillScheduler {
    private const val TAG = "RefillScheduler"
    private const val REFILL_JOB_ID = RefillJobService.JOB_ID
    private const val REFILL_ALARM_REQUEST_CODE = 4001
    private const val SIX_HOURS_MILLIS = 6 * 60 * 60 * 1000L

    /**
     * Schedules periodic background execution using JobScheduler.
     */
    fun schedulePeriodicRefillCheck(context: Context) {
        try {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, RefillJobService::class.java)

            val jobInfo = JobInfo.Builder(REFILL_JOB_ID, componentName)
                .setPeriodic(SIX_HOURS_MILLIS) // Runs every 6 hours
                .setPersisted(true) // Survives device reboots
                .setRequiresBatteryNotLow(false)
                .build()

            val result = jobScheduler.schedule(jobInfo)
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.i(TAG, "Periodic RefillJobService scheduled successfully (every 6h, Job ID $REFILL_JOB_ID).")
            } else {
                Log.w(TAG, "Failed to schedule periodic RefillJobService via JobScheduler.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling RefillJobService: ${e.message}", e)
        }

        // Also schedule a repeating AlarmManager alarm as a secondary guarantee
        scheduleAlarmManagerRefillCheck(context)
    }

    private fun scheduleAlarmManagerRefillCheck(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, RefillCheckReceiver::class.java).apply {
                action = RefillCheckReceiver.ACTION_CHECK_REFILLS
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REFILL_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAtMillis = System.currentTimeMillis() + SIX_HOURS_MILLIS
            alarmManager.setInexactRepeating(
                AlarmManager.RTC,
                triggerAtMillis,
                SIX_HOURS_MILLIS,
                pendingIntent
            )
            Log.d(TAG, "Secondary AlarmManager refill check scheduled.")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling AlarmManager refill alarm: ${e.message}", e)
        }
    }

    /**
     * Immediately triggers the background service to inspect Room database quantities
     * against user thresholds and fire push notifications.
     */
    fun triggerRefillCheckService(context: Context) {
        Log.i(TAG, "Manually triggering RefillCheckBackgroundService immediately.")
        RefillCheckBackgroundService.start(context)
    }
}
