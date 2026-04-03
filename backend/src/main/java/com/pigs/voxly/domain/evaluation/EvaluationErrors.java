package com.pigs.voxly.domain.evaluation;

import com.pigs.voxly.sharedKernel.domain.results.Error;

import java.util.UUID;

public final class EvaluationErrors {

    private EvaluationErrors() {}

    public static final Error EVALUATION_NOT_FOUND = Error.notFound("Evaluation.NotFound", "Evaluation not found");

    public static Error evaluationNotFoundById(UUID id) {
        return Error.notFound("Evaluation.NotFoundById", "Evaluation with ID '" + id + "' not found");
    }

    public static final Error ALREADY_COMPLETED = Error.validation("Evaluation.AlreadyCompleted", "Evaluation has already been completed");

    public static final Error TRANSCRIPTION_FAILED = Error.failure("Evaluation.TranscriptionFailed", "Failed to transcribe media");

    public static final Error ANALYSIS_FAILED = Error.failure("Evaluation.AnalysisFailed", "Failed to analyze transcription");

    public static final Error SESSION_NOT_FOUND = Error.notFound("Evaluation.SessionNotFound", "Associated session not found");
}
