# Documentación técnica de la API

API REST de **U-Medical**. Este documento resume el contrato de la API; la referencia interactiva, siempre actualizada con el código, es **Swagger UI**.

| Recurso | URL (entorno local) |
|---|---|
| Swagger UI (probar endpoints) | http://localhost:8080/swagger-ui.html |
| Especificación OpenAPI 3 (JSON) | http://localhost:8080/api-docs |
| URL base de la API | `http://localhost:8080/api/v1` |
| Colección de Postman | [`postman/u-medical-api.postman_collection.json`](../postman/u-medical-api.postman_collection.json) |
| Guía para la app móvil | [`API-MOVIL.md`](API-MOVIL.md) |

Para guardar la especificación junto a la documentación (con el backend en marcha):

```bash
curl http://localhost:8080/api-docs -o docs/openapi.json
```

## 1. Convenciones

- **Formato:** JSON en UTF-8, salvo la descarga del PDF de un documento (`application/pdf`).
- **Fechas y horas:** fecha `yyyy-MM-dd`, hora `HH:mm:ss` (la API también acepta `HH:mm`).
- **Identificadores:** números enteros (`Long`).
- **Estados de cita:** `PENDIENTE`, `COMPLETADA`, `CANCELADA`.
- **Tipos de documento:** `DIAGNOSTICO`, `RECETA`.
- **Días de la semana (horarios):** `1` lunes … `7` domingo.
- **Roles:** `ROLE_ADMIN`, `ROLE_SECRETARY`, `ROLE_DOCTOR`, `ROLE_PATIENT`.

## 2. Autenticación

La API es **stateless**: no usa sesiones de servidor. El flujo es:

1. `POST /auth/login` (o `/auth/register` para pacientes) devuelve un **token JWT**.
2. Cada petición posterior envía el token en el encabezado `Authorization: Bearer <token>`.
3. El token dura **24 horas** y no hay *refresh token*: al vencer, la API responde `401` y hay que iniciar sesión de nuevo.

El token contiene el usuario (`sub`), su `userId` y su `role`. Una **cuenta desactivada** pierde el acceso de inmediato aunque su token no haya vencido.

En Swagger UI, el botón **Authorize** permite pegar el token y probar los endpoints protegidos.

### Matriz de acceso por rol

| Grupo de endpoints | Público | Paciente | Secretaria | Médico | Admin |
|---|:--:|:--:|:--:|:--:|:--:|
| Login y registro | ✅ | | | | |
| Cambiar la propia contraseña | | ✅ | ✅ | ✅ | ✅ |
| Perfil propio del paciente | | ✅ | | | |
| Listado de médicos activos y especialidades | | ✅ | ✅ | ✅ | ✅ |
| Disponibilidad de horarios | | ✅ | ✅ | | ✅ |
| Reservar cita | | ✅ (propia) | ✅ (a nombre de un paciente) | | ✅ (a nombre de un paciente) |
| Historial propio de citas | | ✅ | | | |
| Reagendar cita | | ✅ (propia) | ✅ | | ✅ |
| Cancelar cita | | ✅ (propia) | ✅ | ✅ (propia) | ✅ |
| Agenda general de la clínica | | | ✅ | | ✅ |
| Buscar y registrar pacientes | | | ✅ | | ✅ |
| Agenda propia y registro de documentos | | | | ✅ | |
| Ver documentos de una cita | | ✅ (propia) | | ✅ (propia) | |
| Descargar PDF de un documento | | ✅ (propia) | | ✅ (propia) | |
| Administrar médicos, horarios, especialidades | | | | | ✅ |
| Administrar usuarios y reportes | | | | | ✅ |

## 3. Formato de errores

Todos los errores usan el mismo cuerpo JSON (incluidos los 401 y 403 generados por la seguridad):

```json
{
  "timestamp": "2026-10-16T21:15:03.512",
  "status": 409,
  "error": "Conflict",
  "message": "Ese horario no esta disponible: esta fuera del horario del medico o ya fue reservado",
  "details": null
}
```

En los errores de validación (`400`), `details` lista los campos con problema, por ejemplo `["email: El email no es valido"]`.

| Código | Significado | Ejemplos |
|---|---|---|
| `400` | Datos inválidos | Campo obligatorio vacío, fecha en el pasado, reserva con menos de 60 minutos de anticipación, rango de fechas excesivo |
| `401` | Sin sesión, token inválido o vencido (en el login: credenciales incorrectas) | |
| `403` | Autenticado, pero sin permiso | Rol insuficiente; cita o documento ajeno; cuenta desactivada al iniciar sesión |
| `404` | El recurso no existe | Cita, documento o paciente inexistente; documento sin PDF |
| `409` | Conflicto con el estado actual | Horario no disponible; paciente con otra cita a esa hora; cita no pendiente; correo o usuario repetido |
| `413` | Archivo demasiado grande | PDF mayor a 10 MB |
| `429` | Demasiados intentos | Límite por IP en login y registro; trae el encabezado `Retry-After` |

## 4. Endpoints

Rutas relativas a `/api/v1`. «Rol» indica quién puede invocarlos.

### 4.1 Autenticación y cuenta

| Método | Ruta | Rol | Descripción | Respuestas |
|---|---|---|---|---|
| `POST` | `/auth/login` | Público | Inicia sesión y devuelve el token | `200`, `401`, `403` (cuenta desactivada), `429` |
| `POST` | `/auth/register` | Público | Registra un paciente (rol fijado por el servidor) y devuelve un token | `201`, `400`, `409`, `429` |
| `PUT` | `/users/me/password` | Autenticado | Cambia la propia contraseña | `204`, `400` (actual incorrecta o nueva inválida) |

**Login** — solicitud y respuesta:

```json
{ "username": "paciente1", "password": "password" }
```
```json
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "userId": 4,
  "username": "paciente1",
  "role": "ROLE_PATIENT",
  "fullName": "Jose Ramirez"
}
```

**Registro de paciente** — obligatorios: `username` (4–50; letras, números, `.`, `-`, `_`), `email`, `password` (8–72), `firstName`, `lastName`. Opcionales: `phone`, `birthDate`, `address`, `bloodType` (`A`, `B`, `AB` u `O` seguido de `+` o `-`) y `captchaToken` (solo se exige si el captcha está activado). La respuesta es la misma que la del login.

> La contraseña actual incorrecta en `PUT /users/me/password` responde **400** a propósito (no 401), para que las aplicaciones cliente no cierren la sesión del usuario por error.

### 4.2 Perfil del paciente

| Método | Ruta | Rol | Descripción | Respuestas |
|---|---|---|---|---|
| `GET` | `/patients/me` | Paciente | Perfil propio | `200` |
| `PUT` | `/patients/me` | Paciente | Edita el perfil (nombre, correo, teléfono, nacimiento, dirección, tipo de sangre). El usuario no cambia | `200`, `400`, `409` (correo en uso) |

### 4.3 Pacientes (personal de la clínica)

| Método | Ruta | Rol | Descripción | Respuestas |
|---|---|---|---|---|
| `GET` | `/patients?search=` | Secretaria, Admin | Busca por nombre, usuario, correo o teléfono (máximo 20) | `200` |
| `GET` | `/patients/{id}` | Secretaria, Admin | Datos básicos de un paciente | `200`, `404` |
| `POST` | `/patients` | Secretaria, Admin | Alta en recepción: genera el usuario a partir del correo y una **contraseña temporal** que se devuelve una sola vez | `201`, `400`, `409` |

### 4.4 Médicos y especialidades

| Método | Ruta | Rol | Descripción | Respuestas |
|---|---|---|---|---|
| `GET` | `/doctors?specialtyId=` | Cualquier rol autenticado | Médicos **activos**, filtrables por especialidad | `200` |
| `POST` | `/doctors` | Admin | Alta de médico con usuario, perfil y bloques de horario en **una sola transacción** | `200`, `400`, `404`, `409` |
| `GET` | `/doctors/management` | Admin | Todos los médicos (activos e inactivos) con sus datos completos | `200` |
| `PUT` | `/doctors/{id}` | Admin | Edita datos, especialidad y colegiado | `200`, `404`, `409` (correo o colegiado en uso) |
| `PATCH` | `/doctors/{id}/active` | Admin | Activa o desactiva. Cuerpo: `{ "active": false }` | `200`, `409` (tiene citas pendientes desde hoy) |
| `GET` | `/specialties` | Cualquier rol autenticado | Catálogo de especialidades | `200` |
| `POST` | `/specialties` | Admin | Crea una especialidad | `200`, `400` (nombre repetido, sin distinguir mayúsculas) |
| `PUT` | `/specialties/{id}` | Admin | Edita una especialidad | `200`, `400`, `404` |

**Alta de médico** (`POST /doctors`):

```json
{
  "username": "dr.gonzalez",
  "email": "dr.gonzalez@clinica.com",
  "password": "password123",
  "firstName": "Luis",
  "lastName": "Gonzalez",
  "phone": "5555-0010",
  "specialtyId": 2,
  "licenseNumber": "MED-2001",
  "biography": "Pediatra con 8 años de experiencia.",
  "schedules": [
    { "dayOfWeek": 2, "startTime": "09:00", "endTime": "13:00", "slotDurationMinutes": 30 }
  ]
}
```

Si algo falla (por ejemplo, dos bloques que se cruzan o un usuario repetido) **no queda nada guardado**: ni el usuario ni los horarios.

### 4.5 Horarios y disponibilidad

| Método | Ruta | Rol | Descripción | Respuestas |
|---|---|---|---|---|
| `GET` | `/schedules/available?doctorId=&date=` | Paciente, Secretaria, Admin | Horarios **libres** de un médico en una fecha | `200` |
| `GET` | `/schedules/available/range?doctorId=&startDate=&endDate=` | Paciente, Secretaria, Admin | Cuántos horarios libres tiene cada día (para pintar un calendario). Máximo 62 días | `200`, `400` |
| `GET` | `/schedules?doctorId=` | Admin, Médico | Bloques de horario activos de un médico | `200` |
| `POST` | `/schedules` | Admin | Crea un bloque (`doctorId`, `dayOfWeek`, `startTime`, `endTime`, `slotDurationMinutes`) | `200`, `400`, `404` |
| `PUT` | `/schedules/{id}` | Admin | Edita un bloque | `200`, `400` (cruce o datos inválidos), `409` (dejaría citas pendientes sin horario) |
| `DELETE` | `/schedules/{id}` | Admin | Elimina un bloque (baja lógica) | `204`, `409` (hay citas pendientes dentro del bloque) |

La disponibilidad solo devuelve horarios **que realmente se pueden reservar**: pertenecen a un bloque activo del médico, no están ocupados por una cita activa y cumplen la anticipación mínima (60 minutos).

```json
[ { "date": "2026-11-02", "time": "08:00:00" }, { "date": "2026-11-02", "time": "08:30:00" } ]
```

Un bloque válido cumple: fin posterior al inicio, duración del slot menor o igual a la duración del bloque, y sin cruce con otro bloque activo del mismo médico el mismo día.

### 4.6 Citas

| Método | Ruta | Rol | Descripción | Respuestas |
|---|---|---|---|---|
| `POST` | `/appointments` | Paciente, Secretaria, Admin | Reserva una cita. Secretaria y Admin envían además `patientId` | `201`, `400`, `403`, `404`, `409` |
| `GET` | `/appointments/my-history` | Paciente | Todas las citas propias, la más reciente primero | `200` |
| `GET` | `/appointments?startDate=&endDate=&doctorId=` | Secretaria, Admin | Agenda general de la clínica en un rango (máximo 62 días), opcionalmente de un solo médico | `200`, `400` |
| `PUT` | `/appointments/{id}/reschedule` | Paciente, Secretaria, Admin | Cambia una cita pendiente a otro horario disponible | `200`, `400`, `403`, `409` |
| `POST` | `/appointments/{id}/cancel` | Paciente, Médico, Secretaria, Admin | Cancela una cita pendiente. Cuerpo opcional `{ "reason": "..." }` | `200`, `403`, `409` |
| `GET` | `/appointments/my-agenda?date=` | Médico | Agenda propia de un día | `200` |
| `GET` | `/appointments/my-agenda-range?startDate=&endDate=` | Médico | Agenda propia en un rango | `200` |
| `PUT` | `/appointments/{id}/diagnosis` | Médico | Registra un diagnóstico o una receta en una cita propia y la marca `COMPLETADA` | `200`, `403`, `409` |
| `GET` | `/appointments/{id}/documents` | Médico, Paciente | Documentos de una cita (solo si es propia) | `200`, `403` |

**Reservar** (`POST /appointments`):

```json
{
  "doctorId": 1,
  "appointmentDate": "2026-11-02",
  "appointmentTime": "08:30:00",
  "reason": "Chequeo general"
}
```

Respuesta (también la de reagendar, cancelar e historial):

```json
{
  "id": 12,
  "doctorId": 1,
  "doctorName": "Carlos Perez",
  "specialty": "Medicina General",
  "patientId": 1,
  "patientName": "Jose Ramirez",
  "patientPhone": "5555-0004",
  "appointmentDate": "2026-11-02",
  "appointmentTime": "08:30:00",
  "status": "PENDIENTE",
  "reason": "Chequeo general",
  "cancellationReason": null
}
```

**Registrar diagnóstico o receta** (`PUT /appointments/{id}/diagnosis`):

```json
{ "type": "DIAGNOSTICO", "content": "Cuadro gripal leve. Reposo 3 días." }
```

Cada llamada **agrega** un documento nuevo (historial): no sobrescribe los anteriores.

**Reglas de negocio aplicadas por la API:**

| Regla | Respuesta si no se cumple |
|---|---|
| El horario debe pertenecer a la agenda del médico y estar libre | `409` |
| Reservar con al menos 60 minutos de anticipación; no se permite el pasado | `400` |
| Un paciente no puede tener dos citas activas a la misma fecha y hora, aunque sean con médicos distintos | `409` |
| El paciente solo cancela o reagenda con al menos 2 horas de anticipación (la secretaria y el admin no tienen ese límite) | `409` |
| Solo se cancelan o reagendan citas `PENDIENTE` | `409` |
| Un paciente solo agenda para sí mismo; Secretaria y Admin deben indicar `patientId` | `403` / `400` |
| Un médico solo documenta sus propias citas | `403` |
| No se registran documentos en citas canceladas ni en citas con fecha futura | `409` |
| No se reserva con un médico desactivado | `409` |
| Dos peticiones simultáneas por el mismo horario: gana una, la otra recibe conflicto (restricción única en la base de datos) | `409` |

### 4.7 Documentos (PDF adjunto)

| Método | Ruta | Rol | Descripción | Respuestas |
|---|---|---|---|---|
| `POST` | `/documents/{id}/file` | Médico (dueño de la cita) | Adjunta un PDF (`multipart/form-data`, campo `file`, máximo 10 MB). Un documento admite un solo PDF y no se reemplaza | `200`, `400` (no es PDF), `403`, `409` (ya tiene PDF), `413` |
| `GET` | `/documents/{id}/file` | Médico y Paciente (dueños de la cita) | Descarga el PDF. Requiere el encabezado `Authorization`, por lo que debe pedirse como *blob* y no con un enlace directo | `200`, `403`, `404` (el documento no tiene PDF) |

### 4.8 Usuarios (administración)

| Método | Ruta | Rol | Descripción | Respuestas |
|---|---|---|---|---|
| `GET` | `/users?search=&role=&active=&page=&size=` | Admin | Lista con búsqueda, filtros y **paginación** (máximo 50 por página) | `200` |
| `POST` | `/users` | Admin | Crea un administrador, una secretaria o un paciente. Sin `username` ni `password` se generan; la contraseña temporal se devuelve una sola vez | `201`, `400` (rol médico o inválido), `409` |
| `PUT` | `/users/{id}` | Admin | Edita nombre, correo y teléfono (el rol no cambia) | `200`, `409` |
| `PATCH` | `/users/{id}/active` | Admin | Activa o desactiva una cuenta | `200`, `409` |
| `POST` | `/users/{id}/reset-password` | Admin | Genera una contraseña temporal nueva (se devuelve una sola vez) | `200` |

Restricciones de `PATCH /users/{id}/active` (`409`): no se puede desactivar la propia cuenta ni al último administrador activo, y los médicos se activan o desactivan solo desde `/doctors/{id}/active`.

Respuesta paginada de `GET /users`:

```json
{
  "content": [ { "id": 1, "username": "admin", "role": "ROLE_ADMIN", "active": true, "...": "..." } ],
  "page": 0,
  "size": 15,
  "totalElements": 6,
  "totalPages": 1
}
```

### 4.9 Reportes

| Método | Ruta | Rol | Descripción | Respuestas |
|---|---|---|---|---|
| `GET` | `/reports/summary?startDate=&endDate=` | Admin | Resumen de citas del rango (máximo 366 días): totales por estado, por especialidad, por médico y por día, más médicos y pacientes activos | `200`, `400` |

Campos principales de la respuesta: `totalAppointments`, `pending`, `completed`, `cancelled`, `unattended` (pendientes cuya fecha ya pasó), `activeDoctors`, `activePatients`, `byStatus[]`, `bySpecialty[]`, `byDoctor[]` y `byDay[]`.

## 5. Protección contra abuso

| Mecanismo | Detalle |
|---|---|
| Límite de intentos por IP | `POST /auth/register`: 10 por hora · `POST /auth/login`: 20 por minuto. Superado el límite responde `429` con `Retry-After`. Configurable; funciona en memoria (una instancia) |
| Captcha en el registro | Opcional (Cloudflare Turnstile), desactivado por defecto. Si está activo, el registro exige `captchaToken` y el servidor lo valida contra Cloudflare |
| Contraseñas | Se almacenan con **BCrypt**; nunca se devuelven. Las contraseñas temporales se muestran una sola vez |
| CORS | Orígenes permitidos configurables (`CORS_ALLOWED_ORIGINS`) |

## 6. Pruebas de la API

La colección de Postman (113 solicitudes con pruebas automáticas, organizadas por perfil) se describe y se registra en [`reporte-de-pruebas.md`](reporte-de-pruebas.md). Se ejecuta completa con **Run** en el orden en que está; usa fechas dinámicas y puede repetirse.
