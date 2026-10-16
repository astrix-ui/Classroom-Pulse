package com.example.classroom.model;

import java.time.Instant;

public class Question {
    private final String questionId;
    private final String questionText;
    private final int sequenceNumber;
    private final Instant publishedAt;

    public Question(String questionId, String questionText, int sequenceNumber, Instant publishedAt) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.sequenceNumber = sequenceNumber;
        this.publishedAt = publishedAt;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}

