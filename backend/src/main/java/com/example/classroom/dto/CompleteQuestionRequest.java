package com.example.classroom.dto;

import jakarta.validation.constraints.NotBlank;

public class CompleteQuestionRequest {
    @NotBlank(message = "Question ID is required")
    private String questionId;
    private String answerText;

    public CompleteQuestionRequest() {}

    public CompleteQuestionRequest(String questionId) {
        this.questionId = questionId;
    }

    public CompleteQuestionRequest(String questionId, String answerText) {
        this.questionId = questionId;
        this.answerText = answerText;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
}

