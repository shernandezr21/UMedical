# U-Medical API: guía para la app móvil (paciente)

Esta guía explica el flujo del paciente y los casos que Swagger no cuenta. La fuente de verdad de cada endpoint es Swagger (`/swagger-ui.html`); si algo de aquí no coincide con Swagger, manda Swagger y avísale a quien mantiene el backend.

## 1. Datos básicos

| | |
|---|---|
| URL base | `http://<IP-del-equipo-con-el-backend>:8080/api/v1` |
| Swagger | `http://<IP-del-equipo-con-el-backend>:8080/swagger-ui.html` |
| Formato | JSON UTF-8 (excepto el PDF de los documentos) |
| Autenticación | JWT en el header `Authorization: Bearer <token>` |
| Duración del token | 24 horas. No hay refresh token: al vencer, la API responde **401** y hay que iniciar sesión de nuevo |
| Cuenta de prueba | usuario `paciente1`, contraseña `password` (o registrar una nueva) |
| Citas pasadas de prueba | `database/seed-citas-pasadas.sql` (2 completadas con diagnóstico y receta) |
| Colección de Postman | `postman/u-medical-api.postman_collection.json` |

Formatos: fechas `yyyy-MM-dd`, horas `HH:mm:ss`. Estados de cita: `PENDIENTE`, `COMPLETADA`, `CANCELADA`. Tipos de documento: `DIAGNOSTICO`, `RECETA`.

## 2. Autenticación

### Registro (público): `POST /auth/register`
```json
{
  "username": "maria.gomez",
  "email": "maria.gomez@correo.com",
  "password": "Clave12345",
  "firstName": "Maria",
  "lastName": "Gomez",
  "phone": "5555-0100",
  "birthDate": "1998-05-20",
  "address": "Calle de prueba 123",
  "bloodType": "O+"
}
```
Obligatorios: `username` (4 a 50, letras, números, `.`, `-`, `_`), `email`, `password` (8 a 72), `firstName`, `lastName`. Opcionales: el resto. Responde **201** con lo mismo que el login (ya trae token, no hace falta iniciar sesión después). **409** si el username o el email ya existen.

### Login (público): `POST /auth/login`
```json
{ "username": "paciente1", "password": "password" }
```
Responde **200**:
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
**401** si las credenciales son incorrectas. El mismo login sirve para doctores y admin: la app de pacientes debe rechazar cualquier `role` distinto de `ROLE_PATIENT`.

## 3. Flujo del paciente

Todos requieren token.

| Método y ruta | Para qué | Respuesta |
|---|---|---|
| `GET /specialties` | Lista de especialidades | `[{ id, name, description }]` |
| `GET /doctors?specialtyId=1` | Médicos (el filtro es opcional) | `[{ id, fullName, specialty, licenseNumber, biography }]` |
| `GET /schedules/available?doctorId=1&date=2026-11-02` | Horarios **libres** de un médico en una fecha | `[{ date, time }]` (vacío si no atiende ese día) |
| `POST /appointments` | Reservar | **201** con la cita |
| `GET /appointments/my-history` | Todas mis citas, la más reciente primero | `[cita]` |
| `PUT /appointments/{id}/reschedule` | Cambiar de horario una cita pendiente | **200** con la cita |
| `POST /appointments/{id}/cancel` | Cancelar una cita pendiente | **200** con la cita |
| `GET /appointments/{id}/documents` | Diagnóstico y receta de mi cita | `[{ id, type, content, appointmentId, fileName }]` |
| `GET /documents/{id}/file` | PDF adjunto de un documento | `application/pdf` |
| `GET /patients/me` | Mi perfil | el perfil (ver abajo) |
| `PUT /patients/me` | Editar mi perfil | **200** con el perfil actualizado |
| `PUT /users/me/password` | Cambiar mi contraseña | **204** sin cuerpo |

### Reservar: `POST /appointments`
```json
{
  "doctorId": 1,
  "appointmentDate": "2026-11-02",
  "appointmentTime": "08:30:00",
  "reason": "Chequeo general"
}
```
`appointmentTime` debe ser uno de los `time` que devolvió `/schedules/available` para esa fecha.

### Reagendar: `PUT /appointments/{id}/reschedule`
```json
{ "appointmentDate": "2026-11-09", "appointmentTime": "09:00:00" }
```

### Cancelar: `POST /appointments/{id}/cancel`
El cuerpo es opcional: `{ "reason": "Tuve un imprevisto" }`.

### La cita (respuesta de reservar, reagendar, cancelar e historial)
```json
{
  "id": 12,
  "doctorId": 1,
  "doctorName": "Carlos Perez",
  "specialty": "Medicina General",
  "patientName": "Jose Ramirez",
  "appointmentDate": "2026-11-02",
  "appointmentTime": "08:30:00",
  "status": "PENDIENTE",
  "reason": "Chequeo general",
  "cancellationReason": null
}
```

### PDF de un documento
`GET /documents/{id}/file` necesita el header `Authorization`, así que **no sirve un enlace directo ni abrir la URL en el navegador**. Hay que pedirlo con `HttpClient` y `responseType: 'blob'`, y luego mostrarlo desde ese blob. Solo tiene PDF el documento cuyo `fileName` no es `null`; si no lo tiene, responde **404**.

### Perfil y contraseña

`GET /patients/me` responde **200**:
```json
{
  "patientId": 1,
  "userId": 4,
  "username": "paciente1",
  "email": "paciente1@correo.com",
  "firstName": "Jose",
  "lastName": "Ramirez",
  "phone": "5555-0004",
  "birthDate": "1995-04-12",
  "address": "Calle de prueba 123",
  "bloodType": "O+"
}
```
`PUT /patients/me` reemplaza los datos editables, así que se manda el formulario completo. El `username` no se puede cambiar (es el identificador del token). Obligatorios: `email`, `firstName`, `lastName`; los demás se pueden omitir o mandar en `null`, y entonces se borran.
```json
{
  "email": "paciente1@correo.com",
  "firstName": "Jose",
  "lastName": "Ramirez",
  "phone": "5555-0004",
  "birthDate": "1995-04-12",
  "address": "Calle de prueba 123",
  "bloodType": "O+"
}
```
**409** si el email ya lo usa otro usuario.

`PUT /users/me/password`:
```json
{ "currentPassword": "password", "newPassword": "ClaveNueva123" }
```
La contraseña nueva debe tener de 8 a 72 caracteres y ser distinta de la actual. Si `currentPassword` es incorrecta responde **400**, a propósito y **no 401**: un 401 cierra la sesión en el interceptor de la app. El token actual sigue siendo válido después de cambiar la contraseña.

## 4. Errores

Todos los errores tienen el mismo formato:
```json
{
  "timestamp": "2026-10-16T21:15:03.512",
  "status": 409,
  "error": "Conflict",
  "message": "Ese horario no esta disponible: esta fuera del horario del medico o ya fue reservado",
  "details": null
}
```
`message` está pensado para mostrarse al usuario. En los errores de validación (400) `details` trae la lista de campos con problema, por ejemplo `["email: El email no es valido"]`.

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos, fecha pasada, o con menos anticipación de la permitida |
| 401 | Sin token, token inválido o vencido (en el login: credenciales incorrectas). La app debe cerrar sesión y llevar al login |
| 403 | Autenticado, pero la cita o el recurso no es suyo, o su rol no alcanza |
| 404 | No existe (cita, documento, o documento sin PDF) |
| 409 | Choca con el estado actual: horario no disponible, ya tiene otra cita a esa hora, la cita no está pendiente, username o email repetidos |

## 5. Reglas que el usuario va a encontrar

- Se reserva con **al menos 60 minutos** de anticipación. Se puede reservar para hoy.
- Solo se puede reservar un horario que aparece en `/schedules/available`.
- Un paciente no puede tener dos citas activas a la misma fecha y hora, aunque sean con médicos distintos.
- Cancelar y reagendar: solo citas `PENDIENTE`, solo las propias, y con **al menos 2 horas** de anticipación.
- Un horario cancelado vuelve a quedar libre.
- Los documentos (diagnóstico y receta) existen solo después de que el doctor atiende la cita.
- Cada documento es parte de un historial: no se editan, se agregan nuevos.

## 6. Conectar la app con el backend (emulador o teléfono)

El error típico es que el login muestra "No se pudo conectar con el servidor". Eso es `status 0`: la app no llegó al backend. Para saber por qué:

**Paso 1. ¿Hay red entre el teléfono y el backend?** Abre el navegador **del teléfono** en `http://<IP>:8080/swagger-ui.html`.
- Si no carga, no es un problema de la app. Revisa:
  - La IP es la del equipo donde corre el backend (`hostname -I` en Linux, `ipconfig` en Windows).
  - El teléfono y el equipo están en la misma red Wi-Fi, y la red no aísla dispositivos (redes de invitados suelen hacerlo).
  - Si el backend corre en una máquina virtual, el adaptador de red debe estar en **modo puente (Bridged)**; en NAT el teléfono no la ve.
  - Si el equipo es Windows, el firewall debe permitir el puerto 8080 de entrada.
- En el **emulador de Android** el equipo anfitrión no es su IP de Wi-Fi sino `10.0.2.2`: `http://10.0.2.2:8080/api/v1`.

**Paso 2. Si Swagger abre pero la app no conecta**, el problema está en el WebView. Capacitor reciente sirve la app Android desde `https://localhost` y el WebView bloquea llamadas `http://` desde una página `https://` (contenido mixto). En `capacitor.config.ts`:
```ts
server: {
  androidScheme: 'http',   // la app se sirve desde http://localhost: sin contenido mixto
  cleartext: true,
},
```
Después hay que sincronizar y volver a compilar: `npx ionic build && npx cap sync android` y correr de nuevo la app.

**Paso 3. Confirmar la causa exacta.** Con el teléfono por USB, abre `chrome://inspect/#devices` en Chrome del escritorio, entra a la consola de la app y mira el error:
- `Mixed Content`: falta el paso 2.
- `blocked by CORS policy`: el backend no reconoce el origen de la app. Avisa cuál es el origen que aparece en el mensaje; se agrega en `CORS_ALLOWED_ORIGINS`. Por defecto el backend acepta `http://localhost`, `https://localhost`, `capacitor://localhost`, `ionic://localhost` y las IP de red local (`192.168.*`, `10.*`) con cualquier puerto.
- `ERR_CONNECTION_TIMED_OUT` o `ERR_CONNECTION_REFUSED`: es de red, vuelve al paso 1.

**Dónde cambiar la URL:** `src/environments/environment.ts` (y `environment.prod.ts`). Es la IP del backend, y cambia si la red le asigna otra al equipo: no la dejen fija en el repositorio.

## 7. Limitaciones conocidas

- Una cita pasada que nadie atendió se queda `PENDIENTE`: aún no existe el estado "no asistió". En la pestaña "Pendientes" conviene filtrar también por fecha (`appointmentDate >= hoy`).
- No hay refresh token: a las 24 h hay que iniciar sesión de nuevo.