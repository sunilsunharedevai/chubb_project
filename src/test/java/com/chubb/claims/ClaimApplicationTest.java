package com.chubb.claims;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chubb.claims.assignment.AssignmentService;
import com.chubb.claims.claim.*;
import com.chubb.claims.event.OutboxRepository;
import com.chubb.claims.exposure.ExposureService;
import com.chubb.claims.workflow.WorkflowService;
import com.chubb.claims.workload.WorkloadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClaimApplicationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired ClaimService claims;
  @Autowired AssignmentService assignment;
  @Autowired WorkflowService workflow;
  @Autowired ExposureService exposure;
  @Autowired WorkloadService workload;
  @Autowired OutboxRepository outbox;

  private ClaimDtos.Detail create(String amount) {
    return claims.create(
        new ClaimDtos.Create(
            ClaimType.MOTOR,
            Market.SG,
            "Test claimant",
            "Collision",
            LocalDate.of(2026, 10, 1),
            new BigDecimal(amount)));
  }

  private ClaimDtos.Detail review(ClaimDtos.Detail c) {
    c = assignment.assign(c.id(), new ClaimDtos.Assignment(c.version(), "officer-1", "Officer"));
    return workflow.review(c.id(), new ClaimDtos.Version(c.version()));
  }

  @Test
  void apiCreateRetrieveAndValidation() throws Exception {
    String body =
        """
{"claimType":"PROPERTY","market":"AU","claimantName":"Test claimant","incidentDescription":"Storm damage","incidentDate":"2026-10-01","estimatedLiability":500.00}
""";
    var result =
        mvc.perform(
                post("/api/claims")
                    .header("X-Correlation-ID", "test-request")
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(header().string("X-Correlation-ID", "test-request"))
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.version").value(0))
            .andExpect(jsonPath("$.currency").value("USD"))
            .andReturn();
    var id = json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    mvc.perform(get("/api/claims/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.claimType").value("PROPERTY"));
    mvc.perform(post("/api/claims").contentType("application/json").content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void apiConflictsNotFoundAndPaginationAreMeaningful() throws Exception {
    var c = create("100.00");
    mvc.perform(
            post("/api/claims/" + c.id() + "/settlement")
                .contentType("application/json")
                .content("{\"expectedVersion\":0}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CLAIM_CONFLICT"));
    mvc.perform(get("/api/claims/" + UUID.randomUUID())).andExpect(status().isNotFound());
    mvc.perform(get("/api/claims").param("size", "101")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/claims").param("unassigned", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void fullInformationApprovalSettlementFlowPersistsHistoryAndEvents() {
    var c = review(create("1000.00"));
    c = workflow.request(c.id(), new ClaimDtos.RequestInformation(c.version(), "Repair estimate?"));
    UUID requestId = c.informationRequests().get(0).id();
    c =
        workflow.provide(
            c.id(),
            requestId,
            new ClaimDtos.ProvideInformation(c.version(), "Repair estimate supplied"));
    c =
        workflow.assess(
            c.id(),
            new ClaimDtos.Assessment(
                c.version(), new BigDecimal("900.00"), new BigDecimal("800.00"), "Covered loss"));
    c = workflow.settle(c.id(), new ClaimDtos.Version(c.version()));
    assertThat(claims.get(c.id()).status()).isEqualTo(ClaimStatus.SETTLED);
    assertThat(claims.history(c.id())).hasSize(7);
    assertThat(outbox.count()).isEqualTo(7);
    assertThat(outbox.findAll())
        .allSatisfy(
            e ->
                assertThat(e.getPayload())
                    .doesNotContain("Test claimant", "Repair estimate supplied"));
  }

  @Test
  void staleVersionPreventsSecondOfficerTakingClaim() {
    var c = create("100.00");
    assignment.assign(c.id(), new ClaimDtos.Assignment(c.version(), "officer-1", "First"));
    assertThatThrownBy(
            () ->
                assignment.assign(
                    c.id(), new ClaimDtos.Assignment(c.version(), "officer-2", "Second")))
        .isInstanceOf(com.chubb.claims.common.DomainException.class)
        .hasMessageContaining("changed");
  }

  @Test
  void exposureIncludesApprovedButExcludesSettledAndRejectedAndWorkloadCountsDecisions() {
    create("100.00");
    var approved = review(create("200.00"));
    workflow.assess(
        approved.id(),
        new ClaimDtos.Assessment(
            approved.version(), new BigDecimal("250.00"), new BigDecimal("200.00"), "Covered"));
    var settled = review(create("300.00"));
    settled =
        workflow.assess(
            settled.id(),
            new ClaimDtos.Assessment(
                settled.version(), new BigDecimal("300.00"), new BigDecimal("200.00"), "Covered"));
    workflow.settle(settled.id(), new ClaimDtos.Version(settled.version()));
    var rejected = review(create("400.00"));
    workflow.reject(rejected.id(), new ClaimDtos.Rejection(rejected.version(), "Not covered"));
    var totals = exposure.get();
    assertThat(totals.openClaimCount()).isEqualTo(2);
    assertThat(totals.totalOutstandingExposure()).isEqualByComparingTo("350.00");
    var team = workload.get();
    assertThat(team.unassignedCount()).isEqualTo(1);
    assertThat(team.performance().settledCount()).isEqualTo(1);
    assertThat(team.performance().rejectedCount()).isEqualTo(1);
  }

  @Test
  void emptyExposureIsZero() {
    assertThat(exposure.get().totalOutstandingExposure()).isEqualByComparingTo("0");
    assertThat(exposure.get().openClaimCount()).isZero();
  }
}
