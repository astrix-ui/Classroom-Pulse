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
        ClassroomStateResponse response = classroomService.completeQuestion(roomCode, studentId, request.getQuestionId(), request.getAnswerText());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/reset")
    public ResponseEntity<ClassroomStateResponse> resetClassroom(
            @PathVariable String roomCode,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken) {
        ClassroomStateResponse response = classroomService.resetClassroom(roomCode, teacherToken);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/classrooms/{roomCode}/allow-answers")
    public ResponseEntity<ClassroomStateResponse> toggleAllowAnswers(
            @PathVariable String roomCode,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken,
            @RequestBody(required = false) AllowAnswersRequest request) {
        boolean allow = request == null || request.getAllowAnswers() == null || Boolean.TRUE.equals(request.getAllowAnswers());
        ClassroomStateResponse response = classroomService.toggleAllowAnswers(roomCode, teacherToken, allow);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/allow-answers")
    public ResponseEntity<ClassroomStateResponse> toggleAllowAnswersPost(
            @PathVariable String roomCode,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken,
            @RequestBody(required = false) AllowAnswersRequest request) {
        boolean allow = request == null || request.getAllowAnswers() == null || Boolean.TRUE.equals(request.getAllowAnswers());
        ClassroomStateResponse response = classroomService.toggleAllowAnswers(roomCode, teacherToken, allow);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/close")
    public ResponseEntity<ClassroomStateResponse> closeClassroom(
            @PathVariable String roomCode,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken) {
        ClassroomStateResponse response = classroomService.closeClassroom(roomCode, teacherToken);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/classrooms/{roomCode}/groups/settings")
    public ResponseEntity<ClassroomStateResponse> configureGroups(
            @PathVariable String roomCode,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken,
            @Valid @RequestBody ConfigureGroupsRequest request) {
        ClassroomStateResponse response = classroomService.configureGroups(
                roomCode, teacherToken, request.isEnabled(), request.getMaxGroupSize());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/groups/reset")
    public ResponseEntity<ClassroomStateResponse> resetGroups(
            @PathVariable String roomCode,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken) {
        ClassroomStateResponse response = classroomService.resetGroups(roomCode, teacherToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/groups")
    public ResponseEntity<ClassroomStateResponse> createGroup(
            @PathVariable String roomCode,
            @Valid @RequestBody CreateGroupRequest request) {
        ClassroomStateResponse response = classroomService.createGroup(
                roomCode, request.getStudentId(), request.getGroupName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/classrooms/{roomCode}/groups/{groupId}/members")
    public ResponseEntity<ClassroomStateResponse> addMemberToGroup(
            @PathVariable String roomCode,
            @PathVariable String groupId,
            @Valid @RequestBody AddGroupMemberRequest request) {
        ClassroomStateResponse response = classroomService.addMemberToGroup(
                roomCode, groupId, request.getRequestingStudentId(), request.getTargetStudentId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/groups/{groupId}/members/{studentId}/leave")
    public ResponseEntity<ClassroomStateResponse> leaveGroup(
            @PathVariable String roomCode,
            @PathVariable String groupId,
            @PathVariable String studentId) {
        ClassroomStateResponse response = classroomService.leaveGroup(roomCode, groupId, studentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/classrooms/{roomCode}/groups/{groupId}/members/{studentId}")
    public ResponseEntity<ClassroomStateResponse> removeMemberByTeacher(
            @PathVariable String roomCode,
            @PathVariable String groupId,
            @PathVariable String studentId,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken) {
        ClassroomStateResponse response = classroomService.removeStudentFromGroupByTeacher(
                roomCode, teacherToken, groupId, studentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/groups/{groupId}/members/{studentId}/remove")
    public ResponseEntity<ClassroomStateResponse> removeMemberByTeacherPost(
            @PathVariable String roomCode,
            @PathVariable String groupId,
            @PathVariable String studentId,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken) {
        ClassroomStateResponse response = classroomService.removeStudentFromGroupByTeacher(
                roomCode, teacherToken, groupId, studentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/groups/{groupId}/assign")
    public ResponseEntity<ClassroomStateResponse> assignMemberByTeacher(
            @PathVariable String roomCode,
            @PathVariable String groupId,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken,
            @Valid @RequestBody AssignStudentGroupRequest request) {
        ClassroomStateResponse response = classroomService.assignStudentToGroupByTeacher(
                roomCode, teacherToken, groupId, request.getStudentId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/classrooms/{roomCode}/students/{studentId}/kick")
    public ResponseEntity<ClassroomStateResponse> kickStudent(
            @PathVariable String roomCode,
            @PathVariable String studentId,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken) {
        ClassroomStateResponse response = classroomService.kickStudent(roomCode, teacherToken, studentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/students/{studentId}/kick")
    public ResponseEntity<ClassroomStateResponse> kickStudentPost(
            @PathVariable String roomCode,
            @PathVariable String studentId,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken) {
        ClassroomStateResponse response = classroomService.kickStudent(roomCode, teacherToken, studentId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/classrooms/{roomCode}/groups/{groupId}/name")
    public ResponseEntity<ClassroomStateResponse> renameGroup(
            @PathVariable String roomCode,
            @PathVariable String groupId,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken,
            @Valid @RequestBody RenameGroupRequest request) {
        ClassroomStateResponse response = classroomService.renameGroup(
                roomCode, teacherToken, groupId, request.getGroupName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classrooms/{roomCode}/groups/{groupId}/rename")
    public ResponseEntity<ClassroomStateResponse> renameGroupPost(
            @PathVariable String roomCode,
            @PathVariable String groupId,
            @RequestHeader(value = "X-Teacher-Token", required = false) String teacherToken,
            @Valid @RequestBody RenameGroupRequest request) {
        ClassroomStateResponse response = classroomService.renameGroup(
                roomCode, teacherToken, groupId, request.getGroupName());
        return ResponseEntity.ok(response);
    }
}

