package com.example.classroom.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class StudentGroup {
    private final String groupId;
    private volatile String groupName;
    private final String creatorStudentId;
    private final String creatorStudentName;
    private final Instant createdAt;
    private final List<GroupMember> members = new CopyOnWriteArrayList<>();

    public StudentGroup(String groupId, String groupName, String creatorStudentId, String creatorStudentName, Instant createdAt) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.creatorStudentId = creatorStudentId;
        this.creatorStudentName = creatorStudentName;
        this.createdAt = createdAt;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public synchronized void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getCreatorStudentId() {
        return creatorStudentId;
    }

    public String getCreatorStudentName() {
        return creatorStudentName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<GroupMember> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public synchronized boolean hasMember(String studentId) {
        if (studentId == null) return false;
        return members.stream().anyMatch(m -> studentId.equals(m.getStudentId()));
    }

    public synchronized void addMember(GroupMember member) {
        if (member != null && !hasMember(member.getStudentId())) {
            members.add(member);
        }
    }

    public synchronized boolean removeMember(String studentId) {
        if (studentId == null) return false;
        return members.removeIf(m -> studentId.equals(m.getStudentId()));
    }

    public int getMemberCount() {
        return members.size();
    }
}

