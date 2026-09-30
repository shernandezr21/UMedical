# Sistema de Gestion de Servicios Medicos

| Item | Ubicacion |
|---|---|
| Script de base de datos | `database/schema.sql` |
| Diagrama Entidad-Relacion + justificacion | `database/DER.md` |
| Backend Spring Boot (modelos JPA, repos, seguridad JWT) | `backend/` |
| Coleccion Postman (login + endpoints basicos + RBAC) | `postman/` |

## 1. Requisitos previos

- Java 21 (JDK)
- Maven 3.9+
- MySQL
- Postman

## 2. Levantar la base de datos

```bash
mysql -u root -p < database/schema.sql
```

Esto crea la base `u_medical`, las 7 tablas y datos
semilla:

- 1 admin: `admin` / `password`
- 2 doctores: `dr.perez` / `password` (Medicina General), `dra.lopez` / `password` (Cardiologia)
- 1 paciente: `paciente1` / `password`

> La contrasenia de todos los usuarios semilla es `password`

## 3. Configurar y correr el backend

Editar `backend/src/main/resources/application.properties` si nuestro usuario o
password de MySQL no son `root` / `root`:

```properties
spring.datasource.username=root
spring.datasource.password=root
```

Luego, desde la carpeta `backend/`:

```bash
cd backend
mvn spring-boot:run
```

El servidor arranca en `http://localhost:8080`.

- Swagger UI (documentacion tecnica de la API): `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## 4. Probar con Postman

1. Importar `postman/citas-medicas.postman_collection.json` y
   `postman/citas-medicas.postman_environment.json`.
2. Seleccionamos el environment "Citas Medicas - Local".
3. Correr **Auth > Login - Admin (OK)** primero: guardar el token automaticamente
   en la variable de coleccion `token` (via test script).
4. Correr el resto de requests de la carpeta **Endpoints protegidos** — ya usan
   `Authorization: Bearer {{token}}`.
5. La request **Especialidades SIN token** confirma que el endpoint rechaza
   peticiones no autenticadas (403), verificando que Spring Security + JWT
   estan realmente protegiendo la API.

## 5. Endpoints implementados hasta 24 sept 2026

| Metodo | Ruta | Rol requerido | Descripcion |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Publico | Login, devuelve JWT |
| GET | `/api/v1/specialties` | Cualquier rol autenticado | Catalogo de especialidades |
| GET | `/api/v1/doctors?specialtyId=` | Cualquier rol autenticado | Listado de medicos, filtrable |
| GET | `/api/v1/schedules/available?doctorId=&date=` | PATIENT, ADMIN, SECRETARY | Horarios disponibles de un medico en una fecha |
| POST | `/api/v1/appointments` | PATIENT, ADMIN, SECRETARY | Reserva una cita (con validacion anti-cruce) |
| GET | `/api/v1/appointments/my-history` | PATIENT | Historial de citas del paciente autenticado |
| PUT | `/api/v1/appointments/{id}/diagnosis` | DOCTOR | Registra diagnostico/receta (crea un `Document`) |

## 6. Que sigue (Fase 3)

1. Panel web en Angular: login, guards por rol, interceptor HTTP para el
   JWT, gestion de medicos/horarios, modulo de consulta medica.
2. App movil en Ionic: flujo de agendamiento, tabs de citas pendientes/
   historial, almacenamiento seguro del token con `@capacitor/preferences`.
3. Tests de integracion (`@SpringBootTest` + `MockMvc`) para el motor de
   disponibilidad y las reglas anti-cruce, de cara al reporte de pruebas.
