package com.example.classroom.dto;

import com.example.classroom.model.GroupMember;

import java.time.Instant;

public class GroupMemberDto {
    private String studentId;
    private String studentName;
    private Instant joinedAt;

    public GroupMemberDto() {}

    public GroupMemberDto(String studentId, String studentName, Instant joinedAt) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.joinedAt = joinedAt;
    }

    public static GroupMemberDto fromDomain(GroupMember member) {
        if (member == null) return null;
        return new GroupMemberDto(member.getStudentId(), member.getStudentName(), member.getJoinedAt());
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}

