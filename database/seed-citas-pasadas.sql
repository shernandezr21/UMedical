-- =====================================================================
-- U-Medical: citas PASADAS de prueba (para el historial de la app)
--
-- La API no deja crear citas en el pasado a proposito (reserva con minimo
-- 60 min de anticipacion), por eso estas se insertan directo en la base.
--
-- Usa a paciente1 y a dr.perez del schema.sql.
-- =====================================================================
USE u_medical;

SET @patient_id := (SELECT p.id FROM patients p JOIN users u ON u.id = p.user_id WHERE u.username = 'paciente1');
SET @doctor_id  := (SELECT d.id FROM doctors  d JOIN users u ON u.id = d.user_id WHERE u.username = 'dr.perez');

-- Limpieza de una corrida anterior (los documentos se borran en cascada)
DELETE FROM appointments
 WHERE patient_id = @patient_id
   AND doctor_id  = @doctor_id
   AND reason IN ('Control de rutina', 'Dolor de garganta y fiebre', 'Revision de resultados (sin atender)');

-- El Dr. Perez atiende lunes y miercoles de 08:00 a 12:00.

SET @lunes_pasado := CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY;   -- lunes de la semana pasada
SET @lunes_hace_3 := @lunes_pasado - INTERVAL 14 DAY;                      -- lunes de hace 3 semanas

-- ---------------------------------------------------------------------
-- Cita pasada 1: COMPLETADA, con diagnostico y receta
-- ---------------------------------------------------------------------
INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, status, reason, created_at)
VALUES (@patient_id, @doctor_id, @lunes_hace_3, '09:30:00', 'COMPLETADA', 'Control de rutina',
        @lunes_hace_3 - INTERVAL 5 DAY);
SET @cita1 := LAST_INSERT_ID();

INSERT INTO documents (appointment_id, type, content, created_at) VALUES
 (@cita1, 'DIAGNOSTICO', 'Paciente en buen estado general. Presion arterial y peso dentro de parametros normales.', @lunes_hace_3 + INTERVAL 10 HOUR),
 (@cita1, 'RECETA',      'Multivitaminico: 1 tableta diaria por 30 dias. Control en 6 meses.',                       @lunes_hace_3 + INTERVAL 10 HOUR);

-- ---------------------------------------------------------------------
-- Cita pasada 2: COMPLETADA, con diagnostico y receta
-- ---------------------------------------------------------------------
INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, status, reason, created_at)
VALUES (@patient_id, @doctor_id, @lunes_pasado, '11:00:00', 'COMPLETADA', 'Dolor de garganta y fiebre',
        @lunes_pasado - INTERVAL 2 DAY);
SET @cita2 := LAST_INSERT_ID();

INSERT INTO documents (appointment_id, type, content, created_at) VALUES
 (@cita2, 'DIAGNOSTICO', 'Faringitis aguda de origen viral. Reposo relativo e hidratacion abundante por 5 dias.', @lunes_pasado + INTERVAL 11 HOUR),
 (@cita2, 'RECETA',      'Paracetamol 500 mg cada 8 horas por 3 dias. Gargaras con agua tibia y sal 3 veces al dia.', @lunes_pasado + INTERVAL 11 HOUR);

-- ---------------------------------------------------------------------
-- OPCIONAL: una cita pasada que sigue PENDIENTE (sin atender)
-- Sirve para probar, como doctor, el boton de registrar diagnostico: la API
-- solo permite documentar citas de hoy o anteriores.
-- ---------------------------------------------------------------------
INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, status, reason, created_at)
VALUES (@patient_id, @doctor_id, @lunes_pasado + INTERVAL 2 DAY, '08:30:00', 'PENDIENTE', 'Revision de resultados (sin atender)',
        @lunes_pasado - INTERVAL 1 DAY);

-- ---------------------------------------------------------------------
-- Verificacion: 2 citas COMPLETADA con 2 documentos cada una
-- ---------------------------------------------------------------------
SELECT a.id, a.appointment_date, a.appointment_time, a.status, a.reason,
       (SELECT COUNT(*) FROM documents d WHERE d.appointment_id = a.id) AS documentos
  FROM appointments a
 WHERE a.patient_id = @patient_id
 ORDER BY a.appointment_date DESC, a.appointment_time DESC;

