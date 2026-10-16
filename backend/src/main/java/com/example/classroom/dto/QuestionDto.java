package com.example.classroom.dto;

import com.example.classroom.model.Question;

import java.time.Instant;

public class QuestionDto {
    private String questionId;
    private String questionText;
    private int sequenceNumber;
    private Instant publishedAt;

    public QuestionDto() {}

    public QuestionDto(String questionId, String questionText, int sequenceNumber, Instant publishedAt) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.sequenceNumber = sequenceNumber;
        this.publishedAt = publishedAt;
    }

    public static QuestionDto fromDomain(Question question) {
        if (question == null) return null;
        return new QuestionDto(
                question.getQuestionId(),
                question.getQuestionText(),
                question.getSequenceNumber(),
                question.getPublishedAt()
        );
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}

