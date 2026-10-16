package com.example.classroom.controller;

import com.example.classroom.dto.*;
import com.example.classroom.exception.*;
import com.example.classroom.model.CompletionStatus;
import com.example.classroom.service.ClassroomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClassroomController.class)
class ClassroomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClassroomService classroomService;

    @Test
    @DisplayName("POST /api/classrooms creates classroom and returns 201")
    void testCreateClassroom() throws Exception {
        when(classroomService.createClassroom())
                .thenReturn(new CreateClassroomResponse("AB7KQ2", "test-token-123"));

        mockMvc.perform(post("/api/classrooms")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value("AB7KQ2"))
                .andExpect(jsonPath("$.teacherToken").value("test-token-123"));
    }

    @Test
    @DisplayName("POST /api/classrooms/{roomCode}/students joins student and returns 201")
    void testJoinStudent() throws Exception {
        JoinStudentRequest request = new JoinStudentRequest("Ayush");
        when(classroomService.joinStudent(eq("AB7KQ2"), eq("Ayush")))
                .thenReturn(new JoinStudentResponse("uuid-1", "Ayush", "AB7KQ2", CompletionStatus.PENDING));

        mockMvc.perform(post("/api/classrooms/AB7KQ2/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value("uuid-1"))
                .andExpect(jsonPath("$.studentName").value("Ayush"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/classrooms/{roomCode}/students returns 400 for empty name")
    void testJoinStudentValidationFailure() throws Exception {
        JoinStudentRequest request = new JoinStudentRequest("");

        mockMvc.perform(post("/api/classrooms/AB7KQ2/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("POST /api/classrooms/{roomCode}/students returns 409 for duplicate name")
    void testJoinStudentDuplicateFailure() throws Exception {
        JoinStudentRequest request = new JoinStudentRequest("Ayush");
        when(classroomService.joinStudent(eq("AB7KQ2"), eq("Ayush")))
                .thenThrow(new DuplicateStudentException("Ayush"));

        mockMvc.perform(post("/api/classrooms/AB7KQ2/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A student with name 'Ayush' has already joined this classroom"));
    }

    @Test
    @DisplayName("GET /api/classrooms/{roomCode} returns safe state without teacher token")
    void testGetClassroomState() throws Exception {
        ClassroomStateResponse state = new ClassroomStateResponse(
                "AB7KQ2", true, null, Collections.emptyList(), 0, 0, 0, 0.0, Instant.now()
        );
        when(classroomService.getClassroomState("AB7KQ2")).thenReturn(state);

        mockMvc.perform(get("/api/classrooms/AB7KQ2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value("AB7KQ2"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.teacherToken").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/classrooms/{roomCode}/questions returns 401 on missing or invalid token")
    void testPublishQuestionUnauthorized() throws Exception {
        PublishQuestionRequest request = new PublishQuestionRequest("What is Spring?");
        when(classroomService.publishQuestion(eq("AB7KQ2"), isNull(), eq("What is Spring?")))
                .thenThrow(new UnauthorizedTeacherException("Invalid or missing teacher token"));

        mockMvc.perform(post("/api/classrooms/AB7KQ2/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/classrooms/{roomCode}/students/{studentId}/complete marks question complete")
    void testCompleteQuestion() throws Exception {
        CompleteQuestionRequest request = new CompleteQuestionRequest("q-uuid-1");
        ClassroomStateResponse state = new ClassroomStateResponse(
                "AB7KQ2", true, null, Collections.emptyList(), 1, 1, 0, 100.0, Instant.now()
        );
        when(classroomService.completeQuestion("AB7KQ2", "s-uuid-1", "q-uuid-1")).thenReturn(state);

        mockMvc.perform(post("/api/classrooms/AB7KQ2/students/s-uuid-1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedCount").value(1));
    }

    @Test
    @DisplayName("GET /api/health returns 200 UP")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}

