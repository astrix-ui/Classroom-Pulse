package com.example.classroom.dto;

import jakarta.validation.constraints.NotBlank;

public class CompleteQuestionRequest {
    @NotBlank(message = "Question ID is required")
    private String questionId;

    public CompleteQuestionRequest() {}

    public CompleteQuestionRequest(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }
}

