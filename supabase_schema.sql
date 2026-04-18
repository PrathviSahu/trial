-- Supabase schema for Face Attendance System

-- Create students table
CREATE TABLE IF NOT EXISTS public.students (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    ien_number VARCHAR(255) UNIQUE,
    roll_number VARCHAR(255),
    email VARCHAR(255),
    phone_number VARCHAR(255),
    department VARCHAR(255),
    branch VARCHAR(255),
    year INTEGER,
    semester INTEGER,
    face_descriptor TEXT,
    face_enrolled BOOLEAN DEFAULT FALSE,
    section VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

-- Create attendance table
CREATE TABLE IF NOT EXISTS public.attendance (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    confidence DOUBLE PRECISION,
    method VARCHAR(255),
    marked_by VARCHAR(255),
    subject VARCHAR(255),
    class_id VARCHAR(255),
    status VARCHAR(255)
);

-- Create guardians table
CREATE TABLE IF NOT EXISTS public.guardians (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    relationship VARCHAR(255),
    student_id BIGINT REFERENCES public.students(id) ON DELETE SET NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    last_login TIMESTAMP WITHOUT TIME ZONE
);

-- Create leave_requests table
CREATE TABLE IF NOT EXISTS public.leave_requests (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    leave_type VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    approved_by VARCHAR(255),
    admin_remarks VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

-- Create timetable_slots table
CREATE TABLE IF NOT EXISTS public.timetable_slots (
    id BIGSERIAL PRIMARY KEY,
    department VARCHAR(255) NOT NULL,
    year INTEGER NOT NULL,
    semester INTEGER NOT NULL,
    section VARCHAR(255),
    day_of_week VARCHAR(255) NOT NULL,
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    subject_code VARCHAR(255) NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    faculty VARCHAR(255),
    classroom VARCHAR(255),
    type VARCHAR(255) NOT NULL,
    batch VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

-- Triggers to auto-update the updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_students_updated_at BEFORE UPDATE ON public.students FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
CREATE TRIGGER update_leave_requests_updated_at BEFORE UPDATE ON public.leave_requests FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
CREATE TRIGGER update_timetable_slots_updated_at BEFORE UPDATE ON public.timetable_slots FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
