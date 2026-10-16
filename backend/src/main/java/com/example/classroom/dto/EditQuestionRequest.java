package com.example.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EditQuestionRequest {
    @NotBlank(message = "Question text cannot be blank")
    @Size(max = 500, message = "Question text cannot exceed 500 characters")
    private String questionText;

    public EditQuestionRequest() {}

    public EditQuestionRequest(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }
}
