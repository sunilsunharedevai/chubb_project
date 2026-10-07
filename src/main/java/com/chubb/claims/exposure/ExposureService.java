package com.chubb.claims.exposure;

import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExposureService {
    private final JdbcTemplate jdbc;
    public ExposureService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Transactional(readOnly=true)
    public Exposure get() {
        return jdbc.queryForObject("SELECT COALESCE(SUM(estimated_liability),0) AS total, COUNT(*) AS count FROM claims WHERE status NOT IN ('SETTLED','REJECTED')",
            (rs, row) -> new Exposure("USD", rs.getBigDecimal("total"), rs.getLong("count")));
    }
    public record Exposure(String currency, BigDecimal totalOutstandingExposure, long openClaimCount) {}
}
