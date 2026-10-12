<p align="center">
  <img src="docs/img/banner.png" alt="Athenet Banner" width="100%">
</p>

<p align="center">
Microservicio de eventos de <strong>Athenet</strong>: expone el catálogo público de eventos deportivos institucionales y el CRUD administrativo (creación, edición, publicación y cancelación) consumido por el panel de administración.
</p>

<h4 align="center"><strong>
Athenet ORG - <a href="https://github.com/fabetabilo/web-athenet">Athenet Público</a>
</br>
<a href="#control-de-acceso-y-roles">Control de Acceso y Roles</a> - <a href="#desarrollo-local">Desarrollo Local</a> - <a href="#despliegue">Despliegue</a>
</strong></h4>

### Stack

<div align="center">

[![Java](https://img.shields.io/badge/Java_25-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_4-%236DB33F.svg?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-%23336791.svg?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Microsoft Azure](https://img.shields.io/badge/Microsoft_Entra_ID-%230078D4.svg?style=for-the-badge&logo=microsoftazure&logoColor=white)](https://learn.microsoft.com/entra/identity/)
[![Docker](https://img.shields.io/badge/Docker-%232496ED.svg?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-%232088FF.svg?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/features/actions)

</div>

* **Framework backend:** [Spring Boot 4.1](https://spring.io/projects/spring-boot) (Spring Web MVC, Spring Data JPA).
* **Lenguaje:** Java 25.
* **Persistencia:** PostgreSQL, mapeado con Hibernate/JPA (`spring-boot-starter-data-jpa`).
* **Autenticación:** `spring-boot-starter-oauth2-resource-server`, validando JWT emitidos por Microsoft Entra ID.
* **Validación:** Bean Validation (`spring-boot-starter-validation`) sobre las entidades expuestas en el CRUD admin.
* **Build:** Maven (via wrapper `./mvnw`, no requiere instalación global).
* **Contenerización y despliegue:** Docker (multi-stage build) + GitHub Actions (test → build de imagen → push a GHCR → deploy por SSH).

### ms-events

Este microservicio centraliza la información de eventos deportivos institucionales de Athenet: torneos, competencias y actividades organizadas por las casas de estudio adscritas. Expone dos superficies bien diferenciadas:

* **Pública (`/api/public/events`):** catálogo de eventos publicados, próximos eventos, evento destacado para el hero countdown, calendario de próximos días y detalle por `internalId`. Sin autenticación.
* **Administrativa (`/api/admin/events`, `/api/admin/event-types`):** alta, edición, baja y listado completo de eventos (incluye borradores y cancelados), reservada al rol `DIRECTOR`.

| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/public/events` | Público | Catálogo de eventos publicados |
| `GET` | `/api/public/events/next` | Público | Próximos 10 eventos publicados |
| `GET` | `/api/public/events/featured` | Público | Evento individual más próximo (hero countdown) |
| `GET` | `/api/public/events/upcoming` | Público | Calendario paginado (`daysAhead`, `page`, `size`) |
| `GET` | `/api/public/events/{internalId}` | Público | Detalle de un evento |
| `GET` | `/api/admin/event-types` | `DIRECTOR` | Tipos de evento con `label` y `requiresMatchup` |
| `GET` | `/api/admin/events` | `DIRECTOR` | Listado completo (incluye `DRAFT`/`CANCELLED`) |
| `GET` | `/api/admin/events/{internalId}` | `DIRECTOR` | Detalle en cualquier estado |
| `POST` | `/api/admin/events` | `DIRECTOR` | Crear evento (`201` + `Location`) |
| `PUT` | `/api/admin/events/{internalId}` | `DIRECTOR` | Actualizar evento (`internalId` inmutable) |
| `DELETE` | `/api/admin/events/{internalId}` | `DIRECTOR` | Eliminar evento |

El consumidor de `/api/admin/**` es el BFF (contrato en `web-athenet-org/docs/contrato-bff-events.md`). El body de escritura es **plano, solo IDs**:

* **Inicio y término:** `eventDate` + `startTime` y `endDate` + `endTime` (`yyyy-MM-dd` / `HH:mm`, sin zona). Un evento puede cruzar la medianoche o durar varios días.
* **Deporte:** `sportId` es el `Deporte.id` de ms-institution. Es obligatorio solo en partidos.
* **Partido (`MATCH`):** `teamOneId`, `teamTwoId` (distintos) y delegado (`delegateOid` + `delegateDisplayName`) obligatorios; sin organizador.
* **Otro tipo:** `organizerInstitutionId` y `organizerCampusId` obligatorios, `organizerTeamId` opcional; sin equipos.

ms-events guarda las referencias como IDs sueltos: la integridad contra ms-institution (sede ∈ institución, equipo ∈ sede, deporte y división) la valida el BFF.

### Control de Acceso y Roles

El acceso al panel administrativo está protegido mediante autenticación federada con **Microsoft Entra ID** (OAuth 2.0 / JWT). `SecurityConfig` valida el token contra el `issuer-uri` configurado y mapea el claim `roles` del JWT a `GrantedAuthority` de Spring Security (`ROLE_<rol>`), lo que le permite a `/api/admin/**` exigir `hasRole("DIRECTOR")`.

* **`DIRECTOR`:** único rol habilitado para operar el CRUD de eventos (crear, editar, eliminar y ver borradores/cancelados).
* **Sin token / sin rol `DIRECTOR`:** acceso denegado (`401` sin token válido, `403` con token válido pero sin el rol) a todo `/api/admin/**`.
* **Rutas públicas:** `/api/public/**` no requiere autenticación.

Las respuestas de error siguen un formato consistente (`ApiError`: `timestamp`, `status`, `error`, `code`, `message`, `fieldErrors`), manejado centralmente por `GlobalExceptionHandler`. Cada `fieldError` trae el nombre exacto de la clave del body (`field`), un `code` y un `message`:

| HTTP | `code` | Cuándo |
| :--- | :--- | :--- |
| `400` | `MALFORMED_REQUEST` | JSON inválido, fecha u hora con otro formato, campo desconocido u objeto anidado |
| `404` | `EVENT_NOT_FOUND` | No existe el `internalId` de la ruta |
| `409` | `INTERNAL_ID_TAKEN` | `internalId` duplicado |
| `422` | `VALIDATION_FAILED` | Obligatorio faltante o regla del evento incumplida (`END_BEFORE_START`, `SAME_TEAM`, `ORGANIZER_NOT_ALLOWED`, `MATCHUP_NOT_ALLOWED`, `IMMUTABLE`…) |

### Desarrollo Local

#### 1. Verificación de dependencias del entorno

* **Java:** JDK 25 (el `pom.xml` fija `java.version=25`).
* **PostgreSQL:** instancia local o accesible, con una base `athenet_events_db`.
* **Maven:** no hace falta instalarlo, el repo incluye el wrapper (`./mvnw`).

Verifica la versión instalada:

```bash
java -version
```

#### 2. Clonar e instalar dependencias

```bash
./mvnw dependency:go-offline
```

#### 3. Configuración del entorno (`application-local.properties`)

El microservicio requiere los datos del App Registration de Athenet en Microsoft Entra ID. Copiá la plantilla:

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

**Nunca subas `application-local.properties` al repositorio** (ya está en `.gitignore`).

| Parámetro | Descripción |
| :--- | :--- |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | Issuer del tenant de Entra ID (`https://login.microsoftonline.com/{tenantId}/v2.0`). |
| `spring.security.oauth2.resourceserver.jwt.audiences[0..1]` | Application (Client) ID del App Registration, en ambos formatos (`{clientId}` y `api://{clientId}`). |

La conexión a base de datos y el puerto se configuran en `application.properties` (`spring.datasource.url`, `.username`, `.password`, `server.port` — por defecto `8081`).

#### 4. Base de datos

El esquema lo crea Hibernate (`ddl-auto=update`). Como `update` no borra columnas ni quita `NOT NULL`, **tras el cambio de modelo de eventos (deporte, organizador, horario y delegado) la tabla vieja debe borrarse una vez** antes de arrancar, en local y en producción. Los datos son de demo:

```sql
DROP TABLE IF EXISTS event_photos, events CASCADE;
```

Al arrancar, la tabla se recrea con el esquema nuevo. Luego se puede cargar el seed manual `db/data.sql` (no versionado) en pgAdmin o `psql`.

#### 5. Servidor de desarrollo

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8081/`.

#### 6. Compilación para producción

```bash
./mvnw clean package -DskipTests
```

El artefacto se genera en `target/events-0.0.1-SNAPSHOT.jar`.

#### 7. Ejecución de tests

```bash
./mvnw test
```

### Despliegue

El microservicio se dockeriza con un `Dockerfile` multi-stage (build con JDK 25, runtime con JRE 25) y se despliega mediante `.github/workflows/deploy.yml`:

1. **Test:** corre `./mvnw test` contra un servicio de PostgreSQL levantado en el propio job.
2. **Build & Push:** builda la imagen y la publica en GitHub Container Registry (`ghcr.io`).
3. **Deploy:** se conecta por SSH a la instancia de aplicación y levanta el contenedor apuntando a la base de datos, usando secrets del repositorio (`EC2_APP_HOST`, `EC2_SSH_USER`, `EC2_SSH_KEY`, `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_ISSUER_URI`).

### Estructura del Proyecto

```text
src/main/java/com/athenet/events/
├── config/           # SecurityConfig (OAuth2 Resource Server, CORS, roles)
├── controller/        # PublicEventController, AdminEventController
├── dto/               # EventRequest, EventResponse, EventTypeResponse
├── exception/          # GlobalExceptionHandler, ApiError, FieldError y excepciones de dominio
├── mapper/            # EventMapper
├── model/             # Event, EventType, EventStatus
├── repository/        # EventRepository (Spring Data JPA)
└── service/            # EventService (CRUD) y EventValidator (reglas propias del evento)
```
