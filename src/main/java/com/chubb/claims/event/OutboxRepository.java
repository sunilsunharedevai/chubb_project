package com.chubb.claims.event;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OutboxRepository extends JpaRepository<OutboxEvent,UUID> {
    List<OutboxEvent> findTop20ByPublishedAtIsNullOrderByOccurredAtAsc();
}
