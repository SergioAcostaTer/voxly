package com.pigs.voxly.application.shared.ports;

import com.pigs.voxly.sharedKernel.domain.results.ResultT;

import java.util.List;

/**
 * Port interface for AI-powered speech analysis.
 * Implementations can use OpenAI GPT, Azure OpenAI, custom models, etc.
 */
public interface SpeechAnalysisService {

    /**
     * Analyzes transcribed text and returns feedback notes.
     *
     * @param transcription the transcription result from TranscriptionService
     * @param sessionType   the type of presentation (e.g., "presentation", "interview")
     * @return ResultT containing analysis on success
     */
    ResultT<AnalysisResult> analyze(
            TranscriptionService.TranscriptionResult transcription,
            String sessionType
    );

    /**
     * Represents the complete analysis result.
     */
    record AnalysisResult(
            Metrics metrics,
            List<FeedbackNote> feedbackNotes,
            String overallSummary,
            List<String> strengths,
            List<String> areasForImprovement
    ) {}

    /**
     * Calculated presentation metrics.
     */
    record Metrics(
            int wordsPerMinute,
            int totalWords,
            double durationMinutes,
            int fillerWordCount,
            List<FillerWordOccurrence> fillerWords,
            int pauseCount,
            List<PauseOccurrence> pauses,
            double averageSentenceLength,
            double clarityScore
    ) {}

    /**
     * A filler word occurrence with timestamp.
     */
    record FillerWordOccurrence(
            String word,
            double timestampSeconds,
            int count
    ) {}

    /**
     * A pause occurrence with timing.
     */
    record PauseOccurrence(
            double startSeconds,
            double endSeconds,
            double durationSeconds,
            String type // "natural", "awkward", "dramatic"
    ) {}

    /**
     * A feedback note linked to a specific timestamp.
     */
    record FeedbackNote(
            String category, // FILLER_WORDS, PACING, CLARITY, PAUSES, SUGGESTION
            String severity, // INFO, WARNING, CRITICAL
            String message,
            Double timestampSeconds, // null for general notes
            Double endTimestampSeconds
    ) {}
}
