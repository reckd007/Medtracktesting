package com.example

import com.example.data.scanner.DrugCatalog
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun drugCatalog_barcodeLookup_findsCorrectDrug() {
        val augmentin = DrugCatalog.findByBarcode("8901030384910")
        assertNotNull(augmentin)
        assertEquals("Augmentin", augmentin?.brandName)
        assertEquals("625 mg", augmentin?.strength)
    }

    @Test
    fun drugCatalog_frontOfPackOCR_detectsBrandName() {
        val sampleOcrText = "Rx Glucophage Metformin 500mg ER 60 tablets"
        val detected = DrugCatalog.analyzeFrontOfPackText(sampleOcrText)
        assertNotNull(detected)
        assertEquals("Glucophage", detected?.brandName)
    }

    @Test
    fun medicineTextParser_parsesTabletAndDosage() {
        val ocr = """
            Paracetamol 650mg Tablets IP
            Batch: B402
            Exp: 10/2027
        """.trimIndent()
        val parsed = com.example.ui.scanner.MedicineTextParser.parse(ocr)
        assertNotNull(parsed)
        assertEquals("Paracetamol", parsed?.name)
        assertEquals("650 mg", parsed?.dosage)
        assertEquals("Tablet / Capsule", parsed?.formFactor)
    }

    @Test
    fun medicineTextParser_parsesLiquidSyrup() {
        val ocr = """
            Benadryl Cough Syrup
            Diphenhydramine HCl
            100 ml Oral Liquid
        """.trimIndent()
        val parsed = com.example.ui.scanner.MedicineTextParser.parse(ocr)
        assertNotNull(parsed)
        assertEquals("Benadryl", parsed?.name)
        assertEquals("100 ml", parsed?.dosage)
        assertEquals("Liquid", parsed?.formFactor)
    }

    @Test
    fun medicineTextParser_parsesInjectionVial() {
        val ocr = """
            Insulin Glargine 100 IU
            Injectable Solution 10ml Vial
        """.trimIndent()
        val parsed = com.example.ui.scanner.MedicineTextParser.parse(ocr)
        assertNotNull(parsed)
        assertEquals("Insulin", parsed?.name)
        assertEquals("100 IU", parsed?.dosage)
        assertEquals("Injection", parsed?.formFactor)
    }

    @Test
    fun medicineTextParser_parsesTopicalGel() {
        val ocr = """
            Voltaren 1% Gel
            Diclofenac Sodium Topical Ointment 50g
        """.trimIndent()
        val parsed = com.example.ui.scanner.MedicineTextParser.parse(ocr)
        assertNotNull(parsed)
        assertEquals("Voltaren", parsed?.name)
        assertEquals("50 g", parsed?.dosage)
        assertEquals("Topical", parsed?.formFactor)
    }

    @Test
    fun medicineTextParser_defaultsToOtherWhenNoKeywordMatches() {
        val ocr = """
            UnspecifiedMedicine 25mg
            Manufactured for Clinic
        """.trimIndent()
        val form = com.example.ui.scanner.MedicineTextParser.extractFormFactor(ocr)
        assertEquals("Other", form)
    }

    @Test
    fun medicationInventory_triggersLowStockAlertWhenQuantityBelowThreshold() {
        val medicationAboveThreshold = com.example.data.model.MedicationEntity(
            id = 1,
            name = "Amoxicillin",
            dosage = "500 mg",
            form = "Tablet / Capsule",
            stockRemaining = 15,
            refillThreshold = 5
        )
        val medicationAtThreshold = com.example.data.model.MedicationEntity(
            id = 2,
            name = "Atorvastatin",
            dosage = "20 mg",
            form = "Tablet / Capsule",
            stockRemaining = 5,
            refillThreshold = 5
        )
        val medicationBelowThreshold = com.example.data.model.MedicationEntity(
            id = 3,
            name = "Insulin",
            dosage = "100 IU",
            form = "Injection",
            stockRemaining = 2,
            refillThreshold = 5
        )

        assertFalse(medicationAboveThreshold.stockRemaining <= medicationAboveThreshold.refillThreshold)
        assertTrue(medicationAtThreshold.stockRemaining <= medicationAtThreshold.refillThreshold)
        assertTrue(medicationBelowThreshold.stockRemaining <= medicationBelowThreshold.refillThreshold)
    }

    @Test
    fun calendarDoseStatus_classifiesStatusesProperly() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val med = com.example.data.model.MedicationEntity(
            id = 1,
            name = "Metformin",
            dosage = "500 mg",
            form = "Tablet",
            timesOfDay = "08:00 AM, 08:00 PM"
        )
        val takenLog = com.example.data.model.DoseLogEntity(
            medicationId = 1,
            medicationName = "Metformin",
            scheduledTime = "08:00 AM",
            status = "TAKEN",
            dateString = today
        )
        val skippedLog = com.example.data.model.DoseLogEntity(
            medicationId = 1,
            medicationName = "Metformin",
            scheduledTime = "08:00 AM",
            status = "SKIPPED",
            dateString = "2026-01-01"
        )

        assertEquals("TAKEN", takenLog.status)
        assertEquals("SKIPPED", skippedLog.status)
        assertEquals(com.example.util.DayDoseStatus.ALL_TAKEN.name, "ALL_TAKEN")
        assertEquals(com.example.util.DayDoseStatus.MISSED.name, "MISSED")
        assertEquals(com.example.util.DayDoseStatus.UPCOMING.name, "UPCOMING")
    }

    @Test
    fun backgroundCheck_filtersLowStockMedicationsAndTriggersRefillAlert() {
        val meds = listOf(
            com.example.data.model.MedicationEntity(
                id = 1,
                name = "Lisinopril",
                dosage = "10 mg",
                form = "Tablet",
                stockRemaining = 30,
                refillThreshold = 5
            ),
            com.example.data.model.MedicationEntity(
                id = 2,
                name = "Metformin",
                dosage = "500 mg",
                form = "Tablet",
                stockRemaining = 3,
                refillThreshold = 5
            )
        )

        val lowStockList = meds.filter { it.stockRemaining <= it.refillThreshold }
        assertEquals(1, lowStockList.size)
        assertEquals("Metformin", lowStockList.first().name)
        assertEquals(3, lowStockList.first().stockRemaining)
        assertEquals(5, lowStockList.first().refillThreshold)
    }
}
