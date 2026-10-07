package com.chubb.claims.claim;

public enum ClaimStatus {
  SUBMITTED,
  ASSIGNED,
  UNDER_REVIEW,
  INFORMATION_REQUIRED,
  APPROVED,
  SETTLED,
  REJECTED;

  public boolean isTerminal() {
    return this == SETTLED || this == REJECTED;
  }
}
