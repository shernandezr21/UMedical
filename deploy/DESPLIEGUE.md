# Despliegue público de U-Medical (para la presentación)

Un solo servidor con tres contenedores: **MySQL** + **API (Spring Boot)** + **web (Angular) con HTTPS automático (Caddy)**.
Solo se publican los puertos 80 y 443; MySQL y la API quedan dentro de la red de Docker.

```
 Navegador / App móvil ──HTTPS──▶ Caddy (:443) ─┬─ /api/*, /swagger-ui*  ──▶ API Spring Boot (:8080) ──▶ MySQL
                                                └─ todo lo demás          ──▶ Panel Angular (archivos estáticos)
```

Tiempo estimado la primera vez: **1 a 1,5 horas** (la mayor parte es crear la cuenta y esperar la compilación).

---

## 1. Dónde alojarlo

Datos verificados en octubre de 2026; los planes gratuitos cambian, confirma al registrarte.

| Opción | Qué te da | Tarjeta | Ojo |
|---|---|---|---|
| **AWS · plan gratuito** (cuentas nuevas) | $100 de crédito al registrarte + hasta $100 más por actividades (como usar EC2). Dura 6 meses o hasta agotar los créditos | Normalmente sí (verificación) | Al terminar el plazo, la cuenta se cierra y los datos se conservan 90 días |
| **Azure · cuenta gratuita** | $200 de crédito durante 30 días; 12 meses de 750 h de VM B1s / B2pts v2 / B2ats v2 | Sí (retención de 1 USD) | Debes pasar a pago por uso en 30 días o se deshabilita; sin cargos mientras no lo hagas |
| **Azure for Students** | $100 de crédito por 12 meses | **No** | Requiere verificar tu correo institucional |

**Elige el tamaño por la RAM, no por el precio:** necesitas **2 GB como mínimo**. Con créditos no estás limitado a las «micro» de 1 GB:
- AWS: `t3.small` (2 GB) o similar marcada como elegible para el plan gratuito.
- Azure: `Standard_B2s` (4 GB), o la B2pts v2 / B2ats v2 si quieres usar la parte gratuita.

Un mes de una VM así consume una fracción del crédito: de sobra para una demo de unos días.

---

## 2. Crear el servidor (Ubuntu 24.04 LTS)

### AWS (EC2)
1. Crea la cuenta y elige el **Free plan**. En *Billing → Budgets* crea un presupuesto con aviso por correo.
2. *EC2 → Launch instance*: nombre `umedical`, imagen **Ubuntu Server 24.04 LTS**, tipo **t3.small**, almacenamiento **20–30 GB gp3**.
3. *Key pair*: crea una y **descarga el `.pem`** (no se puede volver a bajar). `chmod 400 clave.pem`.
4. *Security group* (reglas de entrada): **SSH 22 → solo «My IP»**, **HTTP 80** y **HTTPS 443 → Anywhere (0.0.0.0/0)**.
5. Cuando esté en ejecución: *EC2 → Elastic IPs → Allocate → Associate* con la instancia. Así la IP **no cambia** si la apagas y la prendes.

### Azure
1. Crea la cuenta (gratuita o de estudiante).
2. *Virtual machines → Create*: imagen **Ubuntu Server 24.04 LTS**, tamaño **B2s**, autenticación **SSH public key** (descarga la clave), región cercana a ti.
3. *Networking*: deja abiertos **22** (limita a tu IP), **80** y **443**.
4. En la IP pública de la VM, cambia la asignación a **Static**.

Anota la **IP pública** del servidor.

---

## 3. Dominio gratis con HTTPS

El certificado de Let's Encrypt necesita un nombre, no solo una IP. Dos formas gratuitas:

**A) DuckDNS (recomendada, estable)**
1. Entra a https://www.duckdns.org e inicia sesión (GitHub o Google).
2. Crea un subdominio, por ejemplo `umedical-demo` → queda `umedical-demo.duckdns.org`.
3. En *current ip* pon la IP pública del servidor y pulsa *update ip*.

**B) sslip.io (sin registrarte, para una prueba rápida)**
Si tu IP es `54.12.34.56`, el nombre es `54-12-34-56.sslip.io` y ya apunta a esa IP. Es un servicio compartido y a veces Let's Encrypt limita las emisiones; para la presentación usa DuckDNS.

Comprueba desde tu equipo que el nombre resuelve a la IP:
```bash
getent hosts umedical-demo.duckdns.org
```

---

## 4. Preparar el servidor (una sola vez)

```bash
ssh -i clave.pem ubuntu@IP_DEL_SERVIDOR        # en Azure, el usuario es el que elegiste
```
Copia el script y ejecútalo (instala Docker y crea 2 GB de memoria de intercambio):
```bash
curl -fsSL https://raw.githubusercontent.com/TU_USUARIO/TU_REPO/main/deploy/server-setup.sh -o server-setup.sh
bash server-setup.sh
exit                                            # y vuelve a entrar para usar docker sin sudo
```
(Si el repositorio es privado, sube el archivo con `scp` o pégalo con `nano`.)

---

## 5. Subir el código

**Antes de subir**, asegúrate de que en el repositorio estén:
`docker-compose.prod.yml`, `.dockerignore`, `backend/Dockerfile` (el nuevo), la carpeta `deploy/` y, en `database/`, los archivos
`schema.sql`, `seed-secretaria.sql` y `seed-citas-pasadas.sql`.

**Opción A: git** (si el repo es privado, usa un token personal como contraseña)
```bash
git clone https://github.com/TU_USUARIO/TU_REPO.git UMedical && cd UMedical
```

**Opción B: rsync desde tu computadora** (sin credenciales de Git)
```bash
rsync -az --exclude node_modules --exclude .git --exclude umedical-app --exclude target \
  -e "ssh -i clave.pem" ./ ubuntu@IP_DEL_SERVIDOR:~/UMedical/
ssh -i clave.pem ubuntu@IP_DEL_SERVIDOR "cd UMedical && ls"
```

---

## 6. Configurar secretos

En el servidor, dentro de la carpeta del proyecto:
```bash
cp deploy/.env.example .env
openssl rand -hex 24      # genera claves aleatorias: úsalo para DB_ROOT_PASSWORD, DB_PASSWORD y JWT_SECRET
nano .env
```
Pon tu `DOMAIN` real y asegúrate de que `CORS_ALLOWED_ORIGINS` empiece con `https://` + el mismo dominio. **Sin el carácter `$` en las claves.**

---

## 7. Levantar todo

```bash
docker compose -f docker-compose.prod.yml up -d --build
```
La primera compilación tarda **10 a 20 minutos** (Maven y Angular). Para seguirla:
```bash
docker compose -f docker-compose.prod.yml logs -f api       # espera la línea "Started ... Application"
docker compose -f docker-compose.prod.yml logs web | tail   # Caddy: "certificate obtained successfully"
docker compose -f docker-compose.prod.yml ps
```

---

## 8. Probar

| Qué | Dirección |
|---|---|
| Panel web | `https://TU_DOMINIO` |
| Swagger | `https://TU_DOMINIO/swagger-ui.html` |
| API sin token | `https://TU_DOMINIO/api/v1/specialties` → debe responder **401** en JSON |

Inicia sesión con `admin` / `password` y recorre el flujo completo: crea un médico, agenda como `paciente1`, documenta como `dr.perez`.

---

## 9. App móvil apuntando al servidor

En `umedical-app/src/environments/environment.prod.ts` (el APK usa este archivo):
```ts
export const environment = {
  production: true,
  apiUrl: 'https://TU_DOMINIO/api/v1',
};
```
Luego:
```bash
npm run build && npx cap sync android
cd android && ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Con HTTPS ya no hace falta que el teléfono esté en la misma red que tu PC: funciona con datos móviles o cualquier Wi-Fi.

---

## 10. Seguridad mínima antes de compartir el enlace

- **Cambia las contraseñas de ejemplo.** `admin`, `secretaria1`, `dr.perez`, `dra.lopez` y `paciente1` usan `password`, y el enlace será público. En el panel: *Usuarios → Restablecer contraseña*; la clave temporal se muestra una sola vez, guárdala. Si tus evaluadores deben entrar con claves conocidas, déjalas solo durante la presentación.
- **Respaldo antes de la demo:** `bash deploy/backup.sh`.
- **SSH solo desde tu IP** y con clave (nunca contraseña).
- **MySQL no está publicado** a internet: no abras el 3306.
- **Swagger es público.** Está bien para la presentación (los endpoints están protegidos por token), pero no lo dejes así a largo plazo.
- Los datos son de ejemplo: no registres información real de pacientes.

---

## 11. Operación

| Tarea | Comando (en la carpeta del proyecto) |
|---|---|
| Ver logs | `docker compose -f docker-compose.prod.yml logs -f api` |
| Actualizar tras un cambio de código | `git pull && docker compose -f docker-compose.prod.yml up -d --build` |
| Reiniciar un servicio | `docker compose -f docker-compose.prod.yml restart api` |
| Respaldar base y PDF | `bash deploy/backup.sh` |
| Volver a los datos de ejemplo | `bash deploy/reset-demo-data.sh` (borra citas, usuarios nuevos y PDF) |

Los datos viven en volúmenes de Docker: sobreviven a reinicios y a `up --build`. **No uses `docker compose down -v`**: borraría también los certificados HTTPS.

---

## 12. Apagar para no gastar crédito

- **Pausa temporal:** detén la instancia (los datos se conservan; en AWS conserva la Elastic IP asociada).
- **Al terminar todo:** AWS → *Terminate instance* y *Release Elastic IP*. Azure → elimina el *Resource group* completo. Respalda antes (`deploy/backup.sh`) y descarga los archivos a tu equipo.

---

## 13. Problemas comunes

| Síntoma | Causa y solución |
|---|---|
| El navegador no abre el sitio | Puertos 80/443 cerrados en el *security group* / NSG, o el dominio no apunta a la IP (`getent hosts DOMINIO`). |
| Certificado HTTPS no se emite | Caddy necesita el puerto 80 abierto y el DNS correcto: `docker compose -f docker-compose.prod.yml logs web`. Reintenta solo. |
| Falla la compilación con `heap out of memory` o `Killed` | Poca RAM: confirma el swap (`free -h`) o usa una VM de 4 GB. |
| `502 Bad Gateway` en `/api` | La API aún arranca (espera 1–2 min) o se cayó: `logs api`. |
| `Could not resolve placeholder 'JWT_SECRET'` | Falta un valor en `.env`. |
| `Access denied for user 'umedical'` | Cambiaste `DB_PASSWORD` después del primer arranque. Borra los datos con `deploy/reset-demo-data.sh` o restaura la clave original. |
| `Schema-validation: missing table ...` | La base no coincide con las entidades: revisa que `database/schema.sql` y `deploy/mysql-init/02-bloques-5-7.sql` sean los actuales. |
| El panel carga pero el login falla | Abre las herramientas de desarrollo (F12 → Red): la petición debe ir a `/api/v1/auth/login` de **tu mismo dominio**. |
| La app móvil no conecta | No se recompiló el APK tras cambiar `environment.prod.ts`, o `CORS_ALLOWED_ORIGINS` perdió `http://localhost:[*]`. |
