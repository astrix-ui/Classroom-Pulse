package com.example.classroom.dto;

public class AllowAnswersRequest {
    private Boolean allowAnswers;

    public AllowAnswersRequest() {}

    public AllowAnswersRequest(Boolean allowAnswers) {
        this.allowAnswers = allowAnswers;
    }

    public Boolean getAllowAnswers() {
        return allowAnswers;
    }

    public void setAllowAnswers(Boolean allowAnswers) {
        this.allowAnswers = allowAnswers;
    }
}

