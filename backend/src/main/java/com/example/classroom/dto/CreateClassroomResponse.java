package com.example.classroom.dto;

public class CreateClassroomResponse {
    private String roomCode;
    private String teacherToken;

    public CreateClassroomResponse() {}

    public CreateClassroomResponse(String roomCode, String teacherToken) {
        this.roomCode = roomCode;
        this.teacherToken = teacherToken;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getTeacherToken() {
        return teacherToken;
    }

    public void setTeacherToken(String teacherToken) {
        this.teacherToken = teacherToken;
    }
}

