package com.chubb.claims;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HttpSmokeTest {
  @LocalServerPort int port;

  @Test
  void runningServerExposesHealthOpenApiAndClaims() {
    RestClient client = RestClient.create("http://localhost:" + port);
    assertThat(
            client
                .get()
                .uri("/actuator/health")
                .retrieve()
                .body(JsonNode.class)
                .get("status")
                .asText())
        .isEqualTo("UP");
    JsonNode spec = client.get().uri("/v3/api-docs").retrieve().body(JsonNode.class);
    assertThat(spec.get("paths").has("/api/claims/{id}/settlement")).isTrue();
    assertThat(client.get().uri("/swagger-ui/index.html").retrieve().body(String.class))
        .contains("Swagger UI");
    JsonNode claim =
        client
            .post()
            .uri("/api/claims")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(
                java.util.Map.of(
                    "claimType",
                    "MOTOR",
                    "market",
                    "SG",
                    "claimantName",
                    "Test",
                    "incidentDescription",
                    "Collision",
                    "incidentDate",
                    java.time.LocalDate.now().minusDays(1).toString(),
                    "estimatedLiability",
                    100))
            .retrieve()
            .body(JsonNode.class);
    assertThat(claim.get("status").asText()).isEqualTo("SUBMITTED");
    JsonNode status =
        client
            .get()
            .uri("/api/claims/" + claim.get("id").asText() + "/status")
            .retrieve()
            .body(JsonNode.class);
    assertThat(status.get("version").asLong()).isZero();
  }
}
