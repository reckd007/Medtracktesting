package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY id ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications ORDER BY id ASC")
    suspend fun getAllMedicationsSync(): List<MedicationEntity>

    @Query("SELECT COUNT(*) FROM medications")
    suspend fun getCount(): Int

    @Query("SELECT * FROM medications WHERE profileName = :profile ORDER BY id ASC")
    fun getMedicationsByProfile(profile: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE stockRemaining <= refillThreshold ORDER BY stockRemaining ASC")
    fun getLowStockMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE stockRemaining <= refillThreshold ORDER BY stockRemaining ASC")
    suspend fun getLowStockMedicationsSync(): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: Long): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medications: List<MedicationEntity>)

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("UPDATE medications SET stockRemaining = :newStock WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Int)

    @Query("UPDATE medications SET stockRemaining = stockRemaining - 1 WHERE id = :id AND stockRemaining > 0")
    suspend fun decrementStock(id: Long)

    @Query("UPDATE medications SET stockRemaining = stockRemaining + :amount WHERE id = :id")
    suspend fun addStock(id: Long, amount: Int)

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity)
}

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs WHERE dateString = :dateString")
    fun getLogsForDate(dateString: String): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs ORDER BY loggedTimestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId AND dateString = :dateString AND scheduledTime = :time LIMIT 1")
    suspend fun getLogForDose(medicationId: Long, dateString: String, time: String): DoseLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DoseLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<DoseLogEntity>)

    @Update
    suspend fun updateLog(log: DoseLogEntity)

    @Query("DELETE FROM dose_logs WHERE medicationId = :medicationId AND dateString = :dateString AND scheduledTime = :time")
    suspend fun deleteLogForDose(medicationId: Long, dateString: String, time: String)
}

@Dao
interface HealthDiaryDao {
    @Query("SELECT * FROM health_diary ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<HealthDiaryEntity>>

    @Query("SELECT * FROM health_diary ORDER BY timestamp DESC LIMIT 30")
    fun getRecentEntries(): Flow<List<HealthDiaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HealthDiaryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<HealthDiaryEntity>)

    @Delete
    suspend fun deleteEntry(entry: HealthDiaryEntity)
}

@Dao
interface DoctorAppointmentDao {
    @Query("SELECT * FROM doctor_appointments ORDER BY dateTimeString ASC")
    fun getAllAppointments(): Flow<List<DoctorAppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: DoctorAppointmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appointments: List<DoctorAppointmentEntity>)

    @Update
    suspend fun updateAppointment(appointment: DoctorAppointmentEntity)

    @Delete
    suspend fun deleteAppointment(appointment: DoctorAppointmentEntity)
}

@Dao
interface InjectionSiteDao {
    @Query("SELECT * FROM injection_site_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<InjectionSiteLogEntity>>

    @Query("SELECT * FROM injection_site_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLog(): Flow<InjectionSiteLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: InjectionSiteLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<InjectionSiteLogEntity>)
}
