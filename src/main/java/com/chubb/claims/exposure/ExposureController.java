package com.chubb.claims.exposure;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/exposure")
public class ExposureController {
    private final ExposureService exposure;
    public ExposureController(ExposureService exposure) { this.exposure = exposure; }
    @GetMapping public ExposureService.Exposure get() { return exposure.get(); }
}
