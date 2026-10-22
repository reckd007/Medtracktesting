package com.example.ui.scanner

import java.util.Locale

/**
 * Extracted medicine attributes parsed from OCR text recognition.
 */
data class ExtractedMedicineDetails(
    val name: String,
    val dosage: String,
    val formFactor: String,
    val rawText: String = "",
    val confidenceNotes: String = ""
)

/**
 * Real-time text parser that analyzes lines and blocks of text detected on
 * medicine packaging, strips, blister packs, and bottles via Google ML Kit Text Recognition.
 */
object MedicineTextParser {

    // Common dosage regex: captures numbers followed by mg, g, ml, mcg, or IU
    private val DOSAGE_REGEX = Regex(
        """(?i)\b(\d+(?:\.\d+)?)\s*(mg|mcg|ml|g|iu)\b|\b(\d+(?:\.\d+)?)\s*(?:mg|mcg|ml|g|iu)\s*/\s*(\d+(?:\.\d+)?)\s*(?:mg|mcg|ml|g|iu)\b|\b(\d+)\s*(?:iu|units)\b""",
        RegexOption.IGNORE_CASE
    )

    // Form factor keywords mapping
    private val TABLET_CAPSULE_KEYWORDS = listOf(
        "tablet", "tablets", "tab", "tabs", "cap", "caps", "capsule", "capsules", "pill", "pills", "caplet", "caplets"
    )
    private val LIQUID_KEYWORDS = listOf(
        "syrup", "suspension", "liquid", "solution", "elixir", "drops", "oral liquid", "oral suspension", "oral solution"
    )
    private val INJECTION_KEYWORDS = listOf(
        "injection", "vial", "ampoule", "ampul", "injectable", "infusion", "pen injector"
    )
    private val TOPICAL_KEYWORDS = listOf(
        "ointment", "cream", "gel", "lotion", "liniment", "patch", "topical"
    )

    // Boilerplate words on pharma packaging to filter out when extracting the medicine name
    private val BOILERPLATE_PATTERNS = listOf(
        "mfg", "exp", "expiry", "batch", "lot", "b.no", "m.r.p", "mrp", "rx only", "rx",
        "keep out of reach", "store below", "protect from", "manufactured", "marketed",
        "distributed", "dosage", "as directed", "physician", "composition", "each film",
        "each tablet", "each capsule", "contains", "schedule", "warning", "for external",
        "net qty", "regd", "trademark", "lic. no", "licence", "tablets ip", "capsules ip",
        "oral use", "not for injection", "shake well", "import", "pack of", "taxes"
    )

    // Recognized common brand & generic pharmaceuticals for high-precision extraction
    private val COMMON_MEDICINE_NAMES = listOf(
        "Paracetamol", "Dolo 650", "Dolo", "Augmentin", "Amoxicillin", "Metformin", "Glucophage",
        "Ibuprofen", "Advil", "Motrin", "Cetirizine", "Zyrtec", "Azithromycin", "Zithromax",
        "Lipitor", "Atorvastatin", "Omeprazole", "Prilosec", "Pantoprazole", "Pantocid",
        "Amlodipine", "Norvasc", "Losartan", "Cozaar", "Crocin", "Calpol", "Tylenol",
        "Aspirin", "Disprin", "Ciprofloxacin", "Cipro", "Levothyroxine", "Synthroid",
        "Lisinopril", "Zestril", "Gabapentin", "Neurontin", "Prednisone", "Metoprolol",
        "Lopressor", "Albuterol", "Ventolin", "Insulin", "Lantus", "Ozempic", "Wegovy",
        "Eliquis", "Apixaban", "Xarelto", "Rivaroxaban", "Panadol", "Combiflam", "Benadryl",
        "Montelukast", "Singulair", "Fluoxetine", "Prozac", "Sertraline", "Zoloft", "Escitalopram",
        "Lexapro", "Clopidogrel", "Plavix", "Rosuvastatin", "Crestor", "Duloxetine", "Cymbalta",
        "Hydrochlorothiazide", "Tramadol", "Ultram", "Diclofenac", "Voltaren", "Meftal-Spas"
    )

    /**
     * Parses raw OCR text into structured medicine details:
     * - Name: Primary brand or generic name
     * - Dosage: Strength with units (e.g. 650 mg, 500 mg, 5 ml)
     * - FormFactor: "Tablet / Capsule", "Liquid", "Injection", "Topical", or "Other"
     */
    fun parse(rawText: String): ExtractedMedicineDetails? {
        if (rawText.isBlank()) return null

        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return null

        // 1. EXTRACT DOSAGE / STRENGTH
        val extractedDosage = extractDosage(rawText, lines)

        // 2. EXTRACT FORM FACTOR
        val extractedFormFactor = extractFormFactor(rawText)

        // 3. EXTRACT MEDICINE NAME
        val extractedName = extractMedicineName(lines, extractedDosage)

        if (extractedName.isBlank() && extractedDosage.isBlank() && extractedFormFactor == "Other") {
            return null
        }

        return ExtractedMedicineDetails(
            name = if (extractedName.isNotBlank()) extractedName else "Unknown Medicine",
            dosage = if (extractedDosage.isNotBlank()) extractedDosage else "500 mg",
            formFactor = extractedFormFactor,
            rawText = rawText,
            confidenceNotes = "Parsed via ML Kit Text Recognition OCR"
        )
    }

    /**
     * Extracts dosage string with unit (e.g. "650 mg", "500 mg", "5 ml") using regex.
     */
    fun extractDosage(rawText: String, lines: List<String>): String {
        // Look through lines first for higher accuracy
        for (line in lines) {
            val match = DOSAGE_REGEX.find(line)
            if (match != null) {
                return normalizeDosage(match.value)
            }
        }
        // Fallback to full text search
        val match = DOSAGE_REGEX.find(rawText)
        if (match != null) {
            return normalizeDosage(match.value)
        }
        return ""
    }

    private fun normalizeDosage(raw: String): String {
        val clean = raw.trim()
        val numMatch = Regex("""^(\d+(?:\.\d+)?)\s*([a-zA-Z%]+.*)$""").find(clean)
        return if (numMatch != null) {
            val num = numMatch.groupValues[1]
            val unit = numMatch.groupValues[2].trim()
            val cleanUnit = when (unit.lowercase(Locale.ROOT)) {
                "mg" -> "mg"
                "mcg" -> "mcg"
                "ml" -> "ml"
                "g" -> "g"
                "iu" -> "IU"
                else -> unit
            }
            "$num $cleanUnit"
        } else {
            clean
        }
    }

    /**
     * Determines form factor based on recognized pharmaceutical keywords.
     * Defaults to "Other" if no keyword matches instead of forcing "pills".
     */
    fun extractFormFactor(text: String): String {
        val lower = text.lowercase(Locale.ROOT)

        // 1. Check Tablet / Capsule
        for (keyword in TABLET_CAPSULE_KEYWORDS) {
            if (Regex("""\b${Regex.escape(keyword)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower)) {
                return "Tablet / Capsule"
            }
        }

        // 2. Check Injection (prioritize over liquid solution/vial)
        for (keyword in INJECTION_KEYWORDS) {
            if (Regex("""\b${Regex.escape(keyword)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower)) {
                return "Injection"
            }
        }

        // 3. Check Topical (gel, ointment, cream, lotion)
        for (keyword in TOPICAL_KEYWORDS) {
            if (Regex("""\b${Regex.escape(keyword)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower)) {
                return "Topical"
            }
        }

        // 4. Check Liquid (syrup, suspension, oral solution, drops)
        for (keyword in LIQUID_KEYWORDS) {
            if (Regex("""\b${Regex.escape(keyword)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower)) {
                return "Liquid"
            }
        }

        // Default to "Other" (or leave unselected) rather than hardcoded "pills"
        return "Other"
    }

    /**
     * Extracts the primary brand or generic medicine name:
     * - Checks for direct pharma catalog matches line-by-line on primary non-boilerplate text
     * - Filters out packaging boilerplate (Batch, Exp, Mfg, Rx, Warning, etc.)
     * - Picks the most prominent valid candidate line
     */
    fun extractMedicineName(lines: List<String>, detectedDosage: String): String {
        // First pass: Check line-by-line from the top (where brand name is most prominent)
        for (line in lines) {
            val lowerLine = line.lowercase(Locale.ROOT)
            val isBoilerplate = BOILERPLATE_PATTERNS.any { lowerLine.contains(it) }
            if (isBoilerplate) continue

            for (known in COMMON_MEDICINE_NAMES) {
                if (Regex("""\b${Regex.escape(known)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(line)) {
                    return known
                }
            }
        }

        // Second pass: Direct match against full text
        val fullCombined = lines.joinToString(" ")
        for (known in COMMON_MEDICINE_NAMES) {
            if (Regex("""\b${Regex.escape(known)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(fullCombined)) {
                return known
            }
        }

        // Third pass: Filter and evaluate lines
        val candidates = mutableListOf<String>()

        for (line in lines) {
            val cleanLine = line.trim()
            if (cleanLine.length < 3) continue

            val lower = cleanLine.lowercase(Locale.ROOT)

            // Skip boilerplate lines
            val isBoilerplate = BOILERPLATE_PATTERNS.any { lower.contains(it) }
            if (isBoilerplate) continue

            // Must contain at least some alphabet characters
            val letterCount = cleanLine.count { it.isLetter() }
            if (letterCount < 3) continue

            // Filter out purely date or barcode lines
            if (cleanLine.matches(Regex("""^[\d\s\W\.\-/:,]+$"""))) continue

            // Strip suffix like "Tablets IP", "Capsules USP", "Syrup", etc. from name candidate
            var stripped = cleanLine
            for (kw in TABLET_CAPSULE_KEYWORDS + LIQUID_KEYWORDS + INJECTION_KEYWORDS + TOPICAL_KEYWORDS + listOf("IP", "USP", "BP")) {
                stripped = stripped.replace(Regex("""(?i)\b${Regex.escape(kw)}\b"""), "").trim()
            }
            // Strip detected dosage from name line if present (e.g. "Augmentin 625mg" -> "Augmentin 625")
            if (detectedDosage.isNotBlank()) {
                val dosageUnit = detectedDosage.substringAfter(" ", "").trim()
                if (dosageUnit.isNotBlank()) {
                    stripped = stripped.replace(Regex("""(?i)\b$dosageUnit\b"""), "").trim()
                }
            }
            stripped = stripped.replace(Regex("""[,\-.:;]+$"""), "").trim()

            if (stripped.length >= 3 && stripped.any { it.isLetter() }) {
                candidates.add(stripped)
            }
        }

        // Return the first high-quality candidate line (usually the largest text on the pack)
        val best = candidates.firstOrNull() ?: ""
        return if (best.isNotBlank()) {
            // Capitalize properly
            best.split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { word ->
                    word.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                }
        } else {
            ""
        }
    }
}
