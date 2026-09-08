DROP DATABASE IF EXISTS u_medical;
CREATE DATABASE u_medical CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE u_medical;

-- ---------------------------------------------------------------------
-- Tabla: roles
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE COMMENT 'ROLE_ADMIN, ROLE_DOCTOR, ROLE_PATIENT, ROLE_SECRETARY'
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla: users
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL COMMENT 'Hash BCrypt',
    first_name VARCHAR(80)  NOT NULL,
    last_name  VARCHAR(80)  NOT NULL,
    phone      VARCHAR(20),
    role_id    BIGINT NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE INDEX idx_users_role ON users(role_id);

-- ---------------------------------------------------------------------
-- Tabla: specialties
-- ---------------------------------------------------------------------
CREATE TABLE specialties (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla: doctors
-- ---------------------------------------------------------------------
CREATE TABLE doctors (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL UNIQUE,
    specialty_id   BIGINT NOT NULL,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    biography      TEXT,
    CONSTRAINT fk_doctors_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_doctors_specialty FOREIGN KEY (specialty_id) REFERENCES specialties(id)
) ENGINE=InnoDB;

CREATE INDEX idx_doctors_specialty ON doctors(specialty_id);

-- ---------------------------------------------------------------------
-- Tabla: patients
-- ---------------------------------------------------------------------
CREATE TABLE patients (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL UNIQUE,
    birth_date DATE,
    address    VARCHAR(255),
    blood_type VARCHAR(5),
    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla: schedules
-- ---------------------------------------------------------------------
CREATE TABLE schedules (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id             BIGINT NOT NULL,
    day_of_week           INT NOT NULL COMMENT '1=Lunes ... 7=Domingo',
    start_time            TIME NOT NULL,
    end_time              TIME NOT NULL,
    slot_duration_minutes INT NOT NULL DEFAULT 30,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_schedules_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    CONSTRAINT chk_schedule_time CHECK (end_time > start_time),
    CONSTRAINT chk_day_of_week CHECK (day_of_week BETWEEN 1 AND 7)
) ENGINE=InnoDB;

CREATE INDEX idx_schedules_doctor_day ON schedules(doctor_id, day_of_week);

-- ---------------------------------------------------------------------
-- Tabla: appointments
-- Registro de citas medicas
-- ---------------------------------------------------------------------
CREATE TABLE appointments (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id       BIGINT NOT NULL,
    doctor_id        BIGINT NOT NULL,
    schedule_id      BIGINT,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' COMMENT 'PENDIENTE, COMPLETADA, CANCELADA',
    reason           VARCHAR(255),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    CONSTRAINT fk_appointments_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id),
    CONSTRAINT uq_doctor_datetime UNIQUE (doctor_id, appointment_date, appointment_time),
    CONSTRAINT chk_status CHECK (status IN ('PENDIENTE', 'COMPLETADA', 'CANCELADA'))
) ENGINE=InnoDB;

CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_doctor_date ON appointments(doctor_id, appointment_date);

-- ---------------------------------------------------------------------
-- Tabla: documents
-- Documentos clinicos generados a partir de una cita (diagnostico, receta).
-- Separada de appointments para poder consultar documentos sin necesidad
-- de atravesar las relaciones de citas/pacientes/medicos.
-- ---------------------------------------------------------------------
CREATE TABLE documents (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    type           VARCHAR(20) NOT NULL COMMENT 'DIAGNOSTICO, RECETA',
    content        TEXT NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT chk_document_type CHECK (type IN ('DIAGNOSTICO', 'RECETA'))
) ENGINE=InnoDB;

CREATE INDEX idx_documents_appointment ON documents(appointment_id);

-- =====================================================================
-- Datos semilla para pruebas
-- =====================================================================
INSERT INTO roles (name) VALUES
    ('ROLE_ADMIN'), ('ROLE_DOCTOR'), ('ROLE_PATIENT'), ('ROLE_SECRETARY');

INSERT INTO specialties (name, description) VALUES
    ('Medicina General', 'Consulta general y chequeos de rutina'),
    ('Pediatria', 'Atencion medica infantil'),
    ('Cardiologia', 'Enfermedades del corazon y sistema circulatorio'),
    ('Dermatologia', 'Enfermedades de la piel');

-- Password en texto plano para TODOS los usuarios semilla: "password"
INSERT INTO users (username, email, password, first_name, last_name, phone, role_id, active) VALUES
    ('admin', 'admin@clinica.com', '$2a$10$J5BLET8RVM0.ciY2xxBnNOAGf3t68nzcYnHF96/wyIaZ.Jw79wfJi', 'Ana', 'Administradora', '5555-0001', 1, TRUE),
    ('dr.perez', 'dr.perez@clinica.com', '$2a$10$J5BLET8RVM0.ciY2xxBnNOAGf3t68nzcYnHF96/wyIaZ.Jw79wfJi', 'Carlos', 'Perez', '5555-0002', 2, TRUE),
    ('dra.lopez', 'dra.lopez@clinica.com', '$2a$10$J5BLET8RVM0.ciY2xxBnNOAGf3t68nzcYnHF96/wyIaZ.Jw79wfJi', 'Maria', 'Lopez', '5555-0003', 2, TRUE),
    ('paciente1', 'paciente1@correo.com', '$2a$10$J5BLET8RVM0.ciY2xxBnNOAGf3t68nzcYnHF96/wyIaZ.Jw79wfJi', 'Jose', 'Ramirez', '5555-0004', 3, TRUE);

INSERT INTO doctors (user_id, specialty_id, license_number, biography) VALUES
    (2, 1, 'MED-1001', 'Medico general con 10 anios de experiencia.'),
    (3, 3, 'MED-1002', 'Cardiologa, especialista en electrocardiografia.');

INSERT INTO patients (user_id, birth_date, address, blood_type) VALUES
    (4, '1995-04-12', 'Zona 10, Ciudad de Guatemala', 'O+');

INSERT INTO schedules (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes) VALUES
    (1, 1, '08:00:00', '12:00:00', 30),
    (1, 3, '08:00:00', '12:00:00', 30),
    (2, 2, '14:00:00', '18:00:00', 30);
