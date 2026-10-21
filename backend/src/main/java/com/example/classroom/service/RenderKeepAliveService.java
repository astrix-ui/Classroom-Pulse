package com.example.classroom.service;

import com.example.classroom.model.ClassroomSession;
import com.example.classroom.repository.ClassroomMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Autonomous keep-alive service designed for Render free-tier hosting.
 *
 * Render spins down free web services after 15 minutes of inactivity (no incoming HTTP traffic).
 * In classroom environments, teachers frequently lock laptops or send students for short breaks.
 * When laptops are locked or browser tabs sleep, client-side timers cease to fire.
 *
 * This service runs autonomously inside the Spring Boot container. Every 8 minutes, if there is
 * at least one active classroom in memory, it sends an HTTP GET request to its own public URL
 * (provided automatically by Render via the RENDER_EXTERNAL_URL environment variable).
 *
 * Because this request hits Render's public edge gateway, Render registers it as active inbound
 * traffic and resets its 15-minute countdown timer. The container remains warm in memory,
 * preventing data loss during student/teacher breaks.
 *
 * When all classrooms are ended or closed, the service halts self-pings so it does not consume
 * unnecessary free monthly instance hours.
 */
@Service
public class RenderKeepAliveService {

    private static final Logger log = LoggerFactory.getLogger(RenderKeepAliveService.class);

    @Value("${RENDER_EXTERNAL_URL:${APP_PUBLIC_URL:${app.public-url:}}}")
    private String publicServiceUrl;

    private final ClassroomMemoryRepository repository;
    private final RestTemplate restTemplate;

    @Autowired
    public RenderKeepAliveService(ClassroomMemoryRepository repository, RestTemplateBuilder restTemplateBuilder) {
        this.repository = repository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Executes every 8 minutes (480,000 ms), well before Render's 15-minute idle cutoff.
     * Initial delay is 3 minutes (180,000 ms) after boot.
     */
    @Scheduled(fixedDelay = 480000, initialDelay = 180000)
    public void performKeepAlivePing() {
        if (publicServiceUrl == null || publicServiceUrl.trim().isEmpty()) {
            // Local development or non-Render deployment where no external URL is specified
            return;
        }

        boolean hasActiveSession = repository.findAll().stream()
                .anyMatch(ClassroomSession::isActive);

        if (!hasActiveSession) {
            log.debug("No active classrooms in session; skipping keep-alive ping to conserve Render hours.");
            return;
        }

        String normalizedUrl = publicServiceUrl.trim().replaceAll("/+$", "");
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            normalizedUrl = "https://" + normalizedUrl;
        }
        String pingEndpoint = normalizedUrl + "/api/health";

        try {
            log.info("Active classroom detected. Sending autonomous Render keep-alive ping to: {}", pingEndpoint);
            ResponseEntity<String> response = restTemplate.getForEntity(pingEndpoint, String.class);
            log.info("Render keep-alive ping successful (status: {})", response.getStatusCode());
        } catch (Exception e) {
            log.warn("Render keep-alive ping to {} failed: {}", pingEndpoint, e.getMessage());
        }
    }

    public String getPublicServiceUrl() {
        return publicServiceUrl;
    }

    public void setPublicServiceUrl(String publicServiceUrl) {
        this.publicServiceUrl = publicServiceUrl;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
}

