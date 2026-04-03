package com.pigs.voxly.domain.sessions.enumerations;

import com.pigs.voxly.sharedKernel.domain.types.Enumeration;

public final class SessionStatus extends Enumeration {

    public static final SessionStatus DRAFT = new SessionStatus(1, "draft");
    public static final SessionStatus UPLOADED = new SessionStatus(2, "uploaded");
    public static final SessionStatus ANALYZING = new SessionStatus(3, "analyzing");
    public static final SessionStatus COMPLETED = new SessionStatus(4, "completed");
    public static final SessionStatus FAILED = new SessionStatus(5, "failed");

    private SessionStatus(int id, String name) {
        super(id, name);
    }

    public static SessionStatus fromName(String name) {
        return switch (name.toLowerCase()) {
            case "draft" -> DRAFT;
            case "uploaded" -> UPLOADED;
            case "analyzing" -> ANALYZING;
            case "completed" -> COMPLETED;
            case "failed" -> FAILED;
            default -> DRAFT;
        };
    }

    public static SessionStatus fromId(int id) {
        return switch (id) {
            case 1 -> DRAFT;
            case 2 -> UPLOADED;
            case 3 -> ANALYZING;
            case 4 -> COMPLETED;
            case 5 -> FAILED;
            default -> DRAFT;
        };
    }
}
