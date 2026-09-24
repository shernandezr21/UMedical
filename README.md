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

Los endpoints de `schedules/available`, `appointments` (crear, historial,
diagnostico) ya tienen sus **reglas de autorizacion por rol** definidas en
`SecurityConfig`

## 6. Decisiones tecnicas relevantes

- **JWT sin estado**: `JwtAuthFilter` valida el token en cada peticion; no hay
  sesiones de servidor (`SessionCreationPolicy.STATELESS`).
- **RBAC**: los roles (`ROLE_ADMIN`, `ROLE_DOCTOR`, `ROLE_PATIENT`,
  `ROLE_SECRETARY`) viven en la tabla `roles` y se mapean a
  `GrantedAuthority` de Spring Security. Las reglas de acceso por ruta estan
  centralizadas en `SecurityConfig.securityFilterChain`.
- **Manejo global de excepciones**: `GlobalExceptionHandler` (`@RestControllerAdvice`)
  centraliza errores de credenciales, acceso denegado, validacion y recursos
  no encontrados en un formato JSON consistente (`ErrorResponse`).
- **CORS explicito**: `CorsConfig` habilita `localhost:4200` (Angular) y
  `localhost:8100` / `capacitor://localhost` (Ionic) desde ya, para que las
  Fases 3 y 4 no tengan que tocar el backend por temas de CORS.
- **`ddl-auto=validate`**: Hibernate valida que las entidades JPA coincidan
  con el esquema creado por `schema.sql`, en vez de generarlo automaticamente
  — asi el script SQL entregado es la unica fuente de verdad del modelo de
  datos (requisito de la rubrica de "Diseno de Base de Datos").

## 7. Estructura del repositorio

```
citas-medicas/
├── README.md
├── database/
│   ├── schema.sql        # DDL + datos semilla (3FN)
│   └── DER.md             # Diagrama ER (Mermaid) + justificacion 3FN
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/citasmedicas/api/
│       ├── config/        # SecurityConfig, CorsConfig, OpenApiConfig
│       ├── entity/         # Role, User, Specialty, Doctor, Patient, Schedule, Appointment
│       ├── repository/     # Interfaces Spring Data JPA
│       ├── security/       # JwtUtil, JwtAuthFilter, CustomUserDetails(Service)
│       ├── dto/            # LoginRequest/Response, DoctorResponse, ErrorResponse
│       ├── controller/     # AuthController, DoctorController, SpecialtyController
│       └── exception/      # GlobalExceptionHandler, ResourceNotFoundException
└── postman/
    ├── citas-medicas.postman_collection.json
    └── citas-medicas.postman_environment.json
```

## 8. Que sigue (Fase 2 — para no perder el hilo despues)

1. **DTOs de citas**: `AppointmentRequest`, `AppointmentResponse`,
   `DiagnosisRequest` con validaciones `@Valid` / `@NotNull` / `@Future`.
2. **Motor de disponibilidad**: dado un `doctorId` + fecha, generar los slots
   libres a partir de `schedules` menos las `appointments` ya reservadas
   (estado != CANCELADA) — esto responde `GET /api/v1/schedules/available`.
3. **Reglas de negocio anti-cruce**: al crear una cita
   (`POST /api/v1/appointments`), validar en una transaccion que el slot
   siga libre justo antes de insertar (condicion de carrera con dos pacientes
   reservando el mismo slot a la vez) — apoyarse en la `UNIQUE KEY` de la
   tabla como ultima linea de defensa.
4. **`GET /api/v1/appointments/my-history`**: usar el `userId` del JWT
   (via `@AuthenticationPrincipal CustomUserDetails`) para resolver el
   `patientId` y listar solo sus propias citas — nunca aceptar un `patientId`
   como parametro externo.
5. **`PUT /api/v1/appointments/{id}/diagnosis`**: validar que el
   `doctorId` del JWT coincide con el `doctor_id` de la cita antes de
   permitir escribir diagnostico/receta.
6. Escribir tests de integracion (`@SpringBootTest` + `MockMvc`) para el
   `GlobalExceptionHandler` y para el flujo de login, de cara al "Reporte de
   pruebas" que piden los entregables finales.

## 9. Notas para la demo/entrega de Fase 1

- Muestra el DER (`database/DER.md`) renderizado en GitHub o en mermaid.live.
- Corre el backend y abre Swagger UI en vivo para mostrar los endpoints.
- Corre la coleccion completa de Postman (Run Collection) para mostrar que
  el login, el JWT y el RBAC funcionan de punta a punta, incluyendo el caso
  negativo (401 sin token, 401 con credenciales invalidas).
