package com.pigs.voxly.domain.sessions.enumerations;

import com.pigs.voxly.sharedKernel.domain.types.Enumeration;

public final class SessionType extends Enumeration {

    public static final SessionType PRESENTATION = new SessionType(1, "presentation");
    public static final SessionType INTERVIEW = new SessionType(2, "interview");
    public static final SessionType PITCH = new SessionType(3, "pitch");
    public static final SessionType FREESTYLE = new SessionType(4, "freestyle");

    private SessionType(int id, String name) {
        super(id, name);
    }

    public static SessionType fromName(String name) {
        return switch (name.toLowerCase()) {
            case "presentation" -> PRESENTATION;
            case "interview" -> INTERVIEW;
            case "pitch" -> PITCH;
            case "freestyle" -> FREESTYLE;
            default -> PRESENTATION;
        };
    }

    public static SessionType fromId(int id) {
        return switch (id) {
            case 1 -> PRESENTATION;
            case 2 -> INTERVIEW;
            case 3 -> PITCH;
            case 4 -> FREESTYLE;
            default -> PRESENTATION;
        };
    }
}
