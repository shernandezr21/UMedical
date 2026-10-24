-- Lleva el esquema al estado actual de la aplicacion (bloques 5 y 7), sin importar si database/schema.sql
-- ya lo trae o no: es idempotente (se puede correr con o sin esos cambios en schema.sql).
USE u_medical;

-- Bloque 5: dias no disponibles del medico
CREATE TABLE IF NOT EXISTS doctor_time_off (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id   BIGINT       NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    reason      VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_time_off_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    CONSTRAINT chk_time_off_dates CHECK (end_date >= start_date),
    INDEX idx_time_off_doctor_dates (doctor_id, start_date, end_date)
);

-- Bloque 7: bitacora de auditoria
CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    occurred_at     DATETIME     NOT NULL,
    actor_user_id   BIGINT       NULL,
    actor_username  VARCHAR(50)  NOT NULL,
    actor_role      VARCHAR(30)  NOT NULL,
    action          VARCHAR(40)  NOT NULL,
    entity_type     VARCHAR(30)  NULL,
    entity_id       BIGINT       NULL,
    patient_id      BIGINT       NULL,
    details         VARCHAR(500) NULL,
    ip_address      VARCHAR(45)  NULL,
    INDEX idx_audit_time (occurred_at),
    INDEX idx_audit_actor (actor_username),
    INDEX idx_audit_action (action),
    INDEX idx_audit_patient (patient_id)
);

-- Bloque 7: el estado NO_ASISTIO debe estar permitido. Se reemplaza cualquier CHECK de estados por uno completo.
SET @old_check = (
    SELECT tc.CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS tc
    JOIN information_schema.CHECK_CONSTRAINTS cc
      ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
    WHERE tc.TABLE_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'appointments'
      AND tc.CONSTRAINT_TYPE = 'CHECK'
      AND cc.CHECK_CLAUSE LIKE '%PENDIENTE%'
    LIMIT 1);

SET @drop_sql = IF(@old_check IS NULL, 'SELECT 1', CONCAT('ALTER TABLE appointments DROP CHECK `', @old_check, '`'));
PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;

ALTER TABLE appointments
    ADD CONSTRAINT chk_appointment_status
    CHECK (status IN ('PENDIENTE', 'COMPLETADA', 'CANCELADA', 'NO_ASISTIO'));
