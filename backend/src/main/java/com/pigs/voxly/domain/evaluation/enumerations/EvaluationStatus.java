package com.pigs.voxly.domain.evaluation.enumerations;

import com.pigs.voxly.sharedKernel.domain.types.Enumeration;

public final class EvaluationStatus extends Enumeration {

    public static final EvaluationStatus PENDING = new EvaluationStatus(1, "pending");
    public static final EvaluationStatus TRANSCRIBING = new EvaluationStatus(2, "transcribing");
    public static final EvaluationStatus ANALYZING = new EvaluationStatus(3, "analyzing");
    public static final EvaluationStatus COMPLETED = new EvaluationStatus(4, "completed");
    public static final EvaluationStatus FAILED = new EvaluationStatus(5, "failed");

    private EvaluationStatus(int id, String name) {
        super(id, name);
    }

    public static EvaluationStatus fromName(String name) {
        return switch (name.toLowerCase()) {
            case "pending" -> PENDING;
            case "transcribing" -> TRANSCRIBING;
            case "analyzing" -> ANALYZING;
            case "completed" -> COMPLETED;
            case "failed" -> FAILED;
            default -> PENDING;
        };
    }
}
