package com.example.classroom.dto;

import com.example.classroom.model.StudentGroup;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StudentGroupDto {
    private String groupId;
    private String groupName;
    private String creatorStudentId;
    private String creatorStudentName;
    private Instant createdAt;
    private List<GroupMemberDto> members;

    public StudentGroupDto() {}

    public StudentGroupDto(String groupId, String groupName, String creatorStudentId, String creatorStudentName, Instant createdAt, List<GroupMemberDto> members) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.creatorStudentId = creatorStudentId;
        this.creatorStudentName = creatorStudentName;
        this.createdAt = createdAt;
        this.members = members;
    }

    public static StudentGroupDto fromDomain(StudentGroup group) {
        if (group == null) return null;
        List<GroupMemberDto> memberDtos = group.getMembers() != null
                ? group.getMembers().stream().map(GroupMemberDto::fromDomain).collect(Collectors.toList())
                : Collections.emptyList();

        return new StudentGroupDto(
                group.getGroupId(),
                group.getGroupName(),
                group.getCreatorStudentId(),
                group.getCreatorStudentName(),
                group.getCreatedAt(),
                memberDtos
        );
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getCreatorStudentId() {
        return creatorStudentId;
    }

    public void setCreatorStudentId(String creatorStudentId) {
        this.creatorStudentId = creatorStudentId;
    }

    public String getCreatorStudentName() {
        return creatorStudentName;
    }

    public void setCreatorStudentName(String creatorStudentName) {
        this.creatorStudentName = creatorStudentName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<GroupMemberDto> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMemberDto> members) {
        this.members = members;
    }
}

