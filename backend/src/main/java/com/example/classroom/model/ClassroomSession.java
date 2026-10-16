package com.example.classroom.model;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassroomSession {
    private final String roomCode;
    private final String teacherToken;
    private final Instant createdAt;
    private volatile boolean active;
    private volatile Question currentQuestion;
    private volatile boolean allowAnswers = false;
    private volatile boolean groupsEnabled = false;
    private volatile int maxGroupSize = 2;
    private final List<StudentGroup> groups = new CopyOnWriteArrayList<>();
    private final AtomicInteger sequenceCounter = new AtomicInteger(0);

    // Historical questions asked during the session
    private final List<Question> questionHistory = new CopyOnWriteArrayList<>();

    // Keyed by studentId
    private final Map<String, StudentStatus> students = new ConcurrentHashMap<>();

    public ClassroomSession(String roomCode, String teacherToken, Instant createdAt) {
        this.roomCode = roomCode;
        this.teacherToken = teacherToken;
        this.createdAt = createdAt;
        this.active = true;
        this.currentQuestion = null;
        this.allowAnswers = false;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getTeacherToken() {
        return teacherToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Question getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(Question currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public int nextSequenceNumber() {
        return sequenceCounter.incrementAndGet();
    }

    public Map<String, StudentStatus> getStudents() {
        return Collections.unmodifiableMap(students);
    }

    public StudentStatus getStudent(String studentId) {
        return students.get(studentId);
    }

    public boolean hasStudentWithName(String name) {
        return students.values().stream()
                .anyMatch(s -> s.getStudentName().equalsIgnoreCase(name.trim()));
    }

    public void addStudent(StudentStatus student) {
        students.put(student.getStudentId(), student);
    }

    public StudentStatus removeStudent(String studentId) {
        return students.remove(studentId);
    }

    public boolean isAllowAnswers() {
        return allowAnswers;
    }

    public void setAllowAnswers(boolean allowAnswers) {
        this.allowAnswers = allowAnswers;
    }

    public List<Question> getQuestionHistory() {
        return Collections.unmodifiableList(questionHistory);
    }

    public void addQuestionToHistory(Question question) {
        if (question != null) {
            this.questionHistory.add(question);
        }
    }

    public void updateQuestionInHistory(Question updated) {
        if (updated == null) return;
        for (int i = 0; i < questionHistory.size(); i++) {
            if (questionHistory.get(i).getQuestionId().equals(updated.getQuestionId())) {
                questionHistory.set(i, updated);
                break;
            }
        }
    }

    public void removeQuestionFromHistory(String questionId) {
        if (questionId == null) return;
        questionHistory.removeIf(q -> q.getQuestionId().equals(questionId));
    }

    public void resetAllStudentsToPending() {
        students.values().forEach(StudentStatus::resetToPending);
    }

    public boolean isGroupsEnabled() {
        return groupsEnabled;
    }

    public void setGroupsEnabled(boolean groupsEnabled) {
        this.groupsEnabled = groupsEnabled;
    }

    public int getMaxGroupSize() {
        return maxGroupSize;
    }

    public void setMaxGroupSize(int maxGroupSize) {
        this.maxGroupSize = maxGroupSize;
    }

    public List<StudentGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public void addGroup(StudentGroup group) {
        if (group != null) {
            this.groups.add(group);
        }
    }

    public boolean removeGroup(String groupId) {
        if (groupId == null) return false;
        return this.groups.removeIf(g -> groupId.equals(g.getGroupId()));
    }

    public void clearGroups() {
        this.groups.clear();
    }

    public StudentGroup findGroupById(String groupId) {
        if (groupId == null) return null;
        return groups.stream()
                .filter(g -> groupId.equals(g.getGroupId()))
                .findFirst()
                .orElse(null);
    }

    public boolean isStudentInAnyGroup(String studentId) {
        if (studentId == null) return false;
        return groups.stream().anyMatch(g -> g.hasMember(studentId));
    }

    public StudentGroup findGroupForStudent(String studentId) {
        if (studentId == null) return null;
        return groups.stream()
                .filter(g -> g.hasMember(studentId))
                .findFirst()
                .orElse(null);
    }
}

