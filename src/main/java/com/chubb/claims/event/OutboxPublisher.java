package com.chubb.claims.event;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name="claims.events.enabled",havingValue="true",matchIfMissing=true)
public class OutboxPublisher {
    private static final Logger log=LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository outbox;
    private final KafkaTemplate<String,String> kafka;
    private final Clock clock;
    private final String topic;
    public OutboxPublisher(OutboxRepository outbox,KafkaTemplate<String,String> kafka,Clock clock,
        @Value("${claims.events.topic}") String topic) { this.outbox=outbox; this.kafka=kafka; this.clock=clock; this.topic=topic; }
    @Scheduled(fixedDelayString="${claims.events.poll-delay-ms:5000}")
    @Transactional
    public void publishPending() {
        for (OutboxEvent event:outbox.findTop20ByPublishedAtIsNullOrderByOccurredAtAsc()) {
            try {
                kafka.send(topic,event.getClaimId().toString(),event.getPayload()).get(15,TimeUnit.SECONDS);
                event.published(clock.instant());
                log.info("Lifecycle event published eventId={} type={}",event.getId(),event.getEventType());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); event.failed(); return;
            } catch (Exception e) {
                event.failed(); log.warn("Lifecycle publication deferred eventId={} type={} failure={}",event.getId(),event.getEventType(),e.getClass().getSimpleName());
                break;
            }
        }
    }
}
