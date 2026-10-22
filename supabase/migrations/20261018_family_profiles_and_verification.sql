-- ==============================================================================
-- Migration: Family Member Profiles with Patient Phone Verification & Per-Profile Medicines
-- Database: Supabase (PostgreSQL with Row Level Security)
-- Execution: Run in local Supabase Studio (SQL Editor) or via Supabase CLI
-- ==============================================================================

-- 1. Create family_profiles table
CREATE TABLE IF NOT EXISTS public.family_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    primary_user_id UUID, -- References auth.users(id) for authenticated caregiver (e.g. Son)
    patient_name TEXT NOT NULL,
    patient_phone TEXT NOT NULL,
    relationship TEXT NOT NULL, -- e.g. "Uncle", "Father", "Mother"
    verification_status TEXT DEFAULT 'pending' CHECK (verification_status IN ('pending', 'verified')),
    verification_code TEXT NOT NULL, -- Random 6-digit confirmation code
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 2. Create medicines table linked to family_profiles(id)
CREATE TABLE IF NOT EXISTS public.medicines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_profile_id UUID NOT NULL REFERENCES public.family_profiles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    dosage TEXT NOT NULL,
    form TEXT DEFAULT 'pill',
    instructions TEXT DEFAULT 'Take as directed',
    times_of_day TEXT DEFAULT '08:00',
    timing_slots TEXT DEFAULT 'Morning',
    stock_remaining INT DEFAULT 30,
    refill_threshold INT DEFAULT 10,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 3. Indexes for fast queries
CREATE INDEX IF NOT EXISTS idx_family_profiles_status ON public.family_profiles(verification_status);
CREATE INDEX IF NOT EXISTS idx_family_profiles_primary_user ON public.family_profiles(primary_user_id);
CREATE INDEX IF NOT EXISTS idx_medicines_profile_id ON public.medicines(family_profile_id);

-- 4. Enable Row Level Security (RLS)
ALTER TABLE public.family_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medicines ENABLE ROW LEVEL SECURITY;

-- 5. Policies for family_profiles
DROP POLICY IF EXISTS "Allow read family_profiles" ON public.family_profiles;
CREATE POLICY "Allow read family_profiles" ON public.family_profiles
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow insert family_profiles" ON public.family_profiles;
CREATE POLICY "Allow insert family_profiles" ON public.family_profiles
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Allow update verification_status" ON public.family_profiles;
CREATE POLICY "Allow update verification_status" ON public.family_profiles
    FOR UPDATE USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow delete family_profiles" ON public.family_profiles;
CREATE POLICY "Allow delete family_profiles" ON public.family_profiles
    FOR DELETE USING (true);

-- 6. Policies for medicines
DROP POLICY IF EXISTS "Allow read medicines" ON public.medicines;
CREATE POLICY "Allow read medicines" ON public.medicines
    FOR SELECT USING (true);

-- ACCESS LOCKOUT POLICY:
-- Disallow inserting or updating medicines for a family profile unless its verification_status is 'verified'
DROP POLICY IF EXISTS "Allow insert medicines only for verified profiles" ON public.medicines;
CREATE POLICY "Allow insert medicines only for verified profiles" ON public.medicines
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.family_profiles
            WHERE id = medicines.family_profile_id
            AND verification_status = 'verified'
        )
    );

DROP POLICY IF EXISTS "Allow update medicines only for verified profiles" ON public.medicines;
CREATE POLICY "Allow update medicines only for verified profiles" ON public.medicines
    FOR UPDATE USING (
        EXISTS (
            SELECT 1 FROM public.family_profiles
            WHERE id = medicines.family_profile_id
            AND verification_status = 'verified'
        )
    );

-- 7. Verification RPC Function
CREATE OR REPLACE FUNCTION public.verify_family_profile(target_profile_id UUID, entered_code TEXT)
RETURNS BOOLEAN AS $$
DECLARE
    matched_id UUID;
BEGIN
    SELECT id INTO matched_id
    FROM public.family_profiles
    WHERE id = target_profile_id
      AND verification_code = entered_code;

    IF matched_id IS NOT NULL THEN
        UPDATE public.family_profiles
        SET verification_status = 'verified',
            updated_at = now()
        WHERE id = target_profile_id;
        RETURN TRUE;
    ELSE
        RETURN FALSE;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 8. Seed Sample Dependent (e.g. Uncle Arthur) with verified status for testing
INSERT INTO public.family_profiles (id, patient_name, patient_phone, relationship, verification_status, verification_code)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Robert Henderson', '+1 (555) 876-5432', 'Father', 'verified', '849201'),
    ('a0000000-0000-0000-0000-000000000002', 'Arthur Miller', '+1 (555) 432-9876', 'Uncle', 'pending', '739104')
ON CONFLICT (id) DO NOTHING;
