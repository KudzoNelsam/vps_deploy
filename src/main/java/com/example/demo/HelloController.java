package com.example.demo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@Tag(name = "Hello", description = "Endpoint de test du pipeline CI/CD")
public class HelloController {

    @Operation(
            summary = "Message de bienvenue",
            description = "Retourne un message de confirmation avec la date/heure du déploiement"
    )
    @ApiResponse(responseCode = "200", description = "Message renvoyé avec succès")
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> hello() {
        return ResponseEntity.ok(
                Map.of(
                        "message", "Hello depuis mon pipeline CI/CD !",
                        "deployed_at", LocalDateTime.now().toString()));
    }

}
