package com.chubb.claims;

import static org.assertj.core.api.Assertions.assertThat;

import com.chubb.claims.claim.*;
import com.chubb.claims.event.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {"claims.events.enabled=true", "claims.events.poll-delay-ms=100"})
@ActiveProfiles("test")
@EmbeddedKafka(
    kraft = true,
    partitions = 3,
    topics = "claims.lifecycle.v1",
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class KafkaIntegrationTest {
  @Autowired EmbeddedKafkaBroker broker;
  @Autowired ClaimService claims;
  @Autowired OutboxRepository outbox;
  @Autowired ObjectMapper json;

  @Test
  void committedOutboxEventReachesBrokerAndIsMarkedPublished() throws Exception {
    var consumerProperties =
        KafkaTestUtils.consumerProps("claims-test-" + UUID.randomUUID(), "false", broker);
    try (var consumer =
        new KafkaConsumer<String, String>(
            consumerProperties, new StringDeserializer(), new StringDeserializer())) {
      broker.consumeFromAnEmbeddedTopic(consumer, "claims.lifecycle.v1");
      var claim =
          claims.create(
              new ClaimDtos.Create(
                  ClaimType.MOTOR,
                  Market.SG,
                  "Private claimant",
                  "Private incident narrative",
                  LocalDate.now().minusDays(1),
                  new BigDecimal("100.00")));
      var record =
          KafkaTestUtils.getSingleRecord(consumer, "claims.lifecycle.v1", Duration.ofSeconds(30));
      var envelope = json.readTree(record.value());
      assertThat(record.key()).isEqualTo(claim.id().toString());
      assertThat(envelope.get("claimId").asText()).isEqualTo(claim.id().toString());
      assertThat(envelope.get("eventType").asText()).isEqualTo("ClaimCreated");
      assertThat(envelope.get("schemaVersion").asInt()).isEqualTo(1);
      assertThat(envelope.get("aggregateVersion").asLong()).isZero();
      assertThat(record.value()).doesNotContain("Private claimant", "Private incident narrative");
      UUID eventId = UUID.fromString(envelope.get("eventId").asText());
      // Kafka acknowledgement precedes the publisher's database commit.
      long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
      while (outbox.findById(eventId).orElseThrow().getPublishedAt() == null
          && System.nanoTime() < deadline) {
        Thread.sleep(50);
      }
      var published = outbox.findById(eventId).orElseThrow();
      assertThat(published.getPublishedAt()).isNotNull();
      assertThat(published.getAttempts()).isEqualTo(1);
      assertThat(claims.history(claim.id()).get(0).eventId()).isEqualTo(eventId);
    }
  }
}
