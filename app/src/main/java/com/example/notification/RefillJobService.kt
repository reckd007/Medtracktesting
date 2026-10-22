package com.example.notification

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.example.data.db.MedTrackDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * JobService executed periodically by Android JobScheduler to ensure
 * medication quantities in the Room database are verified even when the app is idle.
 */
class RefillJobService : JobService() {

    companion object {
        const val TAG = "RefillJobService"
        const val JOB_ID = 3001
    }

    private val serviceJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i(TAG, "JobScheduler initiated RefillJobService. Checking Room database quantities...")

        coroutineScope.launch {
            try {
                val db = MedTrackDatabase.getDatabase(applicationContext)
                val medications = db.medicationDao().getAllMedicationsSync()
                var alertsSent = 0

                for (med in medications) {
                    if (med.stockRemaining <= med.refillThreshold) {
                        alertsSent++
                        NotificationHelper.showRefillNotification(
                            context = applicationContext,
                            medicationId = med.id,
                            medicationName = med.name,
                            stockRemaining = med.stockRemaining,
                            refillThreshold = med.refillThreshold,
                            form = med.form,
                            dosage = med.dosage
                        )
                    }
                }
                Log.i(TAG, "RefillJobService check finished: $alertsSent refill notifications dispatched.")
                jobFinished(params, false)
            } catch (e: Exception) {
                Log.e(TAG, "Error during RefillJobService execution", e)
                jobFinished(params, true)
            }
        }

        return true // Task is running asynchronously in coroutine
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.w(TAG, "RefillJobService stopped prematurely by system.")
        coroutineScope.cancel()
        return true // Reschedule if cancelled
    }
}
