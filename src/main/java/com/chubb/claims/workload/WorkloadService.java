package com.chubb.claims.workload;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkloadService {
    private final JdbcTemplate jdbc;
    public WorkloadService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Transactional(readOnly=true)
    public Workload get() {
        List<OfficerStatusCount> groups = jdbc.query("SELECT assigned_officer_id, status, COUNT(*) AS count FROM claims WHERE status NOT IN ('SETTLED','REJECTED') GROUP BY assigned_officer_id, status ORDER BY assigned_officer_id, status",
            (rs, row) -> new OfficerStatusCount(rs.getString("assigned_officer_id"), rs.getString("status"), rs.getLong("count")));
        long unassigned = groups.stream().filter(g -> g.officerId() == null).mapToLong(OfficerStatusCount::count).sum();
        Performance performance = jdbc.queryForObject("SELECT COUNT(CASE WHEN status='SETTLED' THEN 1 END) AS settled, COUNT(CASE WHEN status='REJECTED' THEN 1 END) AS rejected, AVG(EXTRACT(EPOCH FROM (closed_at - reported_at))) AS seconds FROM claims WHERE status IN ('SETTLED','REJECTED')",
            (rs, row) -> new Performance(rs.getLong("settled"), rs.getLong("rejected"), (Double) rs.getObject("seconds", Double.class)));
        return new Workload(unassigned, groups, performance);
    }
    public record OfficerStatusCount(String officerId, String status, long count) {}
    public record Performance(long settledCount, long rejectedCount, Double averageCompletionSeconds) {}
    public record Workload(long unassignedCount, List<OfficerStatusCount> openClaimsByOfficerAndStatus, Performance performance) {}
}
