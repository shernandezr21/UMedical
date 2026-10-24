USE u_medical;

INSERT INTO users (username, email, password, first_name, last_name, phone, role_id, active)
SELECT 'secretaria1', 'secretaria1@clinica.com', a.password, 'Laura', 'Mendez', '5555-0005', r.id, TRUE
  FROM users a
  JOIN roles r ON r.name = 'ROLE_SECRETARY'
 WHERE a.username = 'admin'
   AND NOT EXISTS (SELECT 1 FROM users WHERE username = 'secretaria1');

-- Verificacion: debe aparecer 1 fila con rol ROLE_SECRETARY
SELECT u.id, u.username, u.email, r.name AS rol
  FROM users u JOIN roles r ON r.id = u.role_id
 WHERE u.username = 'secretaria1';
