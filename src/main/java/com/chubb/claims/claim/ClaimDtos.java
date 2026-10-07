package com.chubb.claims.claim;

import com.chubb.claims.workflow.InformationRequest;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class ClaimDtos {
  private ClaimDtos() {}

  public record Create(
      @NotNull ClaimType claimType,
      @NotNull Market market,
      @NotBlank @Size(max = 200) String claimantName,
      @NotBlank @Size(max = 4000) String incidentDescription,
      @NotNull @PastOrPresent LocalDate incidentDate,
      @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2)
          BigDecimal estimatedLiability) {}

  public record Version(@NotNull @PositiveOrZero Long expectedVersion) {}

  public record Assignment(
      @NotNull @PositiveOrZero Long expectedVersion,
      @NotBlank @Size(max = 100) String officerId,
      @NotBlank @Size(max = 200) String officerName) {}

  public record RequestInformation(
      @NotNull @PositiveOrZero Long expectedVersion, @NotBlank @Size(max = 255) String question) {}

  public record ProvideInformation(
      @NotNull @PositiveOrZero Long expectedVersion, @NotBlank @Size(max = 4000) String response) {}

  public record Assessment(
      @NotNull @PositiveOrZero Long expectedVersion,
      @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2)
          BigDecimal estimatedLiability,
      @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2)
          BigDecimal approvedSettlementAmount,
      @NotBlank @Size(max = 2000) String reason) {}

  public record Rejection(
      @NotNull @PositiveOrZero Long expectedVersion, @NotBlank @Size(max = 2000) String reason) {}

  public record InformationView(
      UUID id,
      String question,
      InformationRequest.Status status,
      String response,
      Instant requestedAt,
      Instant providedAt) {
    static InformationView from(InformationRequest r) {
      return new InformationView(
          r.getId(),
          r.getQuestion(),
          r.getStatus(),
          r.getResponse(),
          r.getRequestedAt(),
          r.getProvidedAt());
    }
  }

  public record Detail(
      UUID id,
      String claimNumber,
      ClaimType claimType,
      Market market,
      String claimantName,
      String incidentDescription,
      LocalDate incidentDate,
      Instant reportedAt,
      Instant updatedAt,
      ClaimStatus status,
      String assignedOfficerId,
      String assignedOfficerName,
      String currency,
      BigDecimal estimatedLiability,
      BigDecimal approvedSettlementAmount,
      String decisionReason,
      long version,
      List<InformationView> informationRequests) {
    public static Detail from(Claim c) {
      return new Detail(
          c.getId(),
          c.getClaimNumber(),
          c.getClaimType(),
          c.getMarket(),
          c.getClaimantName(),
          c.getIncidentDescription(),
          c.getIncidentDate(),
          c.getReportedAt(),
          c.getUpdatedAt(),
          c.getStatus(),
          c.getAssignedOfficerId(),
          c.getAssignedOfficerName(),
          "USD",
          c.getEstimatedLiability(),
          c.getApprovedSettlementAmount(),
          c.getDecisionReason(),
          c.getVersion(),
          c.getInformationRequests().stream().map(InformationView::from).toList());
    }
  }

  public record Summary(
      UUID id,
      String claimNumber,
      ClaimType claimType,
      Market market,
      ClaimStatus status,
      String assignedOfficerId,
      BigDecimal estimatedLiability,
      long version) {
    static Summary from(Claim c) {
      return new Summary(
          c.getId(),
          c.getClaimNumber(),
          c.getClaimType(),
          c.getMarket(),
          c.getStatus(),
          c.getAssignedOfficerId(),
          c.getEstimatedLiability(),
          c.getVersion());
    }
  }

  public record ClaimPage(
      List<Summary> content, int page, int size, long totalElements, int totalPages) {}

  public record StatusView(UUID id, ClaimStatus status, long version, Instant updatedAt) {}
}
