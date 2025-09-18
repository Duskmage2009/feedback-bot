package com.autoservice.feedbackbot.enums;

public enum Position {
    MECHANIC("Mechanic"),
    ELECTRICIAN("Electrician"),
    MANAGER("Manager");

    private final String displayName;

    Position(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}