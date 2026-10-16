package com.example.classroom.dto;

import jakarta.validation.constraints.NotBlank;

public class AddGroupMemberRequest {
    @NotBlank(message = "Requesting student ID cannot be blank")
    private String requestingStudentId;

    @NotBlank(message = "Target student ID cannot be blank")
    private String targetStudentId;

    public AddGroupMemberRequest() {}

    public AddGroupMemberRequest(String requestingStudentId, String targetStudentId) {
        this.requestingStudentId = requestingStudentId;
        this.targetStudentId = targetStudentId;
    }

    public String getRequestingStudentId() {
        return requestingStudentId;
    }

    public void setRequestingStudentId(String requestingStudentId) {
        this.requestingStudentId = requestingStudentId;
    }

    public String getTargetStudentId() {
        return targetStudentId;
    }

    public void setTargetStudentId(String targetStudentId) {
        this.targetStudentId = targetStudentId;
    }
}

