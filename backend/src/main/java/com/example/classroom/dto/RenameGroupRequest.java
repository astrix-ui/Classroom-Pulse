package com.example.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RenameGroupRequest {
    @NotBlank(message = "Group name cannot be blank")
    @Size(min = 1, max = 50, message = "Group name must be between 1 and 50 characters")
    private String groupName;

    public RenameGroupRequest() {}

    public RenameGroupRequest(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}

