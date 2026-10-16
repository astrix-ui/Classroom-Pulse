package com.example.classroom.dto;

import jakarta.validation.constraints.Min;

public class ConfigureGroupsRequest {
    private boolean enabled;

    @Min(value = 2, message = "Group size must be at least 2")
    private int maxGroupSize = 2;

    public ConfigureGroupsRequest() {}

    public ConfigureGroupsRequest(boolean enabled, int maxGroupSize) {
        this.enabled = enabled;
        this.maxGroupSize = maxGroupSize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxGroupSize() {
        return maxGroupSize;
    }

    public void setMaxGroupSize(int maxGroupSize) {
        this.maxGroupSize = maxGroupSize;
    }
}

