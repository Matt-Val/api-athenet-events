package com.athenet.events.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.athenet.events.model.OutboxMessage;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, String> {
    // SKIP LOCKED permite varios publicadores, sin adelantar revisiones del mismo agregado/tipo.
    @Query(value = """
        SELECT o.* FROM domain_outbox o
        WHERE o.published_at IS NULL AND o.next_attempt_at <= CURRENT_TIMESTAMP
          AND NOT EXISTS (SELECT 1 FROM domain_outbox earlier
              WHERE earlier.aggregate_id = o.aggregate_id
                AND earlier.revision < o.revision AND earlier.published_at IS NULL)
        ORDER BY o.occurred_at, o.id
        LIMIT 1 FOR UPDATE OF o SKIP LOCKED
        """, nativeQuery = true)
    Optional<OutboxMessage> lockNextDue();
    @Query("SELECT coalesce(max(o.revision), 0) FROM OutboxMessage o WHERE o.aggregateId = :internalId")
    long lastRevision(@org.springframework.data.repository.query.Param("internalId") String internalId);
}
