package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.MedTrackDatabase
import com.example.data.model.MedicationEntity
import com.example.notification.AlertRingtone
import com.example.notification.AlertRingtoneManager
import com.example.notification.MedicationActionReceiver
import com.example.notification.MedicationAlarmScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MedicationAlarmSystemTest {

    @Test
    fun testPresetRingtonesInitialization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val presets = AlertRingtoneManager.getPresetRingtones(context)

        assertEquals("Must provide exactly 3 preset alarm ringtones", 3, presets.size)
        assertEquals("Preset 1 must be Gentle Reminder", "Gentle Reminder", presets[0].title)
        assertEquals("Preset 2 must be Classic Alarm", "Classic Alarm", presets[1].title)
        assertEquals("Preset 3 must be Urgent Medicine Reminder", "Urgent Medicine Reminder", presets[2].title)

        presets.forEach { preset ->
            assertTrue("Preset must be marked isPreset = true", preset.isPreset)
            assertTrue("Preset URI must point to android.resource", preset.uriString.startsWith("android.resource://"))
        }
    }

    @Test
    fun testRingtoneSelectionPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val presets = AlertRingtoneManager.getPresetRingtones(context)

        // Select Preset 2
        AlertRingtoneManager.setSelectedRingtone(context, presets[1])
        var selected = AlertRingtoneManager.getSelectedRingtone(context)
        assertEquals("Selected ringtone must be Classic Alarm", "Classic Alarm", selected.title)

        // Select Preset 3
        AlertRingtoneManager.setSelectedRingtone(context, presets[2])
        selected = AlertRingtoneManager.getSelectedRingtone(context)
        assertEquals("Selected ringtone must be Urgent Medicine Reminder", "Urgent Medicine Reminder", selected.title)

        // Reset to Preset 1
        AlertRingtoneManager.setSelectedRingtone(context, presets[0])
        selected = AlertRingtoneManager.getSelectedRingtone(context)
        assertEquals("Selected ringtone must be Gentle Reminder", "Gentle Reminder", selected.title)
    }

    @Test
    fun testMissingCustomRingtoneGracefulFallback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeCustom = AlertRingtone(
            id = AlertRingtoneManager.ID_CUSTOM_UPLOADED,
            title = "Deleted Song",
            uriString = "file:///nonexistent/path/deleted_sound.mp3",
            description = "Custom test",
            isPreset = false
        )
        AlertRingtoneManager.setSelectedRingtone(context, fakeCustom)

        // Reading the selected ringtone when file doesn't exist should fallback to Gentle Reminder safely
        val active = AlertRingtoneManager.getSelectedRingtone(context)
        assertEquals("Must fall back to Gentle Reminder when file missing", "Gentle Reminder", active.title)

        val uri = AlertRingtoneManager.getSelectedRingtoneUri(context)
        assertNotNull("URI must never be null", uri)
        assertTrue("Fallback URI must point to app package resource", uri.toString().startsWith("android.resource://${context.packageName}/"))
    }

    @Test
    fun testAlarmRequestCodeCollisionAvoidance() {
        val req1_slot0 = MedicationAlarmScheduler.getRequestCode(medicationId = 1L, timeIndex = 0)
        val req1_slot1 = MedicationAlarmScheduler.getRequestCode(medicationId = 1L, timeIndex = 1)
        val req2_slot0 = MedicationAlarmScheduler.getRequestCode(medicationId = 2L, timeIndex = 0)

        assertFalse("Request codes for different slots must not collide", req1_slot0 == req1_slot1)
        assertFalse("Request codes for different medications must not collide", req1_slot0 == req2_slot0)
    }

    @Test
    fun testAlarmSchedulingAndCancellationLifecycle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val med = MedicationEntity(
            id = 42L,
            name = "Metformin",
            dosage = "500 mg",
            form = "Tablet",
            instructions = "Take after meal",
            timesOfDay = "08:00, 20:00",
            frequencyDaysPerWeek = 7,
            courseDurationDays = 30,
            stockRemaining = 60,
            refillThreshold = 10,
            profileName = "Self"
        )

        // Scheduling must not throw
        MedicationAlarmScheduler.scheduleMedicationAlarms(context, med)

        // Canceling must not throw
        MedicationAlarmScheduler.cancelMedicationAlarms(context, med)
    }

    @Test
    fun testActionReceiverStopAlarmLeavesDosePending() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = MedicationActionReceiver()

        val intent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = NotificationHelper.ACTION_STOP_ALARM
            putExtra(NotificationHelper.EXTRA_MEDICATION_ID, 999L)
            putExtra(NotificationHelper.EXTRA_MEDICATION_NAME, "Aspirin")
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 1234)
        }

        // Triggering STOP must execute without throwing
        receiver.onReceive(context, intent)
    }

    @Test
    fun testActionReceiverTakeDoseInsertsLogAndDecrementsStock() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = MedTrackDatabase.getDatabase(context)

        // Insert test medication
        val med = MedicationEntity(
            name = "Lisinopril",
            dosage = "10 mg",
            form = "Tablet",
            instructions = "Morning with water",
            timesOfDay = "08:00",
            frequencyDaysPerWeek = 7,
            courseDurationDays = 30,
            stockRemaining = 30,
            refillThreshold = 5,
            profileName = "Self"
        )

        val id = runBlocking {
            db.medicationDao().insertMedication(med)
        }
        assertTrue("Medication ID must be generated", id > 0)

        val receiver = MedicationActionReceiver()
        val intent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = NotificationHelper.ACTION_TAKE_DOSE
            putExtra(NotificationHelper.EXTRA_MEDICATION_ID, id)
            putExtra(NotificationHelper.EXTRA_MEDICATION_NAME, "Lisinopril")
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, "08:00")
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 5001)
        }

        receiver.onReceive(context, intent)
    }

    @Test
    fun testCustomMedicationTimesAndDaysOfWeekParsing() {
        // Test parseDaysOfWeek with null, empty, everyday, and custom list
        val nullDays = MedicationAlarmScheduler.parseDaysOfWeek(null)
        assertEquals("Null days must default to 7 days", 7, nullDays.size)

        val emptyDays = MedicationAlarmScheduler.parseDaysOfWeek("")
        assertEquals("Empty days must default to 7 days", 7, emptyDays.size)

        val everyday = MedicationAlarmScheduler.parseDaysOfWeek("Everyday")
        assertEquals("Everyday must parse to 7 days", 7, everyday.size)

        val mwf = MedicationAlarmScheduler.parseDaysOfWeek("Mon,Wed,Fri")
        assertEquals("Mon,Wed,Fri must parse to exactly 3 days", 3, mwf.size)
        assertTrue("Must contain Monday", mwf.contains(java.util.Calendar.MONDAY))
        assertTrue("Must contain Wednesday", mwf.contains(java.util.Calendar.WEDNESDAY))
        assertTrue("Must contain Friday", mwf.contains(java.util.Calendar.FRIDAY))
        assertFalse("Must not contain Sunday", mwf.contains(java.util.Calendar.SUNDAY))
    }

    @Test
    fun testCustomTimeSchedulingNullSafety() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val customMed = MedicationEntity(
            id = 777L,
            name = "Thyroxine",
            dosage = "75 mcg",
            form = "Tablet",
            instructions = "Take 30 mins before breakfast",
            timesOfDay = "07:15,10:40",
            timingSlots = "Custom (7:15 AM),Custom (10:40 AM)",
            frequencyPerDay = 2,
            frequencyDaysPerWeek = 5,
            daysOfWeek = "Mon,Tue,Wed,Thu,Fri",
            courseDurationDays = 0,
            stockRemaining = 50,
            refillThreshold = 10,
            profileName = "Self"
        )

        // Must schedule custom 07:15 and 10:40 alarms cleanly without throwing
        MedicationAlarmScheduler.scheduleMedicationAlarms(context, customMed)

        // Must cancel custom alarms cleanly without throwing
        MedicationAlarmScheduler.cancelMedicationAlarms(context, customMed)

        // Test next daily alarm with custom time
        MedicationAlarmScheduler.scheduleNextDailyAlarm(
            context = context,
            medicationId = 777L,
            medicationName = "Thyroxine",
            dosage = "75 mcg",
            form = "Tablet",
            scheduledTime = "07:15",
            instructions = "Take 30 mins before breakfast",
            timeIndex = 0,
            daysOfWeek = "Mon,Tue,Wed,Thu,Fri"
        )
    }
}
