package com.example.classroom.service;

import com.example.classroom.dto.*;
import com.example.classroom.exception.*;
import com.example.classroom.model.CompletionStatus;
import com.example.classroom.repository.ClassroomMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {

    private ClassroomMemoryRepository repository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ClassroomService service;

    @BeforeEach
    void setUp() {
        repository = new ClassroomMemoryRepository();
        service = new ClassroomService(repository, messagingTemplate);
    }

    @Test
    @DisplayName("Classroom creation generates a 6-character room code and teacher token")
    void testCreateClassroom() {
        CreateClassroomResponse response = service.createClassroom();
        assertNotNull(response);
        assertNotNull(response.getRoomCode());
        assertEquals(6, response.getRoomCode().length());
        assertNotNull(response.getTeacherToken());
        assertTrue(repository.existsById(response.getRoomCode()));
    }

    @Test
    @DisplayName("Student can join an active classroom")
    void testStudentJoin() {
        CreateClassroomResponse room = service.createClassroom();
        JoinStudentResponse student = service.joinStudent(room.getRoomCode(), "Alice");

        assertNotNull(student);
        assertNotNull(student.getStudentId());
        assertEquals("Alice", student.getStudentName());
        assertEquals(CompletionStatus.PENDING, student.getStatus());

        ClassroomStateResponse state = service.getClassroomState(room.getRoomCode());
        assertEquals(1, state.getTotalStudents());
        assertEquals(1, state.getPendingCount());
        assertEquals(0, state.getCompletedCount());
    }

    @Test
    @DisplayName("Duplicate student name is rejected case-insensitively")
    void testDuplicateStudentRejected() {
        CreateClassroomResponse room = service.createClassroom();
        service.joinStudent(room.getRoomCode(), "Alice");

        assertThrows(DuplicateStudentException.class, () ->
                service.joinStudent(room.getRoomCode(), "alice"));

        assertThrows(DuplicateStudentException.class, () ->
                service.joinStudent(room.getRoomCode(), "  ALICE  "));
    }

    @Test
    @DisplayName("Teacher can publish a question with a correct token")
    void testPublishQuestionSuccess() {
        CreateClassroomResponse room = service.createClassroom();
        service.joinStudent(room.getRoomCode(), "Alice");

        QuestionDto question = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "What is 2+2?");

        assertNotNull(question);
        assertNotNull(question.getQuestionId());
        assertEquals("What is 2+2?", question.getQuestionText());
        assertEquals(1, question.getSequenceNumber());

        ClassroomStateResponse state = service.getClassroomState(room.getRoomCode());
        assertNotNull(state.getCurrentQuestion());
        assertEquals("What is 2+2?", state.getCurrentQuestion().getQuestionText());
    }

    @Test
    @DisplayName("Incorrect teacher token is rejected when publishing question")
    void testPublishQuestionIncorrectToken() {
        CreateClassroomResponse room = service.createClassroom();

        assertThrows(UnauthorizedTeacherException.class, () ->
                service.publishQuestion(room.getRoomCode(), "wrong-token", "What is 2+2?"));

        assertThrows(UnauthorizedTeacherException.class, () ->
                service.publishQuestion(room.getRoomCode(), null, "What is 2+2?"));
    }

    @Test
    @DisplayName("Publishing a question resets all student statuses to PENDING")
    void testPublishQuestionResetsStatus() {
        CreateClassroomResponse room = service.createClassroom();
        JoinStudentResponse s1 = service.joinStudent(room.getRoomCode(), "Alice");
        JoinStudentResponse s2 = service.joinStudent(room.getRoomCode(), "Bob");

        QuestionDto q1 = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Question 1");
        service.completeQuestion(room.getRoomCode(), s1.getStudentId(), q1.getQuestionId());

        ClassroomStateResponse stateBefore = service.getClassroomState(room.getRoomCode());
        assertEquals(1, stateBefore.getCompletedCount());
        assertEquals(1, stateBefore.getPendingCount());

        // Publish Question 2
        QuestionDto q2 = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Question 2");
        assertEquals(2, q2.getSequenceNumber());

        ClassroomStateResponse stateAfter = service.getClassroomState(room.getRoomCode());
        assertEquals(0, stateAfter.getCompletedCount());
        assertEquals(2, stateAfter.getPendingCount());
        stateAfter.getStudents().forEach(s -> assertEquals(CompletionStatus.PENDING, s.getStatus()));
    }

    @Test
    @DisplayName("Student can mark the current question completed")
    void testStudentMarksCompleted() {
        CreateClassroomResponse room = service.createClassroom();
        JoinStudentResponse student = service.joinStudent(room.getRoomCode(), "Alice");
        QuestionDto q = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Explain DI");

        ClassroomStateResponse updated = service.completeQuestion(room.getRoomCode(), student.getStudentId(), q.getQuestionId());
        assertEquals(1, updated.getCompletedCount());
        assertEquals(0, updated.getPendingCount());
        assertEquals(100.0, updated.getCompletionPercentage());
    }

    @Test
    @DisplayName("Repeated completion is idempotent and does not corrupt count")
    void testRepeatedCompletionIsIdempotent() {
        CreateClassroomResponse room = service.createClassroom();
        JoinStudentResponse student = service.joinStudent(room.getRoomCode(), "Alice");
        QuestionDto q = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Explain DI");

        service.completeQuestion(room.getRoomCode(), student.getStudentId(), q.getQuestionId());
        ClassroomStateResponse secondCall = service.completeQuestion(room.getRoomCode(), student.getStudentId(), q.getQuestionId());

        assertEquals(1, secondCall.getCompletedCount());
        assertEquals(0, secondCall.getPendingCount());
    }

    @Test
    @DisplayName("Stale questionId is rejected")
    void testStaleQuestionIdRejected() {
        CreateClassroomResponse room = service.createClassroom();
        JoinStudentResponse student = service.joinStudent(room.getRoomCode(), "Alice");
        QuestionDto q1 = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Question 1");
        QuestionDto q2 = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Question 2");

        // Attempting to complete question 1 when question 2 is active
        assertThrows(InvalidClassroomStateException.class, () ->
                service.completeQuestion(room.getRoomCode(), student.getStudentId(), q1.getQuestionId()));
    }

    @Test
    @DisplayName("Closing classroom prevents new joins and student actions")
    void testCloseClassroom() {
        CreateClassroomResponse room = service.createClassroom();
        JoinStudentResponse student = service.joinStudent(room.getRoomCode(), "Alice");
        QuestionDto q = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Question 1");

        service.closeClassroom(room.getRoomCode(), room.getTeacherToken());

        ClassroomStateResponse state = service.getClassroomState(room.getRoomCode());
        assertFalse(state.isActive());

        // New student join rejected
        assertThrows(InvalidClassroomStateException.class, () ->
                service.joinStudent(room.getRoomCode(), "Bob"));

        // Student complete rejected
        assertThrows(InvalidClassroomStateException.class, () ->
                service.completeQuestion(room.getRoomCode(), student.getStudentId(), q.getQuestionId()));
    }

    @Test
    @DisplayName("Concurrent student completion requests do not corrupt state")
    void testConcurrentStudentCompletions() throws Exception {
        CreateClassroomResponse room = service.createClassroom();
        int studentCount = 50;
        List<JoinStudentResponse> students = new ArrayList<>();
        for (int i = 0; i < studentCount; i++) {
            students.add(service.joinStudent(room.getRoomCode(), "Student_" + i));
        }

        QuestionDto q = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Concurrent test");

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(studentCount);

        for (JoinStudentResponse student : students) {
            executor.submit(() -> {
                try {
                    service.completeQuestion(room.getRoomCode(), student.getStudentId(), q.getQuestionId());
                    // Idempotent duplicate call
                    service.completeQuestion(room.getRoomCode(), student.getStudentId(), q.getQuestionId());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        ClassroomStateResponse finalState = service.getClassroomState(room.getRoomCode());
        assertEquals(studentCount, finalState.getTotalStudents());
        assertEquals(studentCount, finalState.getCompletedCount());
        assertEquals(0, finalState.getPendingCount());
        assertEquals(100.0, finalState.getCompletionPercentage());
    }

    @Test
    @DisplayName("Student can leave the classroom session")
    void testStudentLeave() {
        CreateClassroomResponse room = service.createClassroom();
        JoinStudentResponse student = service.joinStudent(room.getRoomCode(), "Alice");
        assertEquals(1, service.getClassroomState(room.getRoomCode()).getTotalStudents());

        ClassroomStateResponse stateAfterLeave = service.leaveStudent(room.getRoomCode(), student.getStudentId());
        assertEquals(0, stateAfterLeave.getTotalStudents());
    }

    @Test
    @DisplayName("Teacher can edit the active question")
    void testEditQuestion() {
        CreateClassroomResponse room = service.createClassroom();
        service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Original Question?");

        QuestionDto updated = service.editQuestion(room.getRoomCode(), room.getTeacherToken(), "Updated Question?");
        assertEquals("Updated Question?", updated.getQuestionText());

        ClassroomStateResponse state = service.getClassroomState(room.getRoomCode());
        assertEquals("Updated Question?", state.getCurrentQuestion().getQuestionText());
    }

    @Test
    @DisplayName("Teacher can delete the active question and students reset to pending")
    void testDeleteQuestion() {
        CreateClassroomResponse room = service.createClassroom();
        JoinStudentResponse student = service.joinStudent(room.getRoomCode(), "Bob");
        QuestionDto q = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "To be deleted?");

        service.completeQuestion(room.getRoomCode(), student.getStudentId(), q.getQuestionId());
        assertEquals(1, service.getClassroomState(room.getRoomCode()).getCompletedCount());

        ClassroomStateResponse stateAfterDelete = service.deleteQuestion(room.getRoomCode(), room.getTeacherToken());
        assertNull(stateAfterDelete.getCurrentQuestion());
        assertEquals(0, stateAfterDelete.getCompletedCount());
        assertEquals(1, stateAfterDelete.getPendingCount());
    }
}

