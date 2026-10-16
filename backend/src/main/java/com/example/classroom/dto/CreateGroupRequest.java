package com.example.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateGroupRequest {
    @NotBlank(message = "Student ID cannot be blank")
    private String studentId;

    @Size(max = 50, message = "Group name cannot exceed 50 characters")
    private String groupName;

    public CreateGroupRequest() {}

    public CreateGroupRequest(String studentId, String groupName) {
        this.studentId = studentId;
        this.groupName = groupName;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}

