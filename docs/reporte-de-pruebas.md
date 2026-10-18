# Reporte de pruebas

**Proyecto:** U-Medical — Sistema de gestión de servicios médicos
**Alcance:** panel web y API REST (funcionalidades hasta el bloque de trabajo 4)
**Documento:** plan de pruebas, casos de prueba y registro de resultados

> **Cómo usar este documento.** Los casos ya están redactados. Al ejecutar cada uno en el sistema, marque el resultado (✅ Pasó · ❌ Falló · ➖ No aplica), tome la captura indicada en la columna *Evidencia* y guárdela en `docs/capturas/` con ese nombre. Al final complete el [resumen de resultados](#9-resumen-de-resultados) y las [conclusiones](#11-conclusiones). Los casos marcados con ⭐ son el mínimo recomendado si el tiempo es corto.

## Contenido

1. [Objetivo y alcance](#1-objetivo-y-alcance)
2. [Estrategia de pruebas](#2-estrategia-de-pruebas)
3. [Entorno de pruebas](#3-entorno-de-pruebas)
4. [Datos de prueba](#4-datos-de-prueba)
5. [Pruebas automatizadas de la API (Postman)](#5-pruebas-automatizadas-de-la-api-postman)
6. [Casos de prueba manuales](#6-casos-de-prueba-manuales)
7. [Defectos detectados y corregidos durante el desarrollo](#7-defectos-detectados-y-corregidos-durante-el-desarrollo)
8. [Defectos encontrados en esta ejecución](#8-defectos-encontrados-en-esta-ejecución)
9. [Resumen de resultados](#9-resumen-de-resultados)
10. [Limitaciones de la validación](#10-limitaciones-de-la-validación)
11. [Conclusiones](#11-conclusiones)
12. [Anexo: lista de capturas](#12-anexo-lista-de-capturas)

---

## 1. Objetivo y alcance

**Objetivo.** Verificar que el sistema cumple los requisitos funcionales de cada perfil (paciente, secretaria, médico y administrador), que aplica correctamente las reglas de negocio y que protege el acceso según el rol.

**Dentro del alcance:** autenticación y sesión; portal del paciente; recepción (secretaria); consulta del médico; administración (usuarios, médicos, horarios, especialidades y panel); seguridad por roles; reglas de agenda; API REST.

**Fuera del alcance de este reporte:** la app móvil (la prueba su responsable), pruebas de carga y rendimiento, y pruebas unitarias automatizadas (ver [limitaciones](#10-limitaciones-de-la-validación)).

## 2. Estrategia de pruebas

| Nivel | Técnica | Herramienta | Evidencia |
|---|---|---|---|
| API | Pruebas funcionales y de seguridad automatizadas: códigos de estado, contenido de las respuestas, permisos por rol y reglas de negocio | Postman (colección de **113 solicitudes** con pruebas) | Captura del *Runner* + resumen de resultados |
| Interfaz | Pruebas funcionales manuales por perfil, siguiendo los casos de la sección 6 | Navegador (flujos reales de usuario) | Capturas por caso |
| Seguridad | Intentos de acceso sin sesión, con otro rol y con cuentas desactivadas | Navegador, Postman | Capturas por caso |
| Datos | Verificación directa en la base de datos (hash de contraseñas, restricciones) | Cliente MySQL | Captura de la consulta |

**Criterio de aceptación.** El resultado real coincide con el resultado esperado. Un caso falla si el comportamiento difiere, hay un error visible en pantalla o en consola, o se permite una acción que no corresponde al rol.

## 3. Entorno de pruebas

*(Complete con los datos reales del equipo donde ejecutó las pruebas.)*

| Elemento | Valor |
|---|---|
| Fecha(s) de ejecución | |
| Responsable(s) | |
| Sistema operativo | |
| Navegador y versión | |
| Java / Node / Angular CLI | Java 21 · Node ___ · Angular CLI 20.x |
| MySQL | versión ___ |
| Backend | http://localhost:8080 |
| Panel web | http://localhost:4200 |
| Versión del código (commit) | `git rev-parse --short HEAD` → ___ |
| Límite de intentos por IP | Desactivado para las pruebas de Postman (`app.rate-limit.enabled=false`) |

## 4. Datos de prueba

Base creada con `database/schema.sql` más `seed-secretaria.sql` y `seed-citas-pasadas.sql`.

| Usuario | Contraseña | Rol | Uso en las pruebas |
|---|---|---|---|
| `admin` | `password` | Administrador | Casos ADM, Panel, usuarios, médicos |
| `secretaria1` | `password` | Secretaria | Casos SEC |
| `dr.perez` | `password` | Médico (Medicina General; lunes y miércoles 08:00–12:00) | Casos DOC; tiene citas pasadas pendientes por documentar |
| `dra.lopez` | `password` | Médico (Cardiología; martes 14:00–18:00) | Pruebas de permisos entre médicos |
| `paciente1` | `password` | Paciente | Casos PAC; tiene 2 citas completadas con documentos y 1 pasada sin atender |

---

## 5. Pruebas automatizadas de la API (Postman)

**Archivo:** `postman/u-medical-api.postman_collection.json` — **113 solicitudes**, cada una con pruebas automáticas (código de estado y contenido). Usa fechas dinámicas y elige horarios libres reales, por lo que puede ejecutarse varias veces.

### Cobertura por carpeta

| Carpeta | Solicitudes | Qué verifica |
|---|---:|---|
| 1. Autenticación y registro | 9 | Login válido e inválido, registro, datos inválidos, duplicados, rutas protegidas sin token (401) |
| 2. Catálogos | 11 | Especialidades y médicos; alta atómica de médico con horarios (incluye rollback ante cruces y duplicados); permisos por rol |
| 3. Paciente: agendar, reagendar y cancelar | 18 | Disponibilidad, reserva, horario ocupado, doble cita del paciente, anticipación mínima, reagendar, cancelar, liberación del horario, citas ajenas |
| 4. Doctor: agenda y diagnóstico | 10 | Agenda propia, diagnóstico y receta, historial acumulado, cita futura, cita ajena, cita cancelada |
| 5. Paciente ve sus documentos | 4 | Visualización de documentos propios; acceso denegado a documentos ajenos |
| 6. Perfil y contraseña | 9 | Perfil del paciente, edición, correo duplicado, cambio de contraseña y validaciones |
| 7. Secretaria | 18 | Agenda general, alta de paciente en recepción, agendar a nombre, reagendar y cancelar, permisos |
| 8. Admin: usuarios, médicos, horarios y panel | 34 | Gestión de usuarios (alta, edición, desactivar, restablecer), protecciones, administración de médicos y horarios, citas huérfanas (409), resumen del panel, permisos |
| **Total** | **113** | |

### Cómo ejecutarla

1. Cargar `seed-secretaria.sql` y `seed-citas-pasadas.sql` (una sola vez) y arrancar el backend con `app.rate-limit.enabled=false`.
2. En Postman: **Import** → seleccionar el archivo de la colección.
3. Clic derecho en la colección → **Run collection** → **Run**.
4. Al terminar, tomar captura del resumen y registrar los totales.

### Resultado de la ejecución

| Dato | Valor |
|---|---|
| Fecha de ejecución | |
| Solicitudes ejecutadas | /113 |
| Pruebas que pasaron | |
| Pruebas que fallaron | |
| Observaciones | |

**Evidencia:** `capturas/24-postman-runner.png`

![Resultado del Runner de Postman](capturas/24-postman-runner.png)

---

## 6. Casos de prueba manuales

Marque el resultado con ✅ / ❌ / ➖ en la columna *Resultado*. Los nombres de la columna *Evidencia* corresponden a archivos de `docs/capturas/`.

### 6.1 Autenticación y sesión (AUT)

| ID | Caso | Pasos | Resultado esperado | Resultado | Evidencia |
|---|---|---|---|:--:|---|
| AUT-01 ⭐ | Inicio de sesión correcto | Ir a `/login`, ingresar `admin` / `password` | Entra al sistema y llega al **Panel**; el menú muestra las opciones de administrador | ⬜ | `01-login.png` |
| AUT-02 ⭐ | Contraseña incorrecta | Ingresar `admin` con una contraseña errónea | Aviso de credenciales incorrectas; no entra | ⬜ | `AUT-02.png` |
| AUT-03 | Campos vacíos | Presionar *Ingresar* sin datos | No se envía el formulario; los campos obligatorios se marcan | ⬜ | `AUT-03.png` |
| AUT-04 ⭐ | Registro de paciente válido | *Crear cuenta*, completar datos válidos | Cuenta creada, sesión iniciada y llegada a **Mis citas** | ⬜ | `02-registro-paciente.png` |
| AUT-05 | Registro con usuario repetido | Registrar con `paciente1` | Error indicando que el usuario ya está en uso | ⬜ | `AUT-05.png` |
| AUT-06 | Validaciones del registro | Correo mal escrito, contraseña de 5 caracteres, confirmación distinta | Cada campo muestra su mensaje de error; no se envía | ⬜ | `AUT-06.png` |
| AUT-07 ⭐ | Cerrar sesión | Presionar *Cerrar sesión*; luego usar el botón «atrás» del navegador | Vuelve al login; al retroceder no se accede a pantallas protegidas | ⬜ | `AUT-07.png` |
| AUT-08 ⭐ | Sesión vencida o token inválido | Con sesión iniciada, alterar el valor `umedical_token` en el almacenamiento local (F12 → Application) y recargar una pantalla | El sistema redirige al login | ⬜ | `26-sesion-expirada.png` |
| AUT-09 ⭐ | Ruta protegida sin sesión | Sin sesión, abrir `/dashboard/panel` | Redirige al login | ⬜ | `AUT-09.png` |
| AUT-10 ⭐ | Ruta de otro perfil | Como paciente, abrir `/dashboard/usuarios` | Pantalla **«No tienes acceso a esta sección»** | ⬜ | `25-acceso-denegado.png` |
| AUT-11 | Pantalla inicial por rol | Iniciar sesión con paciente, secretaria y admin | Paciente → Mis citas · Secretaria → Calendario · Admin → Panel | ⬜ | `AUT-11.png` |
| AUT-12 ⭐ | Cuenta desactivada | Desactivar una cuenta (ADM-08) e intentar iniciar sesión con ella | No permite el acceso | ⬜ | `AUT-12.png` |

### 6.2 Paciente (PAC) — usuario `paciente1`

| ID | Caso | Pasos | Resultado esperado | Resultado | Evidencia |
|---|---|---|---|:--:|---|
| PAC-01 ⭐ | Mis citas | Entrar a *Mis citas* | Pestañas *Próximas* e *Historial* con contador y citas con color por estado | ⬜ | `03-paciente-mis-citas-proximas.png` |
| PAC-02 | Directorio de médicos | Abrir *Médicos* y filtrar por especialidad | Se listan médicos activos; el filtro funciona | ⬜ | `08-paciente-medicos.png` |
| PAC-03 | Agendar desde el directorio | En *Médicos*, **Agendar con este médico** | Abre *Agendar* con ese médico preseleccionado | ⬜ | `PAC-03.png` |
| PAC-04 ⭐ | Calendario de disponibilidad | En *Agendar*, elegir al Dr. Pérez | Calendario de 14 días: días con horarios muestran «N libres»; los demás aparecen atenuados | ⬜ | `05-paciente-agendar-medico.png` |
| PAC-05 ⭐ | Reservar una cita | Elegir día y hora; escribir motivo; confirmar | Mensaje «La cita quedó reservada» | ⬜ | `06-paciente-agendar-horario.png` |
| PAC-06 ⭐ | La cita aparece en Próximas | Ir a *Mis citas → Próximas* | La nueva cita está en la lista con estado *Pendiente* | ⬜ | `PAC-06.png` |
| PAC-07 | Horario ocupado | Volver a *Agendar* con el mismo médico y día | El horario reservado ya no aparece como disponible | ⬜ | `PAC-07.png` |
| PAC-08 | Navegar ventanas de fechas | Usar **Siguientes** y **Anteriores** | El calendario avanza y retrocede de 14 en 14 días | ⬜ | `PAC-08.png` |
| PAC-09 ⭐ | Reagendar | En una cita próxima, **Reagendar**, elegir otro horario y confirmar | La cita cambia de fecha/hora y el horario anterior queda libre | ⬜ | `07-paciente-reagendar.png` |
| PAC-10 ⭐ | Cancelar | **Cancelar**, escribir motivo y confirmar | La cita pasa al *Historial* como *Cancelada* con su motivo | ⬜ | `PAC-10.png` |
| PAC-11 | Cita con menos de 2 horas | Con una cita para dentro de menos de 2 horas (reservada para hoy) | La cita muestra un aviso y no ofrece cancelar ni reagendar | ⬜ | `PAC-11.png` |
| PAC-12 ⭐ | Ver diagnóstico y receta | En *Historial*, una cita completada → **Ver diagnóstico y receta** | Se despliegan los documentos con su fecha | ⬜ | `04-paciente-mis-citas-historial-documentos.png` |
| PAC-13 | Abrir PDF adjunto | En un documento con PDF, **Abrir PDF** | El PDF se abre en una pestaña nueva | ⬜ | `PAC-13.png` |
| PAC-14 | Cita pasada sin atender | Revisar el *Historial* | La cita pasada que nadie atendió se muestra como *Sin atender* | ⬜ | `PAC-14.png` |
| PAC-15 ⭐ | Editar perfil | En *Mi perfil*, cambiar teléfono y guardar | Mensaje de éxito; los datos persisten al recargar | ⬜ | `09-paciente-perfil.png` |
| PAC-16 | Correo repetido en el perfil | Cambiar el correo por el de otro usuario | Error: el correo ya está en uso | ⬜ | `PAC-16.png` |
| PAC-17 ⭐ | Cambiar contraseña | Con la contraseña actual correcta, cambiarla; cerrar sesión y entrar con la nueva | Cambio exitoso; el login funciona con la nueva contraseña | ⬜ | `PAC-17.png` |
| PAC-18 | Contraseña actual incorrecta | En el cambio de contraseña, escribir mal la actual | Se muestra el error y la **sesión no se cierra** | ⬜ | `PAC-18.png` |

### 6.3 Secretaria (SEC) — usuario `secretaria1`

| ID | Caso | Pasos | Resultado esperado | Resultado | Evidencia |
|---|---|---|---|:--:|---|
| SEC-01 ⭐ | Calendario de la clínica | Entrar a *Calendario* | Vista semanal con las citas de todos los médicos, con color por estado | ⬜ | `10-secretaria-calendario.png` |
| SEC-02 | Filtrar por médico | Elegir un médico en el filtro | Solo se muestran sus citas | ⬜ | `SEC-02.png` |
| SEC-03 | Navegar semanas | Usar *Anterior*, *Hoy* y *Siguiente* | Cambia la semana; *Hoy* regresa a la actual | ⬜ | `SEC-03.png` |
| SEC-04 ⭐ | Detalle de una cita | Presionar una cita | Muestra paciente, teléfono, médico, motivo y estado | ⬜ | `11-secretaria-detalle-cita.png` |
| SEC-05 | Reagendar desde el calendario | En una cita pendiente, **Reagendar** y elegir otro horario | La cita cambia de horario | ⬜ | `SEC-05.png` |
| SEC-06 | Cancelar desde el calendario | **Cancelar** con motivo | La cita queda cancelada | ⬜ | `SEC-06.png` |
| SEC-07 ⭐ | Buscar pacientes | En *Pacientes*, buscar por nombre, usuario o correo | La lista se filtra según la búsqueda | ⬜ | `12-secretaria-pacientes-busqueda.png` |
| SEC-08 ⭐ | Registrar paciente | **Nuevo paciente** con nombre, apellido y correo | Muestra usuario y contraseña temporal **una sola vez**, con botón *Copiar* | ⬜ | `13-secretaria-nuevo-paciente-credenciales.png` |
| SEC-09 | Paciente nuevo inicia sesión | Entrar con las credenciales generadas | Acceso correcto como paciente | ⬜ | `SEC-09.png` |
| SEC-10 | Correo repetido | Registrar un paciente con un correo existente | Error: el correo ya está en uso | ⬜ | `SEC-10.png` |
| SEC-11 ⭐ | Agendar a nombre de un paciente | En *Pacientes* → **Agendar cita**; elegir médico, día y hora; confirmar | La cita queda a nombre del paciente y aparece en el calendario | ⬜ | `14-secretaria-agendar-a-paciente.png` |
| SEC-12 | Agendar sin elegir paciente | Abrir *Agendar* sin paciente seleccionado | Aviso y enlace para ir a *Pacientes* | ⬜ | `SEC-12.png` |
| SEC-13 ⭐ | Sin acceso a administración | Como secretaria, abrir `/dashboard/usuarios` | Pantalla de acceso denegado; el menú no muestra opciones de administrador | ⬜ | `SEC-13.png` |

### 6.4 Médico (DOC) — usuario `dr.perez`

| ID | Caso | Pasos | Resultado esperado | Resultado | Evidencia |
|---|---|---|---|:--:|---|
| DOC-01 ⭐ | Agenda semanal | Entrar a *Mi agenda* | Vista semanal solo con sus citas, con color por estado | ⬜ | `15-doctor-mi-agenda.png` |
| DOC-02 | Navegar semanas | *Anterior*, *Hoy*, *Siguiente* | Cambia la semana correctamente | ⬜ | `DOC-02.png` |
| DOC-03 | Detalle de un día | Elegir un día | Muestra las citas del día con su detalle | ⬜ | `DOC-03.png` |
| DOC-04 ⭐ | Cita futura | Abrir el detalle de una cita futura | No ofrece registrar documentos e indica desde cuándo se habilita | ⬜ | `DOC-04.png` |
| DOC-05 ⭐ | Registrar diagnóstico | En una cita pasada pendiente, tipo *Diagnóstico*, escribir contenido y guardar | Documento guardado y cita **Completada** | ⬜ | `DOC-05.png` |
| DOC-06 ⭐ | Registrar receta | En la misma cita, tipo *Receta* y guardar | Aparece una segunda entrada en el historial; la primera se conserva | ⬜ | `DOC-06.png` |
| DOC-07 ⭐ | Adjuntar PDF al guardar | Guardar un documento adjuntando un PDF válido | Se guarda el documento con su PDF | ⬜ | `16-doctor-documento-pdf.png` |
| DOC-08 | Archivo inválido | Intentar adjuntar un archivo que no es PDF | Error de validación; no se guarda el archivo | ⬜ | `DOC-08.png` |
| DOC-09 | Adjuntar PDF después | En un documento sin PDF, adjuntar uno desde el historial | El PDF queda asociado al documento | ⬜ | `DOC-09.png` |
| DOC-10 | Ver PDF | **Ver PDF** en un documento con archivo | Se abre en una pestaña nueva | ⬜ | `DOC-10.png` |
| DOC-11 | Cita cancelada | Abrir una cita cancelada | No permite registrar documentos | ⬜ | `DOC-11.png` |

### 6.5 Administrador (ADM) — usuario `admin`

| ID | Caso | Pasos | Resultado esperado | Resultado | Evidencia |
|---|---|---|---|:--:|---|
| ADM-01 ⭐ | Panel | Entrar al **Panel** | Indicadores y gráficas (por estado, día, especialidad y médico) con datos | ⬜ | `17-admin-panel.png` |
| ADM-02 | Cambiar el periodo | Probar los botones de periodo y fechas **Desde/Hasta** | Los indicadores y gráficas se recalculan | ⬜ | `ADM-02.png` |
| ADM-03 ⭐ | Listado de usuarios | Abrir *Usuarios*, buscar y filtrar por rol y estado | La lista cambia según búsqueda y filtros; hay paginación | ⬜ | `18-admin-usuarios.png` |
| ADM-04 ⭐ | Crear secretaria | **Nuevo usuario** → rol *Secretaria*, datos, sin contraseña | Se crea y muestra usuario y **contraseña temporal** (una sola vez) | ⬜ | `ADM-04.png` |
| ADM-05 | Crear paciente desde Usuarios | **Nuevo usuario** → rol *Paciente* | Se crea con contraseña temporal | ⬜ | `ADM-05.png` |
| ADM-06 | Rol médico no disponible | Abrir el formulario de nuevo usuario | La lista de roles no incluye *Médico* y hay un enlace a *Médicos* | ⬜ | `ADM-06.png` |
| ADM-07 | Editar usuario | **Editar**, cambiar nombre y guardar | Los cambios se reflejan en la lista | ⬜ | `ADM-07.png` |
| ADM-08 ⭐ | Desactivar usuario | **Desactivar** a la secretaria creada; con su sesión abierta en otra ventana, recargar | La cuenta queda *Inactiva* y la otra ventana pierde el acceso | ⬜ | `ADM-08.png` |
| ADM-09 | Reactivar usuario | **Activar** la cuenta | Vuelve a poder iniciar sesión | ⬜ | `ADM-09.png` |
| ADM-10 ⭐ | Restablecer contraseña | **Restablecer contraseña** | Muestra una contraseña temporal; la anterior deja de funcionar y la nueva sirve | ⬜ | `ADM-10.png` |
| ADM-11 | Protección de la propia cuenta | Mirar la fila del propio administrador | No hay botón *Desactivar* en su propia cuenta | ⬜ | `ADM-11.png` |
| ADM-12 ⭐ | Listado de médicos | Abrir *Médicos* | Lista con estado, especialidad, colegiado y acciones | ⬜ | `19-admin-medicos.png` |
| ADM-13 ⭐ | Nuevo médico con horarios | **Nuevo médico**, completar datos y 2 bloques de horario; guardar | Médico creado; aparece en el listado y en el directorio del paciente | ⬜ | `21-admin-nuevo-medico.png` |
| ADM-14 | Validaciones del formulario | Fin antes del inicio; dos bloques cruzados; campos vacíos | Mensajes de error; no se envía | ⬜ | `ADM-14.png` |
| ADM-15 | Alta atómica | Crear un médico con un usuario ya existente | Error y **no** queda ningún médico ni horario a medias | ⬜ | `ADM-15.png` |
| ADM-16 | Editar médico | **Editar datos**, cambiar la biografía; probar un colegiado ya usado | Se guarda el cambio; el colegiado repetido muestra error | ⬜ | `ADM-16.png` |
| ADM-17 ⭐ | Desactivar médico | Desactivar un médico sin citas pendientes; luego intentar con uno que sí las tenga | El primero queda inactivo y desaparece del directorio; el segundo se rechaza con un mensaje que explica el motivo | ⬜ | `ADM-17.png` |
| ADM-18 ⭐ | Gestionar horarios | **Horarios** → agregar, editar y eliminar un bloque | Los cambios se reflejan en la lista de bloques | ⬜ | `20-admin-horarios-medico.png` |
| ADM-19 | Bloque con citas pendientes | Cambiar o eliminar un bloque que contiene citas pendientes futuras | Se rechaza e indica cuántas citas quedarían sin horario | ⬜ | `ADM-19.png` |
| ADM-20 | Bloque cruzado | Agregar un bloque que se cruza con otro del mismo día | Error de cruce | ⬜ | `ADM-20.png` |
| ADM-21 ⭐ | Crear especialidad | En *Especialidades*, crear una nueva; luego repetir el nombre | La primera se crea; la repetida da error | ⬜ | `22-admin-especialidades.png` |
| ADM-22 | Editar especialidad | Editar el nombre o la descripción | Se actualiza en el listado | ⬜ | `ADM-22.png` |
| ADM-23 | Calendario y agendar | Como admin, abrir *Calendario* y agendar a nombre de un paciente | Funciona igual que para la secretaria | ⬜ | `ADM-23.png` |

### 6.6 Seguridad y reglas transversales (SEG)

| ID | Caso | Pasos | Resultado esperado | Resultado | Evidencia |
|---|---|---|---|:--:|---|
| SEG-01 ⭐ | Documentación Swagger | Abrir `http://localhost:8080/swagger-ui.html` | Se listan los endpoints agrupados, con su descripción | ⬜ | `23-swagger-ui.png` |
| SEG-02 ⭐ | Endpoint protegido sin token | En Swagger o Postman, llamar `GET /api/v1/specialties` sin token | `401` con el cuerpo de error en JSON | ⬜ | `SEG-02.png` |
| SEG-03 ⭐ | Token de otro rol | Con el token de un paciente, llamar `GET /api/v1/users` | `403` | ⬜ | `SEG-03.png` |
| SEG-04 | Límite de intentos | Con el límite activado, enviar más de 20 logins en un minuto | `429` con encabezado `Retry-After` | ⬜ | `SEG-04.png` |
| SEG-05 | Doble reserva | Intentar reservar el mismo horario dos veces (dos ventanas o Postman) | La segunda recibe conflicto (`409`) | ⬜ | `SEG-05.png` |
| SEG-06 | Documentos de otro paciente | Con `paciente1`, pedir los documentos de una cita de otro paciente | `403` | ⬜ | `SEG-06.png` |
| SEG-07 ⭐ | Contraseñas cifradas | En MySQL: `SELECT username, password FROM users;` | Las contraseñas son hashes BCrypt (`$2a$…`), nunca texto plano | ⬜ | `SEG-07.png` |
| SEG-08 | CORS | Usar el panel en `localhost:4200` | Las peticiones al backend funcionan sin errores de CORS en la consola | ⬜ | `SEG-08.png` |

---

## 7. Defectos detectados y corregidos durante el desarrollo

Registro de los problemas encontrados al probar el sistema a lo largo del desarrollo y su corrección. Muestra que las pruebas se hicieron de forma continua.

| ID | Defecto detectado | Causa | Corrección |
|---|---|---|---|
| DEF-01 | El backend no arrancaba: `Schema-validation: wrong column type` en `schedules.day_of_week` | El script SQL usaba `TINYINT` y la entidad esperaba `INTEGER` | Columna cambiada a `INT` en `schema.sql` |
| DEF-02 | Error de compilación en la configuración de Swagger | Importación incorrecta de la clase `Info` | Se usó `io.swagger.v3.oas.models.info.Info` |
| DEF-03 | Los usuarios semilla no podían iniciar sesión | El hash BCrypt del script no correspondía a la contraseña | Se generó el hash con el codificador real de la aplicación |
| DEF-04 | Error al firmar tokens: clave de longitud insuficiente | Secreto JWT de menos de 256 bits | Secreto de al menos 32 caracteres, obligatorio por configuración |
| DEF-05 | Una cita cancelada seguía ocupando su horario | La restricción única incluía las citas canceladas | Columna generada `active_slot` (`NULL` si está cancelada) dentro de la restricción única |
| DEF-06 | Se podía reservar un horario fuera de la agenda del médico | La reserva solo comprobaba que no hubiera otra cita | La reserva valida contra el motor de disponibilidad |
| DEF-07 | Sin token, la API respondía `403` en lugar de `401` | Comportamiento por defecto de Spring Security | Manejadores propios: `401` sin sesión, `403` sin permiso, ambos en JSON |
| DEF-08 | Se podía registrar un diagnóstico en una cita cancelada o futura | Faltaban reglas en el servicio | Reglas de estado y fecha; respuesta `409` |
| DEF-09 | Un alta de médico podía dejar el usuario creado aunque fallaran los horarios | Operaciones separadas | Alta en una sola transacción, con validación previa de cruces |
| DEF-10 | La agenda mostraba el día siguiente después de las 6 p. m. | Fechas calculadas en UTC en el navegador | Cálculo con la fecha local |
| DEF-11 | Los médicos desactivados seguían apareciendo en el directorio | El listado no filtraba por estado | Listado público solo con médicos activos |
| DEF-12 | Una cuenta desactivada seguía operando con un token vigente | El filtro JWT no consultaba el estado de la cuenta | Verificación de cuenta activa en cada petición |
| DEF-13 | Cuerpos de petición o parámetros inválidos devolvían `500` | Excepciones no controladas | Manejadores globales que responden `400` con el detalle |

## 8. Defectos encontrados en esta ejecución

*(Complete durante la ejecución de la sección 6. Si no se encontró ninguno, indíquelo.)*

| ID | Caso | Descripción | Gravedad (alta/media/baja) | Estado | Evidencia |
|---|---|---|---|---|---|
| | | | | | |

## 9. Resumen de resultados

| Sección | Casos | ✅ Pasaron | ❌ Fallaron | ➖ No aplica |
|---|---:|---:|---:|---:|
| 6.1 Autenticación (AUT) | 12 | | | |
| 6.2 Paciente (PAC) | 18 | | | |
| 6.3 Secretaria (SEC) | 13 | | | |
| 6.4 Médico (DOC) | 11 | | | |
| 6.5 Administrador (ADM) | 23 | | | |
| 6.6 Seguridad (SEG) | 8 | | | |
| **Total manuales** | **85** | | | |
| API automatizadas (Postman) | 113 | | | |

Cobertura de requisitos: cada perfil y cada módulo del sistema tiene al menos un caso ⭐ en la sección 6.

## 10. Limitaciones de la validación

- **No hay pruebas unitarias ni de integración automatizadas en el código** (JUnit/Karma). La verificación automática se limita a la colección de Postman sobre la API; la interfaz se valida manualmente. La incorporación de pruebas automatizadas está planificada.
- No se realizaron pruebas de carga, rendimiento ni accesibilidad.
- La **app móvil** no está cubierta por este reporte.
- Las pruebas se ejecutaron en un único navegador y sistema operativo (ver sección 3).
- El límite de intentos por IP se validó con configuración reducida; su comportamiento con varias instancias del backend no se probó (funciona en memoria).

## 11. Conclusiones

*(Complete al terminar: porcentaje de casos aprobados, defectos pendientes, y si el sistema cumple los requisitos de esta entrega.)*

## 12. Anexo: lista de capturas

Guarde cada captura en `docs/capturas/` con el nombre indicado. Las numeradas del `01` al `26` se reutilizan en el [manual del sistema](manual-del-sistema.md); las de formato `AUT-02.png`, `PAC-10.png`, etc. son evidencia propia de cada caso. Consulte [`capturas/README.md`](capturas/README.md) para la lista completa y recomendaciones.
