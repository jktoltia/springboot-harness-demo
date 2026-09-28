package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InfoController {

    private final String version;
    private final String environment;

    public InfoController(@Value("${app.version}") String version,
                          @Value("${app.environment}") String environment) {
        this.version = version;
        this.environment = environment;
    }

    @GetMapping("/api/info")
    public Map<String, String> info() {
        return Map.of("app", "springboot-harness-demo", "version", version, "environment", environment);
    }
}
