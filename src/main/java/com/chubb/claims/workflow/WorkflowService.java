package com.chubb.claims.workflow;

import com.chubb.claims.claim.*;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class WorkflowService {
  private final ClaimService claims;
  private final Clock clock;

  public WorkflowService(ClaimService claims, Clock clock) {
    this.claims = claims;
    this.clock = clock;
  }

  public ClaimDtos.Detail review(UUID id, ClaimDtos.Version input) {
    return claims.change(
        id, input.expectedVersion(), "ClaimReviewStarted", c -> c.startReview(clock.instant()));
  }

  public ClaimDtos.Detail request(UUID id, ClaimDtos.RequestInformation input) {
    return claims.change(
        id,
        input.expectedVersion(),
        "AdditionalInformationRequested",
        c -> c.requestInformation(input.question(), clock.instant()));
  }

  public ClaimDtos.Detail provide(UUID id, UUID requestId, ClaimDtos.ProvideInformation input) {
    return claims.change(
        id,
        input.expectedVersion(),
        "AdditionalInformationProvided",
        c -> c.provideInformation(requestId, input.response(), clock.instant()));
  }

  public ClaimDtos.Detail assess(UUID id, ClaimDtos.Assessment input) {
    return claims.change(
        id,
        input.expectedVersion(),
        "ClaimAssessed",
        c ->
            c.assess(
                input.estimatedLiability(),
                input.approvedSettlementAmount(),
                input.reason(),
                clock.instant()));
  }

  public ClaimDtos.Detail settle(UUID id, ClaimDtos.Version input) {
    return claims.change(
        id, input.expectedVersion(), "ClaimSettled", c -> c.settle(clock.instant()));
  }

  public ClaimDtos.Detail reject(UUID id, ClaimDtos.Rejection input) {
    return claims.change(
        id,
        input.expectedVersion(),
        "ClaimRejected",
        c -> c.reject(input.reason(), clock.instant()));
  }
}
