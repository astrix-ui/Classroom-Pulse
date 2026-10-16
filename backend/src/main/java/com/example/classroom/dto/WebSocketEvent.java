package com.example.classroom.dto;

import com.example.classroom.model.ClassroomEventType;

import java.time.Instant;

public class WebSocketEvent {
    private ClassroomEventType type;
    private String roomCode;
    private Instant timestamp;
    private Object payload;

    public WebSocketEvent() {}

    public WebSocketEvent(ClassroomEventType type, String roomCode, Instant timestamp, Object payload) {
        this.type = type;
        this.roomCode = roomCode;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public static WebSocketEvent of(ClassroomEventType type, String roomCode, Object payload) {
        return new WebSocketEvent(type, roomCode, Instant.now(), payload);
    }

    public ClassroomEventType getType() {
        return type;
    }

    public void setType(ClassroomEventType type) {
        this.type = type;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}

