package com.chubb.claims.assignment;

import com.chubb.claims.claim.*;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AssignmentService {
    private final ClaimService claims;
    private final Clock clock;
    public AssignmentService(ClaimService claims, Clock clock) { this.claims = claims; this.clock = clock; }
    public ClaimDtos.Detail assign(UUID id, ClaimDtos.Assignment input) {
        return claims.change(id, input.expectedVersion(), "ClaimAssigned", c -> c.assign(input.officerId(), input.officerName(), clock.instant()));
    }
}
