package com.example.classroom.controller;

import com.example.classroom.dto.*;
import com.example.classroom.service.ClassroomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClassroomController {

    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @PostMapping("/classrooms")
    public ResponseEntity<CreateClassroomResponse> createClassroom() {
        CreateClassroomResponse response = classroomService.createClassroom();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/classrooms/{roomCode}/students")
    public ResponseEntity<JoinStudentResponse> joinStudent(
            @PathVariable String roomCode,
            @Valid @RequestBody JoinStudentRequest request) {
        JoinStudentResponse response = classroomService.joinStudent(roomCode, request.getStudentName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/classrooms/{roomCode}")
    public ResponseEntity<ClassroomStateResponse> getClassroomState(@PathVariable String roomCode) {
        ClassroomStateResponse response = classroomService.getClassroomState(roomCode);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/classrooms/{roomCode}/students/{studentId}")
    public ResponseEntity<ClassroomStateResponse> leaveStudent(
            @PathVariable String roomCode,
            @PathVariable String studentId) {
        ClassroomStateResponse response = classroomService.leaveStudent(roomCode, studentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/students/{studentId}/leave")
    public ResponseEntity<ClassroomStateResponse> leaveStudentPost(
            @PathVariable String roomCode,
            @PathVariable String studentId) {
        ClassroomStateResponse response = classroomService.leaveStudent(roomCode, studentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/questions")
    public ResponseEntity<QuestionDto> publishQuestion(
            @PathVariable String roomCode,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken,
            @Valid @RequestBody PublishQuestionRequest request) {
        QuestionDto response = classroomService.publishQuestion(roomCode, teacherToken, request.getQuestionText());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/classrooms/{roomCode}/questions")
    public ResponseEntity<QuestionDto> editQuestion(
            @PathVariable String roomCode,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken,
            @Valid @RequestBody EditQuestionRequest request) {
        QuestionDto response = classroomService.editQuestion(roomCode, teacherToken, request.getQuestionText());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/classrooms/{roomCode}/questions")
    public ResponseEntity<ClassroomStateResponse> deleteQuestion(
            @PathVariable String roomCode,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken) {
        ClassroomStateResponse response = classroomService.deleteQuestion(roomCode, teacherToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/students/{studentId}/complete")
    public ResponseEntity<ClassroomStateResponse> completeQuestion(
            @PathVariable String roomCode,
            @PathVariable String studentId,
            @Valid @RequestBody CompleteQuestionRequest request) {
        ClassroomStateResponse response = classroomService.completeQuestion(roomCode, studentId, request.getQuestionId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/reset")
    public ResponseEntity<ClassroomStateResponse> resetClassroom(
            @PathVariable String roomCode,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken) {
        ClassroomStateResponse response = classroomService.resetClassroom(roomCode, teacherToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/close")
    public ResponseEntity<ClassroomStateResponse> closeClassroom(
            @PathVariable String roomCode,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken) {
        ClassroomStateResponse response = classroomService.closeClassroom(roomCode, teacherToken);
        return ResponseEntity.ok(response);
    }
}

