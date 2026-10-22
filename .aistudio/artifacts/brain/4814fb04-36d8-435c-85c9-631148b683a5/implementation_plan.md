# MedTrack — Modern Minimalist Medication & Health Tracker

MedTrack is an ultra-clean, minimal, and modern Android medication tracking application designed with a soft medical aesthetic, soothing teal accents, and zero visual clutter. It empowers users to seamlessly manage daily doses, track inventory with refill warnings, log health vitals and symptoms, rotate injection sites, and coordinate care appointments.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architecture and feature parameters incorporate your responses to our initial discovery questions:

- **Confirmed Decision 1 (Storage Strategy)**: Full on-device local storage powered by Android Jetpack Room with reactive Kotlin Flows, pre-populated with realistic starter medications (e.g., Amoxicillin, Metformin, Lisinopril, Vitamin D3, and an injectable GLP-1/Insulin for site rotation) so the app is immediately useful on first launch.
- **Confirmed Decision 2 (Navigation Architecture)**: 5-destination Material 3 Navigation Bar spanning:
  1. **Today / Home**: Schedule timeline, large tactile "Take" buttons, daily progress circle (e.g., "4 of 6 taken"), streak counter, and quick add pill action.
  2. **Cabinet**: Medication inventory cards, pill counters, visual stock gauge, amber low-stock alerts, and refill triggers.
  3. **Reminders & Alarms**: Scheduled dose reminders with Take / Snooze / Skip actions, smart refill alerts, and interactive injection site rotation map.
  4. **Health Diary**: Log symptoms, mood, pain scale (1–10), vitals (blood pressure, blood glucose, weight), interactive trend charts, and monthly report summary.
  5. **Appointments & Profiles**: Doctor consultation calendar, multi-profile family support (switch between family members), and adherence achievement streak badges.
- **Confirmed Decision 3 (Medication Addition & Dual Scanning)**: Comprehensive Add Medication flow featuring:
  - Prescribed dosage, timing (Morning, Noon, Evening, Bedtime), frequency (times/day, days/week), overall course duration (e.g., 10 days, 30 days, or ongoing), stock count, and refill threshold.
  - **Barcode Scanner**: Camera view & instant barcode lookup against an embedded pharmaceutical drug catalog.
  - **Medicine Pack Front Scanner**: Visual pack recognition simulating front-of-box analysis to auto-extract and autofill drug name, dosage strength, form (tablets, capsules, injections), and default timing.

---

### 1. Overview & Core Concept

- **What It Does**: MedTrack helps individuals and caregivers adhere to complex medication routines without anxiety. Users effortlessly record taken doses, receive timely reminder alerts, monitor running pill quantities with automated low-stock warnings, track side-effects or health metrics, and maintain a historical calendar for doctor visits.
- **Target Audience / Persona**: Patients managing daily chronic or acute prescriptions, caregivers coordinating family member medications, and individuals monitoring vitamins, injection site rotations, and health indicators.
- **Key Value**: Replaces anxiety-inducing alarms and messy pill organizers with a calm, tactile, and dependable digital companion that ensures no dose or refill is missed.

---

### 2. User Experience & Visual Design

#### Visual Identity & Theme
- **Aesthetic Direction**: Clinical clarity meets boutique wellness. Generous white space, calm teal accents, subtle rounded cards (16–24dp corners), crisp typography, and high-contrast accessibility.
- **Color Palette (Strictly Applied)**:
  - **Primary**: `#0D9488` (Deep medical teal)
  - **Primary Soft / Container**: `#CCFBF1` (Mint ice) and `#F0FDFA` (Soft clinical tinted canvas)
  - **Surface / Cards**: `#FFFFFF` with ultra-light border `#E2E8F0` and subtle elevation
  - **Success**: `#10B981` (Vibrant emerald green for taken doses and achievements)
  - **Warning / Low Stock**: `#F59E0B` (Amber warning for pills running out)
  - **Text Primary**: `#134E4A` (Deep teal-charcoal for high-contrast legibility)
  - **Text Secondary**: `#5F8A86` (Muted teal-slate for captions and helper labels)
- **App Logo**:
  - Two-tone teal & white capsule pill logo with "MedTrack" displayed cleanly on the capsule, present in the Top App Bar and splash experience.

#### Key Screens & User Flows

1. **Today / Home Screen**:
   - Header with MedTrack capsule branding, current date, and user/family avatar switcher.
   - **Adherence Card**: Circular progress indicator ("4 of 6 taken today - 67%"), current daily streak counter with flame badge.
   - **Time Filter Chips**: All, Morning, Afternoon, Evening, Bedtime.
   - **Medication Schedule Cards**: Medicine name, dosage, instructions ("With food"), scheduled time, status chip, and a prominent, high-contrast, one-tap "Take" button (with quick "Snooze" / "Skip" popup menu).
   - Floating Quick Action Button: "Add Medication" with gentle teal glow.

2. **Medication Cabinet / Inventory Screen**:
   - Grid or card list showing all active medications and emergency/as-needed (PRN) treatments.
   - Remaining pill count counter (e.g., "14 pills left • 7 days remaining").
   - Dynamic stock level progress bar: transitions from green to amber when below refill threshold (e.g., <= 5 pills).
   - Prominent Amber "Low Stock — Refill Soon" badge with quick "Log Refill" and "Order / Note" buttons.

3. **Reminders & Alarms + Injection Site Rotation**:
   - Time-ordered reminder feed with interactive action controls (Confirm Taken, Snooze 15m, Skip).
   - **Smart Refill Alarms**: Automated banner alerting upcoming pharmacy pickups.
   - **Injection Site Rotation Tracker**: Interactive anatomical diagram (Abdomen Left/Right, Thigh Left/Right, Upper Arm) highlighting last used site and recommending next rotation zone to avoid tissue hypertrophy.

4. **Health Diary & Vitals Tracking**:
   - Quick daily log sheet: Mood (emoji scale), Pain Scale (1–10 slider), Symptoms tag selector, Blood Pressure (sys/dia), Blood Sugar (mg/dL), and Weight (kg/lbs).
   - Clean trend charts (Canvas-drawn smooth bezier graphs) showing 7-day and 30-day vitals.
   - Monthly adherence calendar grid with color-coded daily completion dots.
   - "Export Health Report" dialog generating a formatted summary ready for medical consultations.

5. **Appointments & Family Profile Screen**:
   - Doctor visits organizer with doctor name, specialty, clinic location, date/time, and prepared questions list.
   - Family member profile switcher (e.g., "Self", "Dad", "Child") allowing separate regimens.
   - Streak awards and adherence milestones.

6. **Add Medication Modal with Dual Scanner**:
   - Input wizard: Drug name, strength, pill shape/color icon picker, dosage count, frequency per day & per week, schedule times, course duration, and initial inventory.
   - **Barcode Scan Mode**: Viewfinder simulating UPC/NDC scanning that detects pharmaceutical codes and presets details.
   - **Front-of-Pack Vision Scanner**: Camera viewfinder simulating packaging recognition that extracts text (Brand, Strength e.g., "500mg", Form e.g., "Capsules") directly into form fields.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Jetpack Room SQLite Architecture vs Remote Backend**:
  - *Chosen Approach*: Fully local Jetpack Room database with pre-populated starter records.
  - *Why*: Medication adherence apps must be 100% reliable offline without login friction or network lag. Room provides immediate reactivity via Kotlin `Flow` and rock-solid privacy for sensitive medical data.
- **Decision 2: Interactive Dual Scanner Simulation & Camera Support**:
  - *Chosen Approach*: Interactive camera preview overlay with simulated high-accuracy optical detection and instant preset sample pills, paired with full manual override.
  - *Why*: Allows instant testing and rich interaction in all Android emulator and web streaming environments while providing realistic pharmaceutical OCR and barcode autofill.
- **Decision 3: Single-Activity Compose M3 with Edge-to-Edge**:
  - *Chosen Approach*: Full edge-to-edge support with `enableEdgeToEdge()`, window insets padding for notch and navigation bars, and fluid animated transitions.
  - *Why*: Delivers an iPhone-grade minimal, polished aesthetic compliant with modern Android Material 3 design guidelines.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
├─────────────────┬──────────────────┬─────────────────┬────────────────┤
│   HomeScreen    │  CabinetScreen   │ RemindersScreen │  DiaryScreen   │
│ (Today Schedule,│(Inventory Cards, │(Alarms, Snooze, │(Vitals, Charts,│
│  Progress Ring) │ Low Stock Alert) │ Injection Map)  │ Monthly Export)│
├─────────────────┴──────────────────┴─────────────────┴────────────────┤
│                       Appointments & Profiles                          │
│         (Doctor Visits, Family Switcher, Streak Achievements)         │
├────────────────────────────────────────────────────────────────────────┤
│                       Add Medication Wizard                            │
│     (Form Fields + Barcode Scanner + Medicine Pack Vision Scanner)     │
├────────────────────────────────────────────────────────────────────────┤
│                   MedTrackViewModel (StateFlow UI State)               │
├────────────────────────────────────────────────────────────────────────┤
│                        MedTrackRepository                              │
├────────────────────────────────────────────────────────────────────────┤
│                 Android Jetpack Room Local Database                    │
│   ├── MedicationEntity    (Name, Dosage, Timing, Stock, Frequency)    │
│   ├── DoseLogEntity       (MedicationId, Timestamp, Status, Notes)    │
│   ├── HealthMetricEntity  (Vitals, Pain, Mood, Symptoms, Timestamp)   │
│   ├── AppointmentEntity   (Doctor, Specialty, DateTime, Notes)        │
│   └── InjectionSiteEntity (BodyPart, Side, Timestamp)                 │
└────────────────────────────────────────────────────────────────────────┘
```

#### Core Database Entities
- `Medication`: `id`, `name`, `dosage`, `type` (Pill, Capsule, Liquid, Injection), `colorHex`, `timesOfDay` (List<String>), `frequencyDaysPerWeek`, `courseDurationDays`, `stockRemaining`, `refillThreshold`, `instructions`, `profileId`.
- `DoseLog`: `id`, `medicationId`, `scheduledTime`, `loggedTime`, `status` (`TAKEN`, `SKIPPED`, `SNOOZED`), `notes`.
- `HealthEntry`: `id`, `timestamp`, `mood`, `painLevel`, `systolic`, `diastolic`, `bloodSugar`, `weight`, `notes`.
- `Appointment`: `id`, `doctorName`, `specialty`, `dateTime`, `location`, `notes`.
- `InjectionRecord`: `id`, `siteName` (e.g., "Abdomen - Right Lower"), `timestamp`.
