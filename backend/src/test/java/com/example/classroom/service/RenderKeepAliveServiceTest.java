package com.example.classroom.service;

import com.example.classroom.model.ClassroomSession;
import com.example.classroom.repository.ClassroomMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RenderKeepAliveServiceTest {

    private ClassroomMemoryRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    private RenderKeepAliveService keepAliveService;

    @BeforeEach
    void setUp() {
        repository = new ClassroomMemoryRepository();
        lenient().when(restTemplateBuilder.setConnectTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        lenient().when(restTemplateBuilder.setReadTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        lenient().when(restTemplateBuilder.build()).thenReturn(restTemplate);

        keepAliveService = new RenderKeepAliveService(repository, restTemplateBuilder);
    }

    @Test
    @DisplayName("Does not send ping if publicServiceUrl is not configured")
    void testNoPingWhenUrlNotConfigured() {
        keepAliveService.setPublicServiceUrl(null);
        repository.save(new ClassroomSession("TEST01", "token-1", Instant.now()));

        keepAliveService.performKeepAlivePing();

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Does not send ping if no active classroom sessions exist")
    void testNoPingWhenNoClassrooms() {
        keepAliveService.setPublicServiceUrl("https://my-app.onrender.com");

        keepAliveService.performKeepAlivePing();

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Does not send ping if all classrooms are closed/inactive")
    void testNoPingWhenAllClassroomsInactive() {
        keepAliveService.setPublicServiceUrl("https://my-app.onrender.com");
        ClassroomSession session = new ClassroomSession("TEST02", "token-2", Instant.now());
        session.setActive(false);
        repository.save(session);

        keepAliveService.performKeepAlivePing();

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Sends keep-alive ping to /api/health when an active classroom is present")
    void testPingSentWhenActiveClassroomExists() {
        keepAliveService.setPublicServiceUrl("https://my-app.onrender.com/");
        ClassroomSession session = new ClassroomSession("TEST03", "token-3", Instant.now());
        repository.save(session);

        when(restTemplate.getForEntity(eq("https://my-app.onrender.com/api/health"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"status\":\"UP\"}", HttpStatus.OK));

        keepAliveService.performKeepAlivePing();

        verify(restTemplate, times(1)).getForEntity("https://my-app.onrender.com/api/health", String.class);
    }

    @Test
    @DisplayName("Normalizes URL missing https scheme and trailing slash")
    void testUrlNormalization() {
        keepAliveService.setPublicServiceUrl("my-app.onrender.com");
        ClassroomSession session = new ClassroomSession("TEST04", "token-4", Instant.now());
        repository.save(session);

        when(restTemplate.getForEntity(eq("https://my-app.onrender.com/api/health"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"status\":\"UP\"}", HttpStatus.OK));

        keepAliveService.performKeepAlivePing();

        verify(restTemplate, times(1)).getForEntity("https://my-app.onrender.com/api/health", String.class);
    }

    @Test
    @DisplayName("Gracefully catches network exceptions without throwing")
    void testGracefulErrorHandling() {
        keepAliveService.setPublicServiceUrl("https://my-app.onrender.com");
        ClassroomSession session = new ClassroomSession("TEST05", "token-5", Instant.now());
        repository.save(session);

        when(restTemplate.getForEntity(eq("https://my-app.onrender.com/api/health"), eq(String.class)))
                .thenThrow(new RestClientException("Connection timed out"));

        assertDoesNotThrow(() -> keepAliveService.performKeepAlivePing());
        verify(restTemplate, times(1)).getForEntity("https://my-app.onrender.com/api/health", String.class);
    }
}

