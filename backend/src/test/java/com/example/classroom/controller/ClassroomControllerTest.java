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
import java.util.List;

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
                .andExpect(jsonPath("$.message").value("A student with name 'Ayush' has already joined this classroom. Please choose a different name."));
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
        when(classroomService.completeQuestion("AB7KQ2", "s-uuid-1", "q-uuid-1", null)).thenReturn(state);

        mockMvc.perform(post("/api/classrooms/AB7KQ2/students/s-uuid-1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedCount").value(1));
    }

    @Test
    @DisplayName("PUT /api/classrooms/{roomCode}/allow-answers toggles answer state")
    void testToggleAllowAnswersEndpoint() throws Exception {
        ClassroomStateResponse state = new ClassroomStateResponse(
                "AB7KQ2", true, null, Collections.emptyList(), 1, 0, 1, 0.0, Instant.now()
        );
        state.setAllowAnswers(false);
        when(classroomService.toggleAllowAnswers("AB7KQ2", "t-uuid-1", false)).thenReturn(state);

        AllowAnswersRequest request = new AllowAnswersRequest(false);
        mockMvc.perform(put("/api/classrooms/AB7KQ2/allow-answers")
                        .header("X-Teacher-Token", "t-uuid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowAnswers").value(false));
    }

    @Test
    @DisplayName("GET /api/health returns 200 UP")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("PUT /api/classrooms/{roomCode}/groups/settings configures group formation")
    void testConfigureGroupsEndpoint() throws Exception {
        ClassroomStateResponse state = new ClassroomStateResponse(
                "AB7KQ2", true, null, Collections.emptyList(), 0, 0, 0, 0.0, Instant.now()
        );
        state.setGroupsEnabled(true);
        state.setMaxGroupSize(3);

        when(classroomService.configureGroups("AB7KQ2", "t-uuid-1", true, 3)).thenReturn(state);

        ConfigureGroupsRequest request = new ConfigureGroupsRequest(true, 3);
        mockMvc.perform(put("/api/classrooms/AB7KQ2/groups/settings")
                        .header("X-Teacher-Token", "t-uuid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupsEnabled").value(true))
                .andExpect(jsonPath("$.maxGroupSize").value(3));
    }

    @Test
    @DisplayName("POST /api/classrooms/{roomCode}/groups creates a new group")
    void testCreateGroupEndpoint() throws Exception {
        ClassroomStateResponse state = new ClassroomStateResponse(
                "AB7KQ2", true, null, Collections.emptyList(), 1, 0, 1, 0.0, Instant.now()
        );
        state.setGroups(List.of(new StudentGroupDto("g-1", "Tigers", "s-1", "Alice", Instant.now(), Collections.emptyList())));

        when(classroomService.createGroup("AB7KQ2", "s-1", "Tigers")).thenReturn(state);

        CreateGroupRequest request = new CreateGroupRequest("s-1", "Tigers");
        mockMvc.perform(post("/api/classrooms/AB7KQ2/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groups[0].groupName").value("Tigers"));
    }

    @Test
    @DisplayName("DELETE /api/classrooms/{roomCode}/groups/{groupId}/members/{studentId} removes student by teacher")
    void testRemoveMemberByTeacherEndpoint() throws Exception {
        ClassroomStateResponse state = new ClassroomStateResponse(
                "AB7KQ2", true, null, Collections.emptyList(), 1, 0, 1, 0.0, Instant.now()
        );
        when(classroomService.removeStudentFromGroupByTeacher("AB7KQ2", "t-uuid-1", "g-1", "s-1")).thenReturn(state);

        mockMvc.perform(delete("/api/classrooms/AB7KQ2/groups/g-1/members/s-1")
                        .header("X-Teacher-Token", "t-uuid-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/classrooms/{roomCode}/groups/{groupId}/assign assigns student by teacher")
    void testAssignMemberByTeacherEndpoint() throws Exception {
        ClassroomStateResponse state = new ClassroomStateResponse(
                "AB7KQ2", true, null, Collections.emptyList(), 1, 0, 1, 0.0, Instant.now()
        );
        when(classroomService.assignStudentToGroupByTeacher("AB7KQ2", "t-uuid-1", "g-1", "s-2")).thenReturn(state);

        AssignStudentGroupRequest request = new AssignStudentGroupRequest("s-2");
        mockMvc.perform(post("/api/classrooms/AB7KQ2/groups/g-1/assign")
                        .header("X-Teacher-Token", "t-uuid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}

