package com.example.classroom.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignStudentGroupRequest {

    @NotBlank(message = "studentId is required")
    private String studentId;

    public AssignStudentGroupRequest() {}

    public AssignStudentGroupRequest(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}

