package com.example.classroom.service;

import com.example.classroom.dto.*;
import com.example.classroom.exception.*;
import com.example.classroom.model.*;
import com.example.classroom.repository.ClassroomMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassroomService {

    private static final Logger log = LoggerFactory.getLogger(ClassroomService.class);

    // Characters excluding 0, O, 1, I for unambiguous room codes
    private static final String ROOM_CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int ROOM_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClassroomMemoryRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public ClassroomService(ClassroomMemoryRepository repository, SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    public CreateClassroomResponse createClassroom() {
        String roomCode = generateUniqueRoomCode();
        String teacherToken = UUID.randomUUID().toString();
        ClassroomSession session = new ClassroomSession(roomCode, teacherToken, Instant.now());
        repository.save(session);
        log.info("Classroom created with code: {}", roomCode);
        return new CreateClassroomResponse(roomCode, teacherToken);
    }

    public JoinStudentResponse joinStudent(String roomCode, String studentName) {
        ClassroomSession session = getSessionOrThrow(roomCode);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        String cleanName = studentName.trim().replaceAll("\\s+", " ");
        if (cleanName.length() < 2 || cleanName.length() > 50) {
            throw new IllegalArgumentException("Student name must be between 2 and 50 characters");
        }

        synchronized (session) {
            if (session.hasStudentWithName(cleanName)) {
                throw new DuplicateStudentException(cleanName);
            }

            String studentId = UUID.randomUUID().toString();
            StudentStatus student = new StudentStatus(studentId, cleanName, Instant.now());
            session.addStudent(student);

            // Broadcast safe updated state
            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.STUDENT_JOINED, session.getRoomCode(), state);

            return new JoinStudentResponse(studentId, cleanName, session.getRoomCode(), student.getStatus());
        }
    }

    public ClassroomStateResponse leaveStudent(String roomCode, String studentId) {
        ClassroomSession session = getSessionOrThrow(roomCode);

        synchronized (session) {
            boolean groupUpdated = false;

            // When a student leaves the room, ensure they are also removed from any groups
            for (StudentGroup group : new ArrayList<>(session.getGroups())) {
                if (group.hasMember(studentId)) {
                    group.removeMember(studentId);
                    groupUpdated = true;
                    // If the group is now empty, disband the group
                    if (group.getMemberCount() == 0) {
                        session.removeGroup(group.getGroupId());
                    }
                }
            }

            StudentStatus removed = session.removeStudent(studentId);
            ClassroomStateResponse state = toSafeState(session);
            if (removed != null || groupUpdated) {
                broadcastEvent(ClassroomEventType.STUDENT_LEFT, session.getRoomCode(), state);
                if (groupUpdated) {
                    broadcastEvent(ClassroomEventType.GROUPS_UPDATED, session.getRoomCode(), state);
                }
                log.info("Student {} ({}) left room {} (groupUpdated={})",
                        removed != null ? removed.getStudentName() : studentId, studentId, roomCode, groupUpdated);
            }
            return state;
        }
    }

    public ClassroomStateResponse getClassroomState(String roomCode) {
        ClassroomSession session = getSessionOrThrow(roomCode);
        return toSafeState(session);
    }

    public QuestionDto publishQuestion(String roomCode, String teacherToken, String questionText) {
        ClassroomSession session = getSessionOrThrow(roomCode);
        verifyTeacherToken(session, teacherToken);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        if (questionText == null || questionText.trim().isEmpty()) {
            throw new IllegalArgumentException("Question text cannot be blank");
        }
        if (questionText.trim().length() > 500) {
            throw new IllegalArgumentException("Question text cannot exceed 500 characters");
        }

        synchronized (session) {
            String questionId = UUID.randomUUID().toString();
            int sequenceNumber = session.nextSequenceNumber();
            Question question = new Question(questionId, questionText.trim(), sequenceNumber, Instant.now());

            session.setCurrentQuestion(question);
            session.addQuestionToHistory(question);
            session.resetAllStudentsToPending();

            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.QUESTION_PUBLISHED, session.getRoomCode(), state);

            return QuestionDto.fromDomain(question);
        }
    }

    public QuestionDto editQuestion(String roomCode, String teacherToken, String questionText) {
        ClassroomSession session = getSessionOrThrow(roomCode);
        verifyTeacherToken(session, teacherToken);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        if (questionText == null || questionText.trim().isEmpty()) {
            throw new IllegalArgumentException("Question text cannot be blank");
        }
        if (questionText.trim().length() > 500) {
            throw new IllegalArgumentException("Question text cannot exceed 500 characters");
        }

        synchronized (session) {
            Question current = session.getCurrentQuestion();
            if (current == null) {
                throw new InvalidClassroomStateException("No question has been published to edit");
            }

            Question updated = new Question(
                current.getQuestionId(),
                questionText.trim(),
                current.getSequenceNumber(),
                current.getPublishedAt()
            );

            session.setCurrentQuestion(updated);
            session.updateQuestionInHistory(updated);

            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.QUESTION_UPDATED, session.getRoomCode(), state);
            log.info("Question updated in room {}: {}", roomCode, questionText.trim());

            return QuestionDto.fromDomain(updated);
        }
    }

    public ClassroomStateResponse deleteQuestion(String roomCode, String teacherToken) {
        ClassroomSession session = getSessionOrThrow(roomCode);
        verifyTeacherToken(session, teacherToken);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        synchronized (session) {
            Question current = session.getCurrentQuestion();
            if (current != null) {
                session.removeQuestionFromHistory(current.getQuestionId());
                session.getStudents().values().forEach(s -> s.removeAnsweredQuestion(current.getQuestionId()));
            }

            session.setCurrentQuestion(null);
            session.resetAllStudentsToPending();

            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.QUESTION_DELETED, session.getRoomCode(), state);
            log.info("Question deleted in room {}", roomCode);

            return state;
        }
    }

    public ClassroomStateResponse completeQuestion(String roomCode, String studentId, String questionId) {
        return completeQuestion(roomCode, studentId, questionId, null);
    }

    public ClassroomStateResponse completeQuestion(String roomCode, String studentId, String questionId, String answerText) {
        ClassroomSession session = getSessionOrThrow(roomCode);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        StudentStatus student = session.getStudent(studentId);
        if (student == null) {
            throw new StudentNotFoundException(studentId);
        }

        Question currentQuestion = session.getCurrentQuestion();
        if (currentQuestion == null) {
            throw new InvalidClassroomStateException("No question has been published yet");
        }

        if (questionId == null || !currentQuestion.getQuestionId().equals(questionId.trim())) {
            throw new InvalidClassroomStateException("The question is no longer active or question ID is invalid");
        }

        // When written answers are enabled, validate that an answer is provided
        if (session.isAllowAnswers()) {
            if (answerText == null || answerText.trim().isEmpty()) {
                throw new IllegalArgumentException("Answer text is required when answers are enabled");
            }
            if (answerText.trim().length() > 2000) {
                throw new IllegalArgumentException("Answer text cannot exceed 2000 characters");
            }
        }

        Instant now = Instant.now();
        boolean anyUpdated = false;

        synchronized (session) {
            String groupId = student.getGroupId();
            StudentGroup group = (session.isGroupsEnabled() && groupId != null)
                    ? session.findGroupById(groupId)
                    : null;

            if (group != null) {
                // If student is part of an active group, submit answer on behalf of all group members
                for (GroupMember member : group.getMembers()) {
                    StudentStatus memberStatus = session.getStudent(member.getStudentId());
                    if (memberStatus != null) {
                        boolean updated = memberStatus.markCompleted(currentQuestion.getQuestionId(), answerText, now);
                        if (updated) {
                            anyUpdated = true;
                        }
                    }
                }
                log.info("Student {} submitted answer for group {} ({} members) in room {}",
                        student.getStudentName(), group.getGroupName(), group.getMemberCount(), roomCode);
            } else {
                anyUpdated = student.markCompleted(currentQuestion.getQuestionId(), answerText, now);
                log.info("Student {} submitted individual answer in room {}", student.getStudentName(), roomCode);
            }

            ClassroomStateResponse state = toSafeState(session);
            if (anyUpdated) {
                broadcastEvent(ClassroomEventType.STUDENT_STATUS_CHANGED, session.getRoomCode(), state);
            }

            return state;
        }
    }

    public ClassroomStateResponse resetClassroom(String roomCode, String teacherToken) {
        ClassroomSession session = getSessionOrThrow(roomCode);
        verifyTeacherToken(session, teacherToken);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        synchronized (session) {
            session.resetAllStudentsToPending();
            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.CLASSROOM_RESET, session.getRoomCode(), state);
            return state;
        }
    }

    public ClassroomStateResponse toggleAllowAnswers(String roomCode, String teacherToken, boolean allowAnswers) {
        ClassroomSession session = getSessionOrThrow(roomCode);
        verifyTeacherToken(session, teacherToken);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        synchronized (session) {
            session.setAllowAnswers(allowAnswers);
            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.ALLOW_ANSWERS_CHANGED, session.getRoomCode(), state);
            log.info("Allow answers toggled to {} in room {}", allowAnswers, roomCode);
            return state;
        }
    }

    public ClassroomStateResponse closeClassroom(String roomCode, String teacherToken) {
        ClassroomSession session = getSessionOrThrow(roomCode);
        verifyTeacherToken(session, teacherToken);

        synchronized (session) {
            session.setActive(false);
            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.CLASSROOM_CLOSED, session.getRoomCode(), state);
            return state;
        }
    }

    public void updateStudentConnectionStatus(String roomCode, String studentId, boolean connected) {
        repository.findById(roomCode).ifPresent(session -> {
            StudentStatus student = session.getStudent(studentId);
            if (student != null) {
                student.setConnected(connected);
                ClassroomStateResponse state = toSafeState(session);
                broadcastEvent(ClassroomEventType.CLASSROOM_STATE_UPDATED, session.getRoomCode(), state);
            }
        });
    }

    private void broadcastEvent(ClassroomEventType type, String roomCode, Object payload) {
        try {
            WebSocketEvent event = WebSocketEvent.of(type, roomCode, payload);
            String destination = "/topic/classrooms/" + roomCode;
            messagingTemplate.convertAndSend(destination, event);
            log.debug("Broadcast event {} to {}", type, destination);
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket event {} to room {}", type, roomCode, e);
        }
    }

    private ClassroomSession getSessionOrThrow(String roomCode) {
        return repository.findById(roomCode)
                .orElseThrow(() -> new ClassroomNotFoundException(roomCode));
    }

    private void verifyTeacherToken(ClassroomSession session, String teacherToken) {
        if (teacherToken == null || !teacherToken.equals(session.getTeacherToken())) {
            throw new UnauthorizedTeacherException("Invalid or missing teacher token");
        }
    }

    private String generateUniqueRoomCode() {
        for (int i = 0; i < 1000; i++) {
            StringBuilder sb = new StringBuilder(ROOM_CODE_LENGTH);
            for (int j = 0; j < ROOM_CODE_LENGTH; j++) {
                int index = RANDOM.nextInt(ROOM_CODE_CHARS.length());
                sb.append(ROOM_CODE_CHARS.charAt(index));
            }
            String code = sb.toString();
            if (!repository.existsById(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate a unique room code");
    }

    public ClassroomStateResponse toSafeState(ClassroomSession session) {
        List<StudentDto> studentDtos = session.getStudents().values().stream()
                .map(StudentDto::fromDomain)
                .sorted(Comparator
                        .comparing((StudentDto s) -> s.getStatus() == CompletionStatus.PENDING ? 0 : 1)
                        .thenComparing(s -> s.getStudentName().toLowerCase()))
                .collect(Collectors.toList());

        int total = studentDtos.size();
        int completed = (int) studentDtos.stream().filter(s -> s.getStatus() == CompletionStatus.COMPLETED).count();
        int pending = total - completed;
        double percentage = total > 0 ? Math.round((completed * 100.0 / total) * 10.0) / 10.0 : 0.0;

        ClassroomStateResponse state = new ClassroomStateResponse(
                session.getRoomCode(),
                session.isActive(),
                QuestionDto.fromDomain(session.getCurrentQuestion()),
                studentDtos,
                total,
                completed,
                pending,
                percentage,
                session.getCreatedAt()
        );
        state.setAllowAnswers(session.isAllowAnswers());
        state.setQuestions(session.getQuestionHistory().stream()
                .map(QuestionDto::fromDomain)
                .collect(Collectors.toList()));
        state.setGroupsEnabled(session.isGroupsEnabled());
        state.setMaxGroupSize(session.getMaxGroupSize());
        state.setGroups(session.getGroups().stream()
                .map(StudentGroupDto::fromDomain)
                .collect(Collectors.toList()));

        return state;
    }

    public ClassroomStateResponse configureGroups(String roomCode, String teacherToken, boolean enabled, int maxGroupSize) {
        ClassroomSession session = getSessionOrThrow(roomCode);
        verifyTeacherToken(session, teacherToken);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        if (maxGroupSize < 2) {
            throw new IllegalArgumentException("Group size must be at least 2");
        }

        synchronized (session) {
            session.setGroupsEnabled(enabled);
            session.setMaxGroupSize(maxGroupSize);

            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.GROUPS_UPDATED, session.getRoomCode(), state);
            log.info("Groups configured in room {}: enabled={}, maxGroupSize={}", roomCode, enabled, maxGroupSize);
            return state;
        }
    }

    public ClassroomStateResponse resetGroups(String roomCode, String teacherToken) {
        ClassroomSession session = getSessionOrThrow(roomCode);
        verifyTeacherToken(session, teacherToken);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        synchronized (session) {
            for (StudentStatus student : session.getStudents().values()) {
                student.setGroupId(null);
                student.setGroupName(null);
            }
            session.clearGroups();

            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.GROUPS_UPDATED, session.getRoomCode(), state);
            log.info("Groups reset in room {}", roomCode);
            return state;
        }
    }

    public ClassroomStateResponse createGroup(String roomCode, String studentId, String groupName) {
        ClassroomSession session = getSessionOrThrow(roomCode);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        if (!session.isGroupsEnabled()) {
            throw new InvalidClassroomStateException("Group formation is not enabled by teacher");
        }

        StudentStatus creator = session.getStudent(studentId);
        if (creator == null) {
            throw new StudentNotFoundException(studentId);
        }

        synchronized (session) {
            if (session.isStudentInAnyGroup(studentId)) {
                throw new GroupConflictException("You are already in a group");
            }

            String finalGroupName = (groupName != null && !groupName.trim().isEmpty())
                    ? groupName.trim()
                    : "Group " + (session.getGroups().size() + 1);

            String groupId = UUID.randomUUID().toString();
            StudentGroup group = new StudentGroup(groupId, finalGroupName, studentId, creator.getStudentName(), Instant.now());
            group.addMember(new GroupMember(studentId, creator.getStudentName(), Instant.now()));

            creator.setGroupId(groupId);
            creator.setGroupName(finalGroupName);
            session.addGroup(group);

            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.GROUPS_UPDATED, session.getRoomCode(), state);
            log.info("Student {} created group {} in room {}", creator.getStudentName(), finalGroupName, roomCode);
            return state;
        }
    }

    public ClassroomStateResponse addMemberToGroup(String roomCode, String groupId, String requestingStudentId, String targetStudentId) {
        ClassroomSession session = getSessionOrThrow(roomCode);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        if (!session.isGroupsEnabled()) {
            throw new InvalidClassroomStateException("Group formation is not enabled by teacher");
        }

        synchronized (session) {
            StudentGroup group = session.findGroupById(groupId);
            if (group == null) {
                throw new IllegalArgumentException("Group not found");
            }

            if (!group.hasMember(requestingStudentId)) {
                throw new IllegalArgumentException("Only members of this group can add other students");
            }

            if (group.getMemberCount() >= session.getMaxGroupSize()) {
                throw new IllegalStateException("Group is already full (maximum " + session.getMaxGroupSize() + " members)");
            }

            StudentStatus targetStudent = session.getStudent(targetStudentId);
            if (targetStudent == null) {
                throw new StudentNotFoundException(targetStudentId);
            }

            // Concurrency check: if student was already added by another group
            if (session.isStudentInAnyGroup(targetStudentId)) {
                throw new GroupConflictException("Student '" + targetStudent.getStudentName() + "' has already been added to another group.");
            }

            group.addMember(new GroupMember(targetStudentId, targetStudent.getStudentName(), Instant.now()));
            targetStudent.setGroupId(group.getGroupId());
            targetStudent.setGroupName(group.getGroupName());

            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.GROUPS_UPDATED, session.getRoomCode(), state);
            log.info("Student {} added to group {} in room {}", targetStudent.getStudentName(), group.getGroupName(), roomCode);
            return state;
        }
    }

    public ClassroomStateResponse leaveGroup(String roomCode, String groupId, String studentId) {
        ClassroomSession session = getSessionOrThrow(roomCode);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        synchronized (session) {
            StudentGroup group = session.findGroupById(groupId);
            if (group != null) {
                group.removeMember(studentId);
                StudentStatus student = session.getStudent(studentId);
                if (student != null) {
                    student.setGroupId(null);
                    student.setGroupName(null);
                }

                if (group.getMemberCount() == 0) {
                    session.removeGroup(groupId);
                }
            }

            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.GROUPS_UPDATED, session.getRoomCode(), state);
            log.info("Student {} left group {} in room {}", studentId, groupId, roomCode);
            return state;
        }
    }

    public ClassroomStateResponse removeStudentFromGroupByTeacher(String roomCode, String teacherToken, String groupId, String studentId) {
        ClassroomSession session = getSessionOrThrow(roomCode);
        verifyTeacherToken(session, teacherToken);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        synchronized (session) {
            StudentGroup group = session.findGroupById(groupId);
            if (group != null) {
                group.removeMember(studentId);
                StudentStatus student = session.getStudent(studentId);
                if (student != null) {
                    student.setGroupId(null);
                    student.setGroupName(null);
                }

                if (group.getMemberCount() == 0) {
                    session.removeGroup(groupId);
                }
            }

            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.GROUPS_UPDATED, session.getRoomCode(), state);
            log.info("Teacher removed student {} from group {} in room {}", studentId, groupId, roomCode);
            return state;
        }
    }

    public ClassroomStateResponse assignStudentToGroupByTeacher(String roomCode, String teacherToken, String targetGroupId, String studentId) {
        ClassroomSession session = getSessionOrThrow(roomCode);
        verifyTeacherToken(session, teacherToken);

        if (!session.isActive()) {
            throw new InvalidClassroomStateException("Classroom session is closed");
        }

        synchronized (session) {
            StudentGroup targetGroup = session.findGroupById(targetGroupId);
            if (targetGroup == null) {
                throw new IllegalArgumentException("Target group not found");
            }

            if (targetGroup.getMemberCount() >= session.getMaxGroupSize()) {
                throw new IllegalStateException("Target group is already full (maximum " + session.getMaxGroupSize() + " members)");
            }

            StudentStatus student = session.getStudent(studentId);
            if (student == null) {
                throw new StudentNotFoundException(studentId);
            }

            // If the student is already in target group, no-op
            if (targetGroup.hasMember(studentId)) {
                return toSafeState(session);
            }

            // If student is currently in any other group, remove them from that group first
            for (StudentGroup g : new ArrayList<>(session.getGroups())) {
                if (g.hasMember(studentId)) {
                    g.removeMember(studentId);
                    if (g.getMemberCount() == 0) {
                        session.removeGroup(g.getGroupId());
                    }
                    break;
                }
            }

            targetGroup.addMember(new GroupMember(studentId, student.getStudentName(), Instant.now()));
            student.setGroupId(targetGroup.getGroupId());
            student.setGroupName(targetGroup.getGroupName());

            ClassroomStateResponse state = toSafeState(session);
            broadcastEvent(ClassroomEventType.GROUPS_UPDATED, session.getRoomCode(), state);
            log.info("Teacher assigned student {} to group {} in room {}", student.getStudentName(), targetGroup.getGroupName(), roomCode);
            return state;
        }
    }
}

