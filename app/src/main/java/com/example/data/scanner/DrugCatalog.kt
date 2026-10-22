package com.example.data.scanner

data class CatalogDrug(
    val barcode: String,
    val brandName: String,
    val genericName: String,
    val strength: String,
    val form: String, // "Capsule", "Tablet", "Liquid", "Injection"
    val colorHex: String,
    val recommendedFrequencyPerDay: Int,
    val recommendedDaysPerWeek: Int,
    val defaultTimingSlots: String, // e.g. "Morning,Evening"
    val defaultTimes: String, // e.g. "08:00,20:00"
    val defaultCourseDurationDays: Int, // 0 = Ongoing
    val defaultPackStock: Int,
    val defaultRefillThreshold: Int,
    val generalInstructions: String,
    val packageDescription: String
)

object DrugCatalog {
    val catalog: List<CatalogDrug> = listOf(
        CatalogDrug(
            barcode = "8901030384910",
            brandName = "Augmentin",
            genericName = "Amoxicillin & Clavulanate",
            strength = "625 mg",
            form = "Tablet",
            colorHex = "#0D9488",
            recommendedFrequencyPerDay = 2,
            recommendedDaysPerWeek = 7,
            defaultTimingSlots = "Morning,Evening",
            defaultTimes = "08:00,20:00",
            defaultCourseDurationDays = 7,
            defaultPackStock = 14,
            defaultRefillThreshold = 4,
            generalInstructions = "Take at the start of a meal to reduce digestive upset",
            packageDescription = "White blister strip with red/blue GSK pharmaceutical branding"
        ),
        CatalogDrug(
            barcode = "0031001311005",
            brandName = "Glucophage",
            genericName = "Metformin Hydrochloride",
            strength = "500 mg",
            form = "Tablet",
            colorHex = "#0284C7",
            recommendedFrequencyPerDay = 2,
            recommendedDaysPerWeek = 7,
            defaultTimingSlots = "Morning,Evening",
            defaultTimes = "08:00,20:00",
            defaultCourseDurationDays = 0, // Chronic ongoing
            defaultPackStock = 60,
            defaultRefillThreshold = 10,
            generalInstructions = "Take with or right after breakfast and dinner",
            packageDescription = "White bottle with blue crest, 500mg extended release"
        ),
        CatalogDrug(
            barcode = "0378018001014",
            brandName = "Lipitor",
            genericName = "Atorvastatin Calcium",
            strength = "20 mg",
            form = "Tablet",
            colorHex = "#0D9488",
            recommendedFrequencyPerDay = 1,
            recommendedDaysPerWeek = 7,
            defaultTimingSlots = "Evening",
            defaultTimes = "20:00",
            defaultCourseDurationDays = 0,
            defaultPackStock = 30,
            defaultRefillThreshold = 7,
            generalInstructions = "Take once daily in the evening, with or without food",
            packageDescription = "White carton box with green emblem, 30 film-coated tablets"
        ),
        CatalogDrug(
            barcode = "5000167054321",
            brandName = "Zithromax",
            genericName = "Azithromycin",
            strength = "250 mg",
            form = "Capsule",
            colorHex = "#10B981",
            recommendedFrequencyPerDay = 1,
            recommendedDaysPerWeek = 7,
            defaultTimingSlots = "Morning",
            defaultTimes = "09:00",
            defaultCourseDurationDays = 5,
            defaultPackStock = 6,
            defaultRefillThreshold = 2,
            generalInstructions = "Take on an empty stomach 1 hr before or 2 hrs after meal",
            packageDescription = "Pfizer red and white Z-Pak card pack (6 capsules)"
        ),
        CatalogDrug(
            barcode = "0007101562309",
            brandName = "Prinivil",
            genericName = "Lisinopril",
            strength = "10 mg",
            form = "Tablet",
            colorHex = "#14B8A6",
            recommendedFrequencyPerDay = 1,
            recommendedDaysPerWeek = 7,
            defaultTimingSlots = "Morning",
            defaultTimes = "08:00",
            defaultCourseDurationDays = 0,
            defaultPackStock = 30,
            defaultRefillThreshold = 7,
            generalInstructions = "Take first thing every morning at the same time",
            packageDescription = "Teal prescription vial, yellow round tablets"
        ),
        CatalogDrug(
            barcode = "0000600216104",
            brandName = "Ozempic",
            genericName = "Semaglutide",
            strength = "2 mg / 1.5 mL",
            form = "Injection",
            colorHex = "#8B5CF6",
            recommendedFrequencyPerDay = 1,
            recommendedDaysPerWeek = 1,
            defaultTimingSlots = "Evening",
            defaultTimes = "20:00",
            defaultCourseDurationDays = 0,
            defaultPackStock = 4, // 4 weekly doses per pen
            defaultRefillThreshold = 1,
            generalInstructions = "Inject subcutaneously once weekly into abdomen, thigh, or upper arm",
            packageDescription = "Novo Nordisk red pen box with disposable needles"
        ),
        CatalogDrug(
            barcode = "0004505146001",
            brandName = "Prilosec",
            genericName = "Omeprazole",
            strength = "20 mg",
            form = "Capsule",
            colorHex = "#F59E0B",
            recommendedFrequencyPerDay = 1,
            recommendedDaysPerWeek = 7,
            defaultTimingSlots = "Morning",
            defaultTimes = "07:30",
            defaultCourseDurationDays = 14,
            defaultPackStock = 14,
            defaultRefillThreshold = 3,
            generalInstructions = "Take 30 to 60 minutes before morning breakfast",
            packageDescription = "Purple carton box, delayed-release capsules"
        ),
        CatalogDrug(
            barcode = "0300450444501",
            brandName = "Advil Dual",
            genericName = "Ibuprofen & Acetaminophen",
            strength = "250 mg / 125 mg",
            form = "Tablet",
            colorHex = "#EF4444",
            recommendedFrequencyPerDay = 2,
            recommendedDaysPerWeek = 7,
            defaultTimingSlots = "Afternoon,Evening",
            defaultTimes = "13:00,21:00",
            defaultCourseDurationDays = 3,
            defaultPackStock = 20,
            defaultRefillThreshold = 5,
            generalInstructions = "Take with a full glass of water. Do not exceed 6 tablets in 24h",
            packageDescription = "Yellow/blue pocket blister pack"
        ),
        CatalogDrug(
            barcode = "300450449103",
            brandName = "Tylenol Extra Strength",
            genericName = "Acetaminophen",
            strength = "500 mg",
            form = "Tablet",
            colorHex = "#DC2626",
            recommendedFrequencyPerDay = 2,
            recommendedDaysPerWeek = 7,
            defaultTimingSlots = "Morning,Evening",
            defaultTimes = "08:00,20:00",
            defaultCourseDurationDays = 5,
            defaultPackStock = 50,
            defaultRefillThreshold = 10,
            generalInstructions = "Take 2 caplets every 6 hours while symptoms last. Do not exceed 6 caplets in 24h.",
            packageDescription = "Red Tylenol bottle with white safety cap"
        ),
        CatalogDrug(
            barcode = "305730169208",
            brandName = "Advil Liqui-Gels",
            genericName = "Ibuprofen",
            strength = "200 mg",
            form = "Capsule",
            colorHex = "#F97316",
            recommendedFrequencyPerDay = 2,
            recommendedDaysPerWeek = 7,
            defaultTimingSlots = "Morning,Evening",
            defaultTimes = "09:00,21:00",
            defaultCourseDurationDays = 4,
            defaultPackStock = 40,
            defaultRefillThreshold = 8,
            generalInstructions = "Take 1 liquid-filled capsule every 4 to 6 hours with food or milk if stomach upset occurs.",
            packageDescription = "Clear liquid-filled blue/gold gelatin capsules"
        ),
        CatalogDrug(
            barcode = "323900014316",
            brandName = "DayQuil Cold & Flu",
            genericName = "Acetaminophen, Dextromethorphan, Phenylephrine",
            strength = "325 mg / 10 mg / 5 mg per 15 mL",
            form = "Liquid",
            colorHex = "#EA580C",
            recommendedFrequencyPerDay = 3,
            recommendedDaysPerWeek = 5,
            defaultTimingSlots = "Morning,Afternoon,Evening",
            defaultTimes = "08:00,14:00,20:00",
            defaultCourseDurationDays = 5,
            defaultPackStock = 360, // 360 mL bottle
            defaultRefillThreshold = 60,
            generalInstructions = "Take 30 mL using dosage cup every 4 hours. Do not exceed 4 doses in 24 hours.",
            packageDescription = "Orange syrup bottle with calibrated measuring cup"
        ),
        CatalogDrug(
            barcode = "323900014323",
            brandName = "NyQuil Cold & Flu",
            genericName = "Acetaminophen, Doxylamine, Dextromethorphan",
            strength = "650 mg / 12.5 mg / 30 mg per 30 mL",
            form = "Liquid",
            colorHex = "#1E3A8A",
            recommendedFrequencyPerDay = 1,
            recommendedDaysPerWeek = 5,
            defaultTimingSlots = "Bedtime",
            defaultTimes = "22:00",
            defaultCourseDurationDays = 5,
            defaultPackStock = 360,
            defaultRefillThreshold = 60,
            generalInstructions = "Take 30 mL at bedtime with measuring cup. May cause significant drowsiness.",
            packageDescription = "Blue-green nighttime liquid bottle"
        ),
        CatalogDrug(
            barcode = "301201140228",
            brandName = "Pepto-Bismol",
            genericName = "Bismuth Subsalicylate",
            strength = "525 mg per 30 mL",
            form = "Suspension",
            colorHex = "#EC4899",
            recommendedFrequencyPerDay = 2,
            recommendedDaysPerWeek = 3,
            defaultTimingSlots = "Afternoon,Evening",
            defaultTimes = "12:00,18:00",
            defaultCourseDurationDays = 3,
            defaultPackStock = 354,
            defaultRefillThreshold = 60,
            generalInstructions = "Shake well before use. Measure with provided dosage cup.",
            packageDescription = "Pink thick suspension bottle with yellow cap"
        ),
        CatalogDrug(
            barcode = "359310579201",
            brandName = "Albuterol Inhalation Aerosol",
            genericName = "Albuterol Sulfate",
            strength = "90 mcg / actuation",
            form = "Inhaler",
            colorHex = "#0284C7",
            recommendedFrequencyPerDay = 2,
            recommendedDaysPerWeek = 7,
            defaultTimingSlots = "Morning,Evening",
            defaultTimes = "08:00,20:00",
            defaultCourseDurationDays = 0,
            defaultPackStock = 200, // 200 actuations / puffs
            defaultRefillThreshold = 20,
            generalInstructions = "Inhale 1-2 puffs every 4-6 hours as needed for bronchospasm. Rinse mouth after use.",
            packageDescription = "Blue actuator with dose counter canister"
        ),
        CatalogDrug(
            barcode = "041100809643",
            brandName = "Claritin 24 Hour Allergy",
            genericName = "Loratadine",
            strength = "10 mg",
            form = "Tablet",
            colorHex = "#10B981",
            recommendedFrequencyPerDay = 1,
            recommendedDaysPerWeek = 7,
            defaultTimingSlots = "Morning",
            defaultTimes = "08:00",
            defaultCourseDurationDays = 0,
            defaultPackStock = 30,
            defaultRefillThreshold = 5,
            generalInstructions = "Take 1 tablet daily with a full glass of water, with or without food.",
            packageDescription = "White and blue blister carton"
        ),
        CatalogDrug(
            barcode = "312547170258",
            brandName = "Benadryl Allergy",
            genericName = "Diphenhydramine HCl",
            strength = "25 mg",
            form = "Capsule",
            colorHex = "#E11D48",
            recommendedFrequencyPerDay = 2,
            recommendedDaysPerWeek = 5,
            defaultTimingSlots = "Evening,Bedtime",
            defaultTimes = "19:00,22:30",
            defaultCourseDurationDays = 5,
            defaultPackStock = 24,
            defaultRefillThreshold = 4,
            generalInstructions = "Take 1 to 2 capsules every 4 to 6 hours as needed for allergic reactions.",
            packageDescription = "Pink capsules in foil blister packaging"
        )
    )

    fun findByBarcode(code: String): CatalogDrug? {
        val clean = code.trim().replace("(", "").replace(")", "").removePrefix("01")
        val stripped = clean.trimStart('0')
        return catalog.firstOrNull { it.barcode == code.trim() }
            ?: catalog.firstOrNull { it.barcode == clean }
            ?: catalog.firstOrNull { it.barcode.trimStart('0') == stripped }
            ?: catalog.firstOrNull { 
                val catStripped = it.barcode.trimStart('0')
                catStripped == stripped || catStripped.endsWith(stripped) || stripped.endsWith(catStripped) 
            }
    }

    /**
     * Optical front-of-box pack recognizer simulation:
     * Analyzes detected text phrases from the front face of medicine packs.
     */
    fun analyzeFrontOfPackText(rawText: String): CatalogDrug? {
        val lower = rawText.lowercase()
        return catalog.firstOrNull { drug ->
            lower.contains(drug.brandName.lowercase()) ||
            lower.contains(drug.genericName.lowercase())
        }
    }
}
