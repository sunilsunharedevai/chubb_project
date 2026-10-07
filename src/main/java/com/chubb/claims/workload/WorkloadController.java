package com.chubb.claims.workload;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workload")
public class WorkloadController {
  private final WorkloadService workload;

  public WorkloadController(WorkloadService workload) {
    this.workload = workload;
  }

  @GetMapping
  public WorkloadService.Workload get() {
    return workload.get();
  }
}
