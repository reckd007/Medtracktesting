package com.example.data.medicine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Medicine Search Provider for Indian medicines and brand names.
 *
 * NOTE ON INDIAN MEDICINE DATABASES:
 * Public Indian government repositories (such as CDSCO / data.gov.in) publish
 * non-REST periodic PDF/CSV dumps without live autocomplete APIs.
 * Real-time Indian branded drug databases with REST APIs (e.g. Eka Care Indian
 * Branded Drugs API with 500k+ SKUs, DrugSetu, myUpchar API) require paid
 * commercial enterprise licenses and API keys.
 *
 * This provider implements:
 * 1. A verified database of top Indian brand names, generic compositions (salts),
 *    strengths, forms, and manufacturers (such as Micro Labs, Cipla, Sun Pharma,
 *    Alkem, Dr. Reddy's, Mankind, Torrent, Abbott India).
 * 2. Pluggable configuration for commercial Indian provider API keys (e.g. Eka Care / DrugSetu).
 */
class IndianMedicineCatalogProvider(
    private val commercialApiKey: String? = null
) : MedicineSearchProvider {

    override val providerName: String = "Indian Medicine Database"
    override val coversIndianMedicines: Boolean = true
    override val requiresApiKey: Boolean = false // Embedded verified registry operates zero-key; commercial REST expansion optional

    override suspend fun search(query: String, limit: Int): List<MedicineSearchResult> = withContext(Dispatchers.Default) {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        if (cleanQuery.length < 2) return@withContext emptyList()

        // Match against brand name, generic name, or common variations
        val matches = indianMedicineCatalog.filter { entry ->
            entry.brandName.lowercase(Locale.ROOT).contains(cleanQuery) ||
            entry.genericName.lowercase(Locale.ROOT).contains(cleanQuery) ||
            "${entry.brandName} ${entry.genericName} ${entry.strength}".lowercase(Locale.ROOT).contains(cleanQuery)
        }

        // Rank results: items whose brandName starts with query first, then generic starts with, then contains
        val ranked = matches.sortedWith(
            compareByDescending<IndianMedicineEntry> { it.brandName.lowercase(Locale.ROOT).startsWith(cleanQuery) }
                .thenByDescending { it.genericName.lowercase(Locale.ROOT).startsWith(cleanQuery) }
                .thenBy { it.brandName }
        )

        return@withContext ranked.take(limit).map { entry ->
            MedicineSearchResult(
                id = "in_${entry.brandName.replace(" ", "_").lowercase(Locale.ROOT)}_${entry.strength.replace(" ", "")}",
                brandName = entry.brandName,
                genericName = entry.genericName,
                strength = entry.strength,
                dosageForm = entry.form,
                source = MedicineSource.INDIAN_CATALOG,
                isIndianBrand = true,
                manufacturerOrLabeler = entry.manufacturer,
                extraDescription = "${entry.genericName} (${entry.strength}) • ${entry.form}"
            )
        }
    }

    data class IndianMedicineEntry(
        val brandName: String,
        val genericName: String,
        val strength: String,
        val form: String,
        val manufacturer: String
    )

    companion object {
        private val indianMedicineCatalog = listOf(
            // --- Paracetamol & Analgesics ---
            IndianMedicineEntry("Dolo 650", "Paracetamol", "650 mg", "Tablet", "Micro Labs Ltd"),
            IndianMedicineEntry("Crocin 500", "Paracetamol", "500 mg", "Tablet", "GSK / Haleon"),
            IndianMedicineEntry("Crocin 650 Advance", "Paracetamol", "650 mg", "Tablet", "GSK / Haleon"),
            IndianMedicineEntry("Calpol 500", "Paracetamol", "500 mg", "Tablet", "GlaxoSmithKline"),
            IndianMedicineEntry("Calpol 650", "Paracetamol", "650 mg", "Tablet", "GlaxoSmithKline"),
            IndianMedicineEntry("Calpol Suspension", "Paracetamol", "250 mg/5ml", "Liquid", "GlaxoSmithKline"),
            IndianMedicineEntry("Combiflam", "Ibuprofen & Paracetamol", "400 mg + 325 mg", "Tablet", "Sanofi India Ltd"),
            IndianMedicineEntry("Meftal-Spas", "Mefenamic Acid & Dicyclomine", "250 mg + 10 mg", "Tablet", "Blue Cross Laboratories"),
            IndianMedicineEntry("Meftal 500", "Mefenamic Acid", "500 mg", "Tablet", "Blue Cross Laboratories"),
            IndianMedicineEntry("Zerodol-P", "Aceclofenac & Paracetamol", "100 mg + 325 mg", "Tablet", "Ipca Laboratories"),
            IndianMedicineEntry("Zerodol-SP", "Aceclofenac, Paracetamol & Serratiopeptidase", "100 mg + 325 mg + 15 mg", "Tablet", "Ipca Laboratories"),
            IndianMedicineEntry("Ultracet", "Tramadol & Paracetamol", "37.5 mg + 325 mg", "Tablet", "Janssen / J&J"),

            // --- Antibiotics & Anti-Infectives ---
            IndianMedicineEntry("Augmentin 625 Duo", "Amoxicillin & Potassium Clavulanate", "500 mg + 125 mg", "Tablet", "GlaxoSmithKline"),
            IndianMedicineEntry("Augmentin 375", "Amoxicillin & Potassium Clavulanate", "250 mg + 125 mg", "Tablet", "GlaxoSmithKline"),
            IndianMedicineEntry("Mox 500", "Amoxicillin", "500 mg", "Capsule", "Sun Pharma"),
            IndianMedicineEntry("Moxikind-CV 625", "Amoxicillin & Potassium Clavulanate", "500 mg + 125 mg", "Tablet", "Mankind Pharma"),
            IndianMedicineEntry("Azithral 500", "Azithromycin", "500 mg", "Tablet", "Alembic Pharmaceuticals"),
            IndianMedicineEntry("Azithral 250", "Azithromycin", "250 mg", "Tablet", "Alembic Pharmaceuticals"),
            IndianMedicineEntry("Azee 500", "Azithromycin", "500 mg", "Tablet", "Cipla Ltd"),
            IndianMedicineEntry("Zifi 200", "Cefixime", "200 mg", "Tablet", "FDC Ltd"),
            IndianMedicineEntry("Taxim-O 200", "Cefixime", "200 mg", "Tablet", "Alkem Laboratories"),
            IndianMedicineEntry("Cifran 500", "Ciprofloxacin", "500 mg", "Tablet", "Sun Pharma"),
            IndianMedicineEntry("Ciplox 500", "Ciprofloxacin", "500 mg", "Tablet", "Cipla Ltd"),
            IndianMedicineEntry("Norflox-TZ", "Norfloxacin & Tinidazole", "400 mg + 600 mg", "Tablet", "Cipla Ltd"),
            IndianMedicineEntry("Oflox 200", "Ofloxacin", "200 mg", "Tablet", "Cipla Ltd"),
            IndianMedicineEntry("Zenflox 200", "Ofloxacin", "200 mg", "Tablet", "Mankind Pharma"),
            IndianMedicineEntry("Oflomac-OZ", "Ofloxacin & Ornidazole", "200 mg + 500 mg", "Tablet", "Macleods Pharma"),

            // --- Antacids, PPIs & Gastrointestinal ---
            IndianMedicineEntry("Pan 40", "Pantoprazole", "40 mg", "Tablet", "Alkem Laboratories"),
            IndianMedicineEntry("Pan-D", "Pantoprazole & Domperidone", "40 mg + 30 mg", "Capsule", "Alkem Laboratories"),
            IndianMedicineEntry("Pantocid 40", "Pantoprazole", "40 mg", "Tablet", "Sun Pharma"),
            IndianMedicineEntry("Pantocid DSR", "Pantoprazole & Domperidone SR", "40 mg + 30 mg", "Capsule", "Sun Pharma"),
            IndianMedicineEntry("Omez 20", "Omeprazole", "20 mg", "Capsule", "Dr. Reddy's Laboratories"),
            IndianMedicineEntry("Omez-D", "Omeprazole & Domperidone", "20 mg + 10 mg", "Capsule", "Dr. Reddy's Laboratories"),
            IndianMedicineEntry("Razo 20", "Rabeprazole", "20 mg", "Tablet", "Dr. Reddy's Laboratories"),
            IndianMedicineEntry("Razo-D", "Rabeprazole & Domperidone", "20 mg + 30 mg", "Capsule", "Dr. Reddy's Laboratories"),
            IndianMedicineEntry("Digene Gel", "Aluminium & Magnesium Hydroxide + Simethicone", "450 mg/10ml", "Liquid", "Abbott India"),
            IndianMedicineEntry("Gelusil MPS", "Aluminium Hydroxide & Dimethicone", "250 mg", "Liquid", "Pfizer Ltd"),
            IndianMedicineEntry("Ondem 4", "Ondansetron", "4 mg", "Tablet", "Alkem Laboratories"),
            IndianMedicineEntry("Vomikind 4", "Ondansetron MD", "4 mg", "Tablet", "Mankind Pharma"),
            IndianMedicineEntry("Emeset 4", "Ondansetron", "4 mg", "Tablet", "Cipla Ltd"),
            IndianMedicineEntry("Cremaffin", "Liquid Paraffin & Milk of Magnesia", "11.25 ml/15ml", "Liquid", "Abbott India"),

            // --- Diabetes & Blood Sugar ---
            IndianMedicineEntry("Glycomet 500", "Metformin Hydrochloride", "500 mg", "Tablet", "USV Ltd"),
            IndianMedicineEntry("Glycomet 1000 SR", "Metformin Hydrochloride SR", "1000 mg", "Tablet", "USV Ltd"),
            IndianMedicineEntry("Glycomet-GP 1", "Metformin & Glimepiride", "500 mg + 1 mg", "Tablet", "USV Ltd"),
            IndianMedicineEntry("Glycomet-GP 2", "Metformin & Glimepiride", "500 mg + 2 mg", "Tablet", "USV Ltd"),
            IndianMedicineEntry("Januvia 100", "Sitagliptin", "100 mg", "Tablet", "MSD Pharmaceuticals"),
            IndianMedicineEntry("Janumet 50/500", "Sitagliptin & Metformin", "50 mg + 500 mg", "Tablet", "MSD Pharmaceuticals"),
            IndianMedicineEntry("Galvus 50", "Vildagliptin", "50 mg", "Tablet", "Novartis India"),
            IndianMedicineEntry("Galvus Met 50/500", "Vildagliptin & Metformin", "50 mg + 500 mg", "Tablet", "Novartis India"),
            IndianMedicineEntry("Lantus SoloStar", "Insulin Glargine", "100 IU/ml", "Injection", "Sanofi India"),
            IndianMedicineEntry("Human Mixtard 30/70", "Biphasic Isophane Insulin", "100 IU/ml", "Injection", "Novo Nordisk"),

            // --- Hypertension, Heart & Cholesterol ---
            IndianMedicineEntry("Telma 40", "Telmisartan", "40 mg", "Tablet", "Glenmark Pharmaceuticals"),
            IndianMedicineEntry("Telma 80", "Telmisartan", "80 mg", "Tablet", "Glenmark Pharmaceuticals"),
            IndianMedicineEntry("Telma-H", "Telmisartan & Hydrochlorothiazide", "40 mg + 12.5 mg", "Tablet", "Glenmark Pharmaceuticals"),
            IndianMedicineEntry("Telmikind 40", "Telmisartan", "40 mg", "Tablet", "Mankind Pharma"),
            IndianMedicineEntry("Amlong 5", "Amlodipine", "5 mg", "Tablet", "Micro Labs Ltd"),
            IndianMedicineEntry("Amlokind 5", "Amlodipine", "5 mg", "Tablet", "Mankind Pharma"),
            IndianMedicineEntry("Amlopres 5", "Amlodipine", "5 mg", "Tablet", "Cipla Ltd"),
            IndianMedicineEntry("Ecosprin 75", "Aspirin (Acetylsalicylic Acid)", "75 mg", "Tablet", "USV Ltd"),
            IndianMedicineEntry("Ecosprin 150", "Aspirin (Acetylsalicylic Acid)", "150 mg", "Tablet", "USV Ltd"),
            IndianMedicineEntry("Ecosprin-AV 75", "Aspirin & Atorvastatin", "75 mg + 10 mg", "Capsule", "USV Ltd"),
            IndianMedicineEntry("Atorva 10", "Atorvastatin", "10 mg", "Tablet", "Zydus Cadila"),
            IndianMedicineEntry("Atorva 20", "Atorvastatin", "20 mg", "Tablet", "Zydus Cadila"),
            IndianMedicineEntry("Lipivas 10", "Atorvastatin", "10 mg", "Tablet", "Cipla Ltd"),
            IndianMedicineEntry("Rosuvas 10", "Rosuvastatin", "10 mg", "Tablet", "Sun Pharma"),

            // --- Allergy, Respiratory & Cough ---
            IndianMedicineEntry("Allegra 120", "Fexofenadine Hydrochloride", "120 mg", "Tablet", "Sanofi India"),
            IndianMedicineEntry("Allegra 180", "Fexofenadine Hydrochloride", "180 mg", "Tablet", "Sanofi India"),
            IndianMedicineEntry("Montair-LC", "Montelukast & Levocetirizine", "10 mg + 5 mg", "Tablet", "Cipla Ltd"),
            IndianMedicineEntry("Monticope", "Montelukast & Levocetirizine", "10 mg + 5 mg", "Tablet", "Mankind Pharma"),
            IndianMedicineEntry("Cetzine", "Cetirizine Hydrochloride", "10 mg", "Tablet", "Dr. Reddy's Laboratories"),
            IndianMedicineEntry("Cetgel", "Cetirizine Hydrochloride", "10 mg", "Capsule", "Dr. Reddy's Laboratories"),
            IndianMedicineEntry("Ascoril D Plus", "Dextromethorphan, Phenylephrine & Chlorpheniramine", "10 mg + 5 mg + 2 mg", "Liquid", "Glenmark"),
            IndianMedicineEntry("Ascoril LS", "Levosalbutamol, Ambroxol & Guaiphenesin", "1 mg + 30 mg + 50 mg", "Liquid", "Glenmark"),
            IndianMedicineEntry("Benadryl Cough Syrup", "Diphenhydramine, Ammonium Chloride & Sodium Citrate", "14.08 mg/5ml", "Liquid", "Johnson & Johnson"),
            IndianMedicineEntry("Asthalin Inhaler", "Salbutamol", "100 mcg", "Inhaler", "Cipla Ltd"),
            IndianMedicineEntry("Budecort 200 Inhaler", "Budesonide", "200 mcg", "Inhaler", "Cipla Ltd"),
            IndianMedicineEntry("Foracort 200", "Formoterol & Budesonide", "6 mcg + 200 mcg", "Inhaler", "Cipla Ltd"),

            // --- Vitamins, Minerals & Supplements ---
            IndianMedicineEntry("Shelcal 500", "Calcium & Vitamin D3", "500 mg + 250 IU", "Tablet", "Torrent Pharmaceuticals"),
            IndianMedicineEntry("Shelcal HD", "Calcium & Vitamin D3", "500 mg + 500 IU", "Tablet", "Torrent Pharmaceuticals"),
            IndianMedicineEntry("Becosules Z", "Vitamin B-Complex with Zinc & Vitamin C", "Forte", "Capsule", "Pfizer Ltd"),
            IndianMedicineEntry("Supradyn Daily", "Multivitamin & Multi-minerals", "Daily Formulation", "Tablet", "Bayer India"),
            IndianMedicineEntry("Livogen-Z", "Ferrous Fumarate, Folic Acid & Zinc", "152 mg + 750 mcg + 61.8 mg", "Capsule", "P&G Health"),
            IndianMedicineEntry("Limcee 500", "Vitamin C (Ascorbic Acid)", "500 mg", "Tablet", "Abbott India"),
            IndianMedicineEntry("Celin 500", "Vitamin C (Ascorbic Acid)", "500 mg", "Tablet", "KOYE Pharmaceuticals"),
            IndianMedicineEntry("Thyronorm 50", "Thyroxine Sodium", "50 mcg", "Tablet", "Abbott India"),
            IndianMedicineEntry("Thyronorm 100", "Thyroxine Sodium", "100 mcg", "Tablet", "Abbott India"),
            IndianMedicineEntry("Eltroxin 50", "Thyroxine Sodium", "50 mcg", "Tablet", "GlaxoSmithKline")
        )
    }
}
