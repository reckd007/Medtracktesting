package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.DoctorAppointmentEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppointmentAlarmScheduler {
    private const val TAG = "ApptAlarmScheduler"

    fun getRequestCode(appointmentId: Long): Int {
        return (40000 + (appointmentId % 10000)).toInt()
    }

    fun parseAppointmentTimestamp(dateTimeString: String): Long? {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val formats = listOf(
            "EEE, MMM dd • hh:mm a",
            "EEE, MMM dd yyyy • hh:mm a",
            "MMM dd, yyyy • hh:mm a",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd hh:mm a",
            "yyyy-MM-dd'T'HH:mm"
        )

        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                val parsed = sdf.parse(dateTimeString)
                if (parsed != null) {
                    val cal = Calendar.getInstance().apply {
                        time = parsed
                    }
                    if (!fmt.contains("yyyy")) {
                        cal.set(Calendar.YEAR, currentYear)
                        if (cal.timeInMillis < System.currentTimeMillis() - 7 * 86400000L) {
                            cal.add(Calendar.YEAR, 1)
                        }
                    }
                    return cal.timeInMillis
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun getLeadTimeOffsetMillis(leadTimeCode: String): Long {
        return when (leadTimeCode.uppercase()) {
            "AT_TIME" -> 0L
            "1_HOUR_BEFORE" -> 3600 * 1000L
            "2_HOURS_BEFORE" -> 2 * 3600 * 1000L
            "12_HOURS_BEFORE" -> 12 * 3600 * 1000L
            "2_DAYS_BEFORE" -> 2 * 86400 * 1000L
            else -> 86400 * 1000L // 1_DAY_BEFORE
        }
    }

    fun getLeadTimeReadableLabel(leadTimeCode: String): String {
        return when (leadTimeCode.uppercase()) {
            "AT_TIME" -> "At time of visit"
            "1_HOUR_BEFORE" -> "1 hour before"
            "2_HOURS_BEFORE" -> "2 hours before"
            "12_HOURS_BEFORE" -> "12 hours before"
            "2_DAYS_BEFORE" -> "2 days before"
            else -> "1 day before"
        }
    }

    fun scheduleAppointmentReminder(context: Context, appointment: DoctorAppointmentEntity) {
        if (appointment.isCompleted) {
            cancelAppointmentReminder(context, appointment.id)
            return
        }

        val apptTimeMs = parseAppointmentTimestamp(appointment.dateTimeString) ?: return
        val offsetMs = getLeadTimeOffsetMillis(appointment.reminderLeadTime)
        val triggerAtMs = apptTimeMs - offsetMs

        // If reminder time has already passed, skip
        if (triggerAtMs <= System.currentTimeMillis()) {
            Log.d(TAG, "Appointment reminder time has already passed for Dr. ${appointment.doctorName}")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = getRequestCode(appointment.id)

        val intent = Intent(context, AppointmentAlarmReceiver::class.java).apply {
            action = AppointmentAlarmReceiver.ACTION_APPOINTMENT_ALARM
            putExtra(AppointmentAlarmReceiver.EXTRA_APPOINTMENT_ID, appointment.id)
            putExtra(AppointmentAlarmReceiver.EXTRA_DOCTOR_NAME, appointment.doctorName)
            putExtra(AppointmentAlarmReceiver.EXTRA_SPECIALTY, appointment.specialty)
            putExtra(AppointmentAlarmReceiver.EXTRA_DATE_TIME, appointment.dateTimeString)
            putExtra(AppointmentAlarmReceiver.EXTRA_LOCATION, appointment.clinicLocation)
            putExtra(AppointmentAlarmReceiver.EXTRA_REASON, appointment.reason)
            putExtra(AppointmentAlarmReceiver.EXTRA_LEAD_TIME_LABEL, getLeadTimeReadableLabel(appointment.reminderLeadTime))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }

        Log.i(
            TAG,
            "Scheduled reminder for Dr. ${appointment.doctorName} at ${Date(triggerAtMs)} (lead time: ${appointment.reminderLeadTime})"
        )
    }

    fun cancelAppointmentReminder(context: Context, appointmentId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = getRequestCode(appointmentId)

        val intent = Intent(context, AppointmentAlarmReceiver::class.java).apply {
            action = AppointmentAlarmReceiver.ACTION_APPOINTMENT_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled appointment reminder reqCode=$requestCode")
        }
    }

    fun rescheduleAllAppointments(context: Context, appointments: List<DoctorAppointmentEntity>) {
        appointments.filter { !it.isCompleted }.forEach { appt ->
            scheduleAppointmentReminder(context, appt)
        }
    }
}
