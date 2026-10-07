package com.chubb.claims;

import static org.assertj.core.api.Assertions.*;

import com.chubb.claims.assignment.AssignmentService;
import com.chubb.claims.claim.*;
import com.chubb.claims.event.OutboxRepository;
import com.chubb.claims.exposure.ExposureService;
import com.chubb.claims.workflow.WorkflowService;
import com.chubb.claims.workload.WorkloadService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(properties = "claims.events.enabled=false")
@EnabledIfSystemProperty(named = "postgresIT", matches = "true")
@Testcontainers
class PostgresIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.10-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", postgres::getJdbcUrl);
    properties.add("spring.datasource.username", postgres::getUsername);
    properties.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired ClaimService claims;
  @Autowired AssignmentService assignment;
  @Autowired WorkflowService workflow;
  @Autowired ExposureService exposure;
  @Autowired WorkloadService workload;
  @Autowired OutboxRepository outbox;

  @Test
  void migrationsAndCommittedClaimFlowWorkOnPostgres() {
    var c =
        claims.create(
            new ClaimDtos.Create(
                ClaimType.PROPERTY,
                Market.AU,
                "Test",
                "Storm",
                LocalDate.of(2026, 10, 1),
                new BigDecimal("100.00")));
    c = assignment.assign(c.id(), new ClaimDtos.Assignment(c.version(), "officer-1", "Officer"));
    c = workflow.review(c.id(), new ClaimDtos.Version(c.version()));
    c = workflow.request(c.id(), new ClaimDtos.RequestInformation(c.version(), "Evidence?"));
    c =
        workflow.provide(
            c.id(),
            c.informationRequests().get(0).id(),
            new ClaimDtos.ProvideInformation(c.version(), "Supplied"));
    c =
        workflow.assess(
            c.id(),
            new ClaimDtos.Assessment(
                c.version(), new BigDecimal("100.00"), new BigDecimal("90.00"), "Covered"));
    assertThat(exposure.get().openClaimCount()).isEqualTo(1);
    c = workflow.settle(c.id(), new ClaimDtos.Version(c.version()));
    assertThat(claims.get(c.id()).status()).isEqualTo(ClaimStatus.SETTLED);
    assertThat(exposure.get().openClaimCount()).isZero();
    assertThat(workload.get().performance().settledCount()).isEqualTo(1);
    assertThat(outbox.count()).isEqualTo(7);
    assertThat(claims.history(c.id())).hasSize(7);
  }
}
