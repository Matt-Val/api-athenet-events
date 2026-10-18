package com.athenet.events.model;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Mensaje inmutable del dominio y estado de publicación recuperable. */
@Entity
@Table(name = "domain_outbox", uniqueConstraints = @UniqueConstraint(
        name = "uq_domain_outbox_revision", columnNames = {"aggregate_id", "revision"}))
@Getter @Setter
public class OutboxMessage {
    @Id @Column(length = 36) private String id;
    @Column(name = "routing_key", nullable = false, length = 100) private String routingKey;
    @Column(name = "aggregate_id", nullable = false, length = 255) private String aggregateId;
    @Column(nullable = false) private long revision;
    @Column(nullable = false, columnDefinition = "text") private String payload;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(nullable = false) private int attempts;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "last_error", length = 100) private String lastError;
}
