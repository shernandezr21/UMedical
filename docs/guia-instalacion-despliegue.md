# Guía de instalación y despliegue

Cómo poner en marcha **U-Medical** en un equipo local, cómo configurarlo y qué considerar para desplegarlo en un servidor.

## 1. Requisitos

| Herramienta | Versión | Para qué | Cómo verificar |
|---|---|---|---|
| JDK | **21** | Compilar y ejecutar el backend | `java -version` |
| Maven | 3.9+ (o el wrapper `./mvnw` incluido) | Compilar el backend | `./mvnw -v` |
| MySQL | **8.0+** | Base de datos | `mysql --version` |
| Node.js | 20 o superior | Panel web | `node -v` |
| npm | 10+ | Dependencias del panel | `npm -v` |
| Angular CLI | 20.x | Ejecutar el panel | `ng version` |
| Git | cualquiera | Obtener el código | `git --version` |
| Postman *(opcional)* | cualquiera | Ejecutar la colección de pruebas | |

Instalar Angular CLI si falta: `npm install -g @angular/cli@20`.

> **MySQL 8.0.16 o superior** es necesario: el esquema usa restricciones `CHECK` y una columna generada.

## 2. Obtener el código

```bash
git clone https://github.com/<usuario>/UMedical.git
cd UMedical
```

## 3. Base de datos

El script `database/schema.sql` crea la base **`u_medical`**, las tablas y los datos semilla.

> El script comienza con `DROP DATABASE IF EXISTS u_medical;`: **borra la base si ya existe**. 

```bash
mysql -u root -p < database/schema.sql
```

Datos de prueba opcionales:

```bash
mysql -u root -p < database/seed-secretaria.sql      # usuario secretaria1 (misma contraseña que admin)
mysql -u root -p < database/seed-citas-pasadas.sql   # citas pasadas con diagnóstico y receta para paciente1
```

Verificación:

```sql
USE u_medical;
SHOW TABLES;                      -- 8 tablas: roles, users, specialties, doctors, patients, schedules, appointments, documents
SELECT username FROM users;       -- admin, dr.perez, dra.lopez, paciente1 (y secretaria1 si cargaste el seed)
```

### Si usas MySQL Workbench

Abre `database/schema.sql` y ejecútalo completo (icono del rayo). Si tu terminal es **PowerShell de Windows**, el operador `<` no funciona: usa `Get-Content database/schema.sql | mysql -u root -p`, o `source database/schema.sql;` dentro del cliente `mysql`.

## 4. Backend (API REST)

### 4.1 Configuración de secretos

La contraseña de MySQL y el secreto del JWT **no están en el repositorio**. Crea el archivo `backend/src/main/resources/local.properties` (está en `.gitignore`):

```properties
spring.datasource.password=TU_PASSWORD_DE_MYSQL
app.jwt.secret=UNA_CADENA_ALEATORIA_DE_AL_MENOS_32_CARACTERES

# Opcionales para desarrollo
spring.jpa.show-sql=true
app.rate-limit.enabled=false
```

- El secreto JWT debe tener **al menos 32 caracteres** (HS256 exige 256 bits).
- Si falta la contraseña o el secreto, el backend **no arranca** y lo dice con un error `Could not resolve placeholder 'DB_PASSWORD'` (o `JWT_SECRET`). Es intencional.
- `app.rate-limit.enabled=false` desactiva el límite de intentos por IP; útil al repetir pruebas en Postman.

### 4.2 Ejecutar

```bash
cd backend
./mvnw spring-boot:run        # en Windows: mvnw.cmd spring-boot:run
```

La primera vez descarga las dependencias. Cuando termina de arrancar, el backend queda en **http://localhost:8080**. Comprobaciones:

- Swagger UI: http://localhost:8080/swagger-ui.html
- Un endpoint protegido sin token debe responder `401`: http://localhost:8080/api/v1/specialties

Hibernate trabaja en modo `validate`: **no modifica la base de datos**; solo comprueba que las entidades coincidan con las tablas creadas por `schema.sql`.

### 4.3 Variables de configuración

Todas se pueden definir en `local.properties` (con el nombre de la propiedad) o como **variables de entorno** (con el nombre en mayúsculas).

| Variable de entorno | Propiedad | Por defecto | Descripción |
|---|---|---|---|
| `DB_HOST` · `DB_PORT` · `DB_NAME` | | `localhost` · `3306` · `u_medical` | Conexión a MySQL |
| `DB_USER` | | `root` | Usuario de MySQL |
| `DB_PASSWORD` | `spring.datasource.password` | *(obligatoria)* | Contraseña de MySQL |
| `JWT_SECRET` | `app.jwt.secret` | *(obligatoria)* | Secreto de firma de los tokens (≥ 32 caracteres) |
| `JWT_EXPIRATION_MS` | `app.jwt.expiration-ms` | `86400000` (24 h) | Duración del token |
| `CORS_ALLOWED_ORIGINS` | `app.cors.allowed-origins` | localhost, Capacitor y redes locales | Orígenes permitidos, separados por coma. Acepta patrones (`http://localhost:[*]`) |
| `CLINIC_TIMEZONE` | `app.clinic-timezone` | `America/Guatemala` | Zona horaria de la clínica (reglas de anticipación) |
| `UPLOADS_DIR` | `app.uploads.dir` | `uploads/documents` | Carpeta donde se guardan los PDF |
| `RATE_LIMIT_ENABLED` | `app.rate-limit.enabled` | `true` | Límite de intentos por IP |
| `RATE_LIMIT_REGISTER_PER_HOUR` | | `10` | Registros por hora por IP |
| `RATE_LIMIT_LOGIN_PER_MINUTE` | | `20` | Intentos de login por minuto por IP |
| `CAPTCHA_ENABLED` · `CAPTCHA_SECRET` | `app.captcha.*` | `false` · vacío | Captcha (Cloudflare Turnstile) en el registro |
| `FORWARD_HEADERS_STRATEGY` | `server.forward-headers-strategy` | `none` | Poner `framework` detrás de un proxy |

Reglas de agenda (en `application.properties`): `app.appointments.min-booking-lead-minutes=60` (anticipación mínima para reservar) y `app.appointments.min-cancel-hours=2` (anticipación mínima del paciente para cancelar o reagendar).

## 5. Panel web (Angular)

```bash
cd frontend
npm install
ng serve                       # o: npm start
```

El panel queda en **http://localhost:4200** y se recarga solo al guardar cambios.

La URL de la API se define en `frontend/src/app/core/api.config.ts`:

```typescript
export const API_BASE_URL = 'http://localhost:8080/api/v1';
export const CAPTCHA_SITE_KEY = '';      // vacío = sin captcha
```

> Si el backend corre en otra máquina o puerto, cambia `API_BASE_URL` **y** agrega el origen del panel a `CORS_ALLOWED_ORIGINS` en el backend.

## 6. Verificación de la instalación

1. Abre http://localhost:4200 e inicia sesión con `admin` / `password`. Debes llegar al **Panel**.
2. En **Médicos** deben aparecer `Carlos Perez` y `Maria Lopez`.
3. Cierra sesión, inicia con `paciente1` / `password` y entra a **Agendar cita**: al elegir al Dr. Pérez deben aparecer días con horarios libres (lunes y miércoles).
4. *(Opcional)* Importa `postman/u-medical-api.postman_collection.json` y ejecútala con **Run**: son 113 pruebas. Antes carga `seed-secretaria.sql` y `seed-citas-pasadas.sql`, y desactiva el límite de intentos.

### Usuarios de prueba

Contraseña de todos: `password`.

| Usuario | Rol |
|---|---|
| `admin` | Administrador |
| `secretaria1` | Secretaria *(requiere `seed-secretaria.sql`)* |
| `dr.perez` · `dra.lopez` | Médico |
| `paciente1` | Paciente |

## 7. Despliegue local conjunto (demo)

Orden de arranque para una presentación en un solo equipo:

1. Servicio de **MySQL** en ejecución.
2. **Backend** (`./mvnw spring-boot:run`) → esperar a ver `Started ... Application`.
3. **Panel web** (`ng serve`).
4. *(App móvil)* apuntar su `environment.ts` a la IP del equipo donde corre el backend; ver [`API-MOVIL.md`](API-MOVIL.md) para la conexión desde un teléfono o emulador.

Para que un teléfono llegue al backend: misma red Wi-Fi, firewall del equipo abierto al puerto 8080 y, si el backend corre en una máquina virtual, adaptador de red en modo **puente (Bridged)**.

## 8. Despliegue en un servidor

Pasos recomendados para un entorno real (no incluido en la demo local):

### 8.1 Compilar

```bash
# Backend: genera backend/target/umedical-0.0.1-SNAPSHOT.jar
cd backend && ./mvnw clean package -DskipTests

# Panel: genera frontend/dist/frontend/browser
cd frontend && npm ci && ng build
```

Antes de compilar el panel, si lo vas a servir detrás del mismo dominio que la API (ver 8.3), cambia en `api.config.ts`: `API_BASE_URL = '/api/v1'`.

### 8.2 Ejecutar el backend

```bash
export DB_HOST=127.0.0.1 DB_USER=umedical DB_PASSWORD='...'
export JWT_SECRET='...cadena-larga-y-aleatoria...'
export CORS_ALLOWED_ORIGINS=https://clinica.ejemplo.com
export FORWARD_HEADERS_STRATEGY=framework
export UPLOADS_DIR=/var/lib/umedical/documentos
java -jar backend/target/umedical-0.0.1-SNAPSHOT.jar
```

Conviene ejecutarlo como servicio (por ejemplo, con `systemd`) y crear un **usuario de MySQL dedicado** con permisos solo sobre `u_medical`, en lugar de usar `root`.

### 8.3 Servir el panel y la API detrás de un proxy (ejemplo con nginx)

Un único dominio para el panel y la API evita problemas de CORS:

```nginx
server {
    listen 80;
    server_name clinica.ejemplo.com;

    root /var/www/u-medical;                 # contenido de frontend/dist/frontend/browser
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 10M;            # PDF de hasta 10 MB
    }

    location / {
        try_files $uri $uri/ /index.html;    # rutas de Angular
    }
}
```

Activa **HTTPS** (por ejemplo, con Let's Encrypt / certbot): los tokens y las contraseñas viajan por la red, y el cifrado se resuelve en el servidor web, no en el código.

### 8.4 Lista de verificación de seguridad antes de publicar

- [ ] `JWT_SECRET` propio, largo y aleatorio (distinto del de desarrollo).
- [ ] Contraseñas de los usuarios semilla cambiadas (o usuarios semilla eliminados).
- [ ] Usuario de MySQL dedicado; MySQL no expuesto a internet.
- [ ] HTTPS activo.
- [ ] `CORS_ALLOWED_ORIGINS` limitado al dominio real del panel (sin patrones de red local).
- [ ] `FORWARD_HEADERS_STRATEGY=framework` si hay proxy, para que el límite de intentos use la IP real.
- [ ] Captcha activado en el registro (`CAPTCHA_ENABLED=true`, clave del sitio en `api.config.ts`).
- [ ] Copias de seguridad programadas de la base de datos **y** de la carpeta de PDF (`UPLOADS_DIR`).

> Docker Compose queda fuera del alcance de esta entrega y está planificado como mejora posterior.

## 9. Reiniciar los datos

- **Base de datos limpia:** vuelve a ejecutar `database/schema.sql` (y los seeds opcionales).
- **PDF subidos:** borra el contenido de la carpeta `uploads/documents` (o la de `UPLOADS_DIR`). Si reinicias la base sin borrar los archivos, quedan PDF sin referencia (no afecta el funcionamiento).

## 10. Solución de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Unknown database 'u_medical'` al arrancar el backend | No se ejecutó `schema.sql` en ese servidor MySQL | Ejecuta el script (sección 3) y reinicia |
| `Access denied for user` | Contraseña de MySQL incorrecta | Revisa `spring.datasource.password` en `local.properties` |
| `Could not resolve placeholder 'DB_PASSWORD'` / `'JWT_SECRET'` | Falta `local.properties` o la variable de entorno | Crea el archivo (sección 4.1) |
| `Schema-validation: wrong column type ...` o `missing column` | La base creada no coincide con las entidades (script desactualizado) | Vuelve a ejecutar el `schema.sql` actual |
| `The specified key byte array is ... bits` | Secreto JWT demasiado corto | Usa un secreto de 32 caracteres o más |
| Login responde `401` con usuarios semilla | El hash de la contraseña en la base no corresponde a `password` | Vuelve a cargar `schema.sql` |
| El panel muestra «No se pudo conectar con el servidor» | Backend apagado, URL incorrecta o CORS | Verifica que `http://localhost:8080/swagger-ui.html` abre; revisa `api.config.ts` y `CORS_ALLOWED_ORIGINS`; abre la consola del navegador (F12) |
| `429 Too Many Requests` en registro o login | Límite de intentos por IP | Espera el tiempo indicado, o `app.rate-limit.enabled=false` en desarrollo |
| `Port 8080 was already in use` | Otro proceso usa el puerto | Cierra el proceso o define `server.port` |
| `ng: command not found` | Angular CLI no instalado | `npm install -g @angular/cli@20` |
| Error de versión de Node al instalar el panel | Node demasiado antiguo | Usa Node 20 o superior |
| Los PDF no se guardan | Sin permisos de escritura en `UPLOADS_DIR` | Ajusta permisos o cambia la carpeta |
| Un teléfono no conecta con el backend | Red, firewall, VM en NAT o contenido mixto en Capacitor | Ver sección 6 de [`API-MOVIL.md`](API-MOVIL.md) |
