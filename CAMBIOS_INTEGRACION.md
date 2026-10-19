# Cambios de integración — Eventos

Fecha: 18 de octubre de 2026. Documento de entrega para el responsable de este repositorio.

## Objetivo

Recibir agendas confirmadas desde Competencia, mantener una copia pública del encuentro y producir avisos para Notificaciones. La edición del encuentro sigue perteneciendo a Competencia.

## Qué cambió y por qué

### Consumidor de agendas

Se añadieron `MatchScheduleEnvelope`, `MatchScheduleListener` y `MatchProjectionService`. Escuchan la cola durable `eventos.partidos`, enlazada a `athenet.topic` por `partido.actualizado`. La cola tiene DLQ `eventos.partidos.dlq` en `athenet.dlx` y clave de errores exclusiva.

El listener lee JSON y valida el contrato sin depender de clases Java del productor. La proyección crea o actualiza por `competitionMatchId`, usando `PARTIDO-{matchId}` como identificador de negocio. Comprueba equipos diferentes, término posterior al inicio y consistencia de la competencia de origen. Ignora revisiones de agenda repetidas o antiguas. Si llega primero una cancelación, guarda una copia cancelada para impedir que una publicación antigua la reactive.

Esto completa el tramo asíncrono Competencia → Eventos sin agregar llamadas HTTP entre ambos para crear cada copia.

### Origen persistido y protección del CRUD

`Event` incorpora `competitionId`, `competitionMatchId`, `scheduleVersion`, `notificationVersion` y `rowVersion` para bloqueo optimista. El origen también se expone en `EventResponse`. El repositorio incorpora búsqueda con bloqueo por encuentro de origen y unicidad de esa referencia.

`EventService` rechaza edición y eliminación directa de copias con HTTP 409 `COMPETITION_EVENT_READ_ONLY`. Se verifica el origen guardado, por lo que cambiar el tipo en el cuerpo no elude la restricción. Los conflictos de escritura optimista se traducen a 409 `CONCURRENT_CHANGE`.

Se retiró `@AllArgsConstructor` de `Event` porque no se usaba y generaba el diagnóstico observado en el editor sobre `photos`. Se conservan el constructor vacío de JPA y la inicialización de la colección. Después de ese ajuste se verificó `clean compile` con resultado satisfactorio.

### Avisos versionados mediante outbox

Se reemplazó `AfterCommitSender` por `OutboxMessage`, repositorio, escritor, publicador y scheduler. Se retiraron los DTOs anteriores `EventPublishedMessage` y `EventPublishedEnvelope`, sustituidos por `EventNotificationData` y `EventNotificationEnvelope`.

`EventPublishedPublisher` compara el estado anterior con el nuevo: publica `evento.publicado`, `evento.modificado` o `evento.cancelado` cuando corresponde. Guardar sin un cambio relevante no produce un aviso nuevo. La revisión se obtiene del historial de outbox por `internalId`, de modo que no se reinicia entre tipos de aviso ni al borrar y reutilizar ese identificador.

El JSON se guarda en la transacción del evento. El envío posterior usa mensajes persistentes, confirms, mandatory/returns y reintentos; conserva el mensaje original. La selección de pendientes respeta revisiones anteriores del mismo evento. El scheduler está habilitado por defecto, con intervalo de 2 segundos y hasta 50 mensajes por ciclo.

Los avisos de agenda no sustituyen el mensaje de resultados: Eventos no vuelve a emitir `resultado.registrado`, que pertenece a Competencia.

## Archivos y pruebas

- `messaging/MatchSchedule*` y `service/MatchProjectionService.java`: entrada de agendas.
- `messaging/Outbox*`, `model/OutboxMessage.java` y repositorio: publicación durable.
- `EventPublishedPublisher`, `EventNotification*`: avisos de ciclo de vida del evento.
- Modelo, DTO, mapper, repositorio, servicio y excepciones: origen y control de edición/concurrencia.
- `RabbitConfig`, aplicación y propiedades: topología, scheduling y vhost configurable.
- Nuevas pruebas de proyección, outbox y contrato con el productor de Competencia; pruebas existentes de servicio y errores actualizadas. El smoke test usa dobles de persistencia/mensajería para no exigir infraestructura externa. README actualizado.

Pasaron 53 pruebas seleccionadas de servicios, HTTP/seguridad, publicador, outbox y contrato del productor real de agenda. Se excluyeron pruebas de repositorio que necesitan conexiones. El último cambio del constructor se verificó con compilación limpia.

## Migraciones y configuración

Aplicar en la base/esquema exclusivo de Eventos, en orden:

1. [001_domain_outbox.sql](db/migration/001_domain_outbox.sql): outbox e índice de pendientes.
2. [002_event_origin.sql](db/migration/002_event_origin.sql): origen, versiones e índice único por encuentro.

Son migraciones SQL explícitas; no se añadió un ejecutor automático. Los eventos existentes no reciben un origen ficticio. No se debe purgar el historial de outbox sin preservar la secuencia de revisiones.

Configurar PostgreSQL, Entra y las propiedades RabbitMQ del entorno; `RABBITMQ_VHOST` permite seleccionar el vhost compartido. Mantener habilitados el listener de agenda y el scheduler de outbox. No incluir las configuraciones privadas de Entra en el ZIP.

## Validación local conectada y límites

Una agenda sintética creó `PARTIDO-900001`, produjo `evento.publicado` y fue procesada por Notificaciones. Posteriormente una confirmación auténtica desde BFF y Competencia creó `PARTIDO-1`, PUBLISHED, revisión 1 y delegado real asignado.

La protección HTTP de edición/borrado y las reprogramaciones/cancelaciones están implementadas y cubiertas por pruebas seleccionadas, pero no se completó su ensayo conectado en el cierre mínimo. Tampoco se probó caída del broker durante publicación ni se aplicaron estos SQL a RDS.

## Docker Hub — 2026-10-19

Se preparó la publicación de imágenes en Docker Hub y el despliegue por SSH usando la etiqueta SHA. Las credenciales del registro se configuran mediante `DOCKERHUB_USERNAME` (variable) y `DOCKERHUB_TOKEN` (secret). La preparación no incluye publicación ni despliegue ejecutados.
