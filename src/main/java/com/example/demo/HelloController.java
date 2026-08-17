package com.example.demo;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> hello() {
        return ResponseEntity.ok(
                Map.of(
                        "message", "Hello depuis mon pipeline CI/CD !",
                        "deployed_at", LocalDateTime.now().toString()));
    }

}
