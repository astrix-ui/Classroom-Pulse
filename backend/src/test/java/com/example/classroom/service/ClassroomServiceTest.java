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

    @Test
    @DisplayName("Teacher can toggle written answers; students type answers when true, simple done when false")
    void testToggleAllowAnswers() {
        CreateClassroomResponse room = service.createClassroom();
        JoinStudentResponse student = service.joinStudent(room.getRoomCode(), "Charlie");
        QuestionDto q = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Can you answer?");

        // Default is false (simple I'm Done mode)
        assertFalse(service.getClassroomState(room.getRoomCode()).isAllowAnswers());

        // In simple mode, student can complete without text
        ClassroomStateResponse simpleDone = service.completeQuestion(room.getRoomCode(), student.getStudentId(), q.getQuestionId(), null);
        assertEquals(1, simpleDone.getCompletedCount());

        // Toggle to true (enable written answers mode)
        ClassroomStateResponse toggled = service.toggleAllowAnswers(room.getRoomCode(), room.getTeacherToken(), true);
        assertTrue(toggled.isAllowAnswers());

        // New question
        QuestionDto q2 = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Question 2?");

        // Student completing without answerText should fail when allowAnswers is true
        assertThrows(IllegalArgumentException.class, () ->
            service.completeQuestion(room.getRoomCode(), student.getStudentId(), q2.getQuestionId(), "   "));

        // Student completing with answerText succeeds
        ClassroomStateResponse completedWithAnswer = service.completeQuestion(
            room.getRoomCode(), student.getStudentId(), q2.getQuestionId(), "My typed answer"
        );
        assertEquals(1, completedWithAnswer.getCompletedCount());

        StudentDto charlie = completedWithAnswer.getStudents().stream().filter(s -> s.getStudentName().equals("Charlie")).findFirst().orElseThrow();
        assertEquals("My typed answer", charlie.getAnswers().get(q2.getQuestionId()));
    }

    @Test
    @DisplayName("Question history retains all published questions and tracks student answered questions map")
    void testQuestionHistoryAndStudentAnswerTracking() {
        CreateClassroomResponse room = service.createClassroom();
        JoinStudentResponse student = service.joinStudent(room.getRoomCode(), "Dana");

        QuestionDto q1 = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Question One?");
        service.completeQuestion(room.getRoomCode(), student.getStudentId(), q1.getQuestionId());

        QuestionDto q2 = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Question Two?");
        service.completeQuestion(room.getRoomCode(), student.getStudentId(), q2.getQuestionId());

        QuestionDto q3 = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "Question Three?");

        ClassroomStateResponse state = service.getClassroomState(room.getRoomCode());
        assertEquals(3, state.getQuestions().size());
        assertEquals("Question One?", state.getQuestions().get(0).getQuestionText());
        assertEquals("Question Two?", state.getQuestions().get(1).getQuestionText());
        assertEquals("Question Three?", state.getQuestions().get(2).getQuestionText());

        StudentDto dana = state.getStudents().stream().filter(s -> s.getStudentName().equals("Dana")).findFirst().orElseThrow();
        assertTrue(dana.getAnsweredQuestions().containsKey(q1.getQuestionId()));
        assertTrue(dana.getAnsweredQuestions().containsKey(q2.getQuestionId()));
        assertFalse(dana.getAnsweredQuestions().containsKey(q3.getQuestionId()));
    }

    @Test
    @DisplayName("Joining room requires a unique student name")
    void testUniqueStudentName() {
        CreateClassroomResponse room = service.createClassroom();
        service.joinStudent(room.getRoomCode(), "Alice");

        assertThrows(DuplicateStudentException.class, () ->
            service.joinStudent(room.getRoomCode(), "Alice")
        );
        assertThrows(DuplicateStudentException.class, () ->
            service.joinStudent(room.getRoomCode(), " alice ")
        );
    }

    @Test
    @DisplayName("Group formation configuration, creation, member addition, and conflict prevention")
    void testGroupFormationAndConflictPrevention() {
        CreateClassroomResponse room = service.createClassroom();
        JoinStudentResponse s1 = service.joinStudent(room.getRoomCode(), "Alice");
        JoinStudentResponse s2 = service.joinStudent(room.getRoomCode(), "Bob");
        JoinStudentResponse s3 = service.joinStudent(room.getRoomCode(), "Charlie");

        // Teacher configures group size = 2
        ClassroomStateResponse configured = service.configureGroups(room.getRoomCode(), room.getTeacherToken(), true, 2);
        assertTrue(configured.isGroupsEnabled());
        assertEquals(2, configured.getMaxGroupSize());

        // Alice creates group "Team Rocket"
        ClassroomStateResponse groupState = service.createGroup(room.getRoomCode(), s1.getStudentId(), "Team Rocket");
        assertEquals(1, groupState.getGroups().size());
        StudentGroupDto group = groupState.getGroups().get(0);
        assertEquals("Team Rocket", group.getGroupName());
        assertEquals(1, group.getMembers().size());
        assertEquals("Alice", group.getMembers().get(0).getStudentName());

        // Alice adds Bob
        ClassroomStateResponse addedState = service.addMemberToGroup(room.getRoomCode(), group.getGroupId(), s1.getStudentId(), s2.getStudentId());
        StudentGroupDto updatedGroup = addedState.getGroups().get(0);
        assertEquals(2, updatedGroup.getMembers().size());

        // Charlie creates another group
        ClassroomStateResponse charlieGroupState = service.createGroup(room.getRoomCode(), s3.getStudentId(), "Team Galactic");
        assertEquals(2, charlieGroupState.getGroups().size());
        StudentGroupDto charlieGroup = charlieGroupState.getGroups().stream()
                .filter(g -> g.getGroupName().equals("Team Galactic")).findFirst().orElseThrow();

        // Charlie tries to add Bob (who is already in Alice's group) -> must fail with GroupConflictException!
        assertThrows(GroupConflictException.class, () ->
            service.addMemberToGroup(room.getRoomCode(), charlieGroup.getGroupId(), s3.getStudentId(), s2.getStudentId())
        );

        // Alice tries to add Charlie to Team Rocket -> fails because group size is 2 (already full)
        assertThrows(IllegalStateException.class, () ->
            service.addMemberToGroup(room.getRoomCode(), group.getGroupId(), s1.getStudentId(), s3.getStudentId())
        );

        // Bob leaves Alice's group
        ClassroomStateResponse leftState = service.leaveGroup(room.getRoomCode(), group.getGroupId(), s2.getStudentId());
        StudentGroupDto groupAfterLeave = leftState.getGroups().stream()
                .filter(g -> g.getGroupId().equals(group.getGroupId())).findFirst().orElseThrow();
        assertEquals(1, groupAfterLeave.getMembers().size());

        // Now Charlie CAN add Bob to Team Galactic!
        ClassroomStateResponse charlieAddedBobState = service.addMemberToGroup(
                room.getRoomCode(), charlieGroup.getGroupId(), s3.getStudentId(), s2.getStudentId());
        StudentGroupDto charlieGroupWithBob = charlieAddedBobState.getGroups().stream()
                .filter(g -> g.getGroupName().equals("Team Galactic")).findFirst().orElseThrow();
        assertEquals(2, charlieGroupWithBob.getMembers().size());
    }

    @Test
    @DisplayName("Teacher can remove student from group and move/assign student to another group")
    void testTeacherGroupManagement() {
        CreateClassroomResponse room = service.createClassroom();
        service.configureGroups(room.getRoomCode(), room.getTeacherToken(), true, 3);

        JoinStudentResponse s1 = service.joinStudent(room.getRoomCode(), "Alice");
        JoinStudentResponse s2 = service.joinStudent(room.getRoomCode(), "Bob");
        JoinStudentResponse s3 = service.joinStudent(room.getRoomCode(), "Charlie");

        // Alice creates Group 1
        ClassroomStateResponse gState1 = service.createGroup(room.getRoomCode(), s1.getStudentId(), "Group 1");
        String g1Id = gState1.getGroups().get(0).getGroupId();

        // Charlie creates Group 2
        ClassroomStateResponse gState2 = service.createGroup(room.getRoomCode(), s3.getStudentId(), "Group 2");
        String g2Id = gState2.getGroups().stream().filter(g -> g.getGroupName().equals("Group 2")).findFirst().orElseThrow().getGroupId();

        // Teacher assigns unassigned Bob to Group 1
        ClassroomStateResponse assignedBobState = service.assignStudentToGroupByTeacher(
                room.getRoomCode(), room.getTeacherToken(), g1Id, s2.getStudentId());
        StudentGroupDto g1WithBob = assignedBobState.getGroups().stream().filter(g -> g.getGroupId().equals(g1Id)).findFirst().orElseThrow();
        assertEquals(2, g1WithBob.getMembers().size());

        // Teacher moves Bob from Group 1 to Group 2
        ClassroomStateResponse movedBobState = service.assignStudentToGroupByTeacher(
                room.getRoomCode(), room.getTeacherToken(), g2Id, s2.getStudentId());
        StudentGroupDto g1AfterMove = movedBobState.getGroups().stream().filter(g -> g.getGroupId().equals(g1Id)).findFirst().orElseThrow();
        StudentGroupDto g2AfterMove = movedBobState.getGroups().stream().filter(g -> g.getGroupId().equals(g2Id)).findFirst().orElseThrow();
        assertEquals(1, g1AfterMove.getMembers().size());
        assertEquals(2, g2AfterMove.getMembers().size());

        // Teacher removes Bob from Group 2 (back to unassigned)
        ClassroomStateResponse removedBobState = service.removeStudentFromGroupByTeacher(
                room.getRoomCode(), room.getTeacherToken(), g2Id, s2.getStudentId());
        StudentGroupDto g2AfterRemove = removedBobState.getGroups().stream().filter(g -> g.getGroupId().equals(g2Id)).findFirst().orElseThrow();
        assertEquals(1, g2AfterRemove.getMembers().size());
        StudentDto bobDto = removedBobState.getStudents().stream().filter(s -> s.getStudentId().equals(s2.getStudentId())).findFirst().orElseThrow();
        assertNull(bobDto.getGroupId());
    }

    @Test
    @DisplayName("When one member of a group submits answer, all group members are marked completed with the same answer")
    void testGroupSubmissionSubmitsForAllMembers() {
        CreateClassroomResponse room = service.createClassroom();
        service.configureGroups(room.getRoomCode(), room.getTeacherToken(), true, 3);
        service.toggleAllowAnswers(room.getRoomCode(), room.getTeacherToken(), true);

        QuestionDto question = service.publishQuestion(room.getRoomCode(), room.getTeacherToken(), "What is an interface?");

        JoinStudentResponse s1 = service.joinStudent(room.getRoomCode(), "Alice");
        JoinStudentResponse s2 = service.joinStudent(room.getRoomCode(), "Bob");
        JoinStudentResponse s3 = service.joinStudent(room.getRoomCode(), "Charlie");

        // Alice creates Group Alpha and adds Bob
        ClassroomStateResponse gState = service.createGroup(room.getRoomCode(), s1.getStudentId(), "Alpha");
        String groupId = gState.getGroups().get(0).getGroupId();
        service.addMemberToGroup(room.getRoomCode(), groupId, s1.getStudentId(), s2.getStudentId());

        // Charlie remains unassigned
        ClassroomStateResponse beforeSubmit = service.getClassroomState(room.getRoomCode());
        assertEquals(0, beforeSubmit.getCompletedCount());
        assertEquals(3, beforeSubmit.getPendingCount());

        // Alice submits the answer for her team
        String teamAnswer = "An interface is a contract that defines abstract methods.";
        ClassroomStateResponse afterAliceSubmit = service.completeQuestion(
                room.getRoomCode(), s1.getStudentId(), question.getQuestionId(), teamAnswer);

        // Both Alice and Bob should now be COMPLETED with the exact same answer
        assertEquals(2, afterAliceSubmit.getCompletedCount());
        assertEquals(1, afterAliceSubmit.getPendingCount());

        StudentDto aliceDto = afterAliceSubmit.getStudents().stream().filter(s -> s.getStudentId().equals(s1.getStudentId())).findFirst().orElseThrow();
        StudentDto bobDto = afterAliceSubmit.getStudents().stream().filter(s -> s.getStudentId().equals(s2.getStudentId())).findFirst().orElseThrow();
        StudentDto charlieDto = afterAliceSubmit.getStudents().stream().filter(s -> s.getStudentId().equals(s3.getStudentId())).findFirst().orElseThrow();

        assertEquals(CompletionStatus.COMPLETED, aliceDto.getStatus());
        assertEquals(CompletionStatus.COMPLETED, bobDto.getStatus());
        assertEquals(CompletionStatus.PENDING, charlieDto.getStatus());

        assertEquals(teamAnswer, aliceDto.getAnswers().get(question.getQuestionId()));
        assertEquals(teamAnswer, bobDto.getAnswers().get(question.getQuestionId()));
        assertNull(charlieDto.getAnswers().get(question.getQuestionId()));
    }
}

