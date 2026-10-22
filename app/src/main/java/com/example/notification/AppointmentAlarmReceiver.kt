package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AppointmentAlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ApptAlarmReceiver"
        const val ACTION_APPOINTMENT_ALARM = "com.example.notification.ACTION_APPOINTMENT_ALARM"
        const val EXTRA_APPOINTMENT_ID = "extra_appt_id"
        const val EXTRA_DOCTOR_NAME = "extra_doctor_name"
        const val EXTRA_SPECIALTY = "extra_specialty"
        const val EXTRA_DATE_TIME = "extra_date_time"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_REASON = "extra_reason"
        const val EXTRA_LEAD_TIME_LABEL = "extra_lead_time_label"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != ACTION_APPOINTMENT_ALARM) return

        val apptId = intent.getLongExtra(EXTRA_APPOINTMENT_ID, -1L)
        val doctorName = intent.getStringExtra(EXTRA_DOCTOR_NAME) ?: "Doctor"
        val specialty = intent.getStringExtra(EXTRA_SPECIALTY) ?: "Specialist"
        val dateTimeString = intent.getStringExtra(EXTRA_DATE_TIME) ?: ""
        val location = intent.getStringExtra(EXTRA_LOCATION) ?: "Clinic"
        val reason = intent.getStringExtra(EXTRA_REASON) ?: "Checkup"
        val leadTimeLabel = intent.getStringExtra(EXTRA_LEAD_TIME_LABEL) ?: "Reminder"

        Log.d(TAG, "Doctor appointment alarm received for Dr. $doctorName (ID $apptId, $leadTimeLabel)")

        NotificationHelper.showDoctorAppointmentNotification(
            context = context,
            appointmentId = apptId,
            doctorName = doctorName,
            specialty = specialty,
            dateTimeString = dateTimeString,
            clinicLocation = location,
            reason = reason,
            leadTimeLabel = leadTimeLabel
        )
    }
}
