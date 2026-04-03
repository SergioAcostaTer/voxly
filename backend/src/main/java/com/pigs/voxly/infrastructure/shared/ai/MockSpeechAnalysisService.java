package com.pigs.voxly.infrastructure.shared.ai;

import com.pigs.voxly.application.shared.ports.SpeechAnalysisService;
import com.pigs.voxly.application.shared.ports.TranscriptionService;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mock implementation of SpeechAnalysisService for development and testing.
 * Performs basic text analysis without calling external AI APIs.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockSpeechAnalysisService implements SpeechAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(MockSpeechAnalysisService.class);

    private static final Pattern FILLER_PATTERN = Pattern.compile(
            "\\b(um|uh|like|basically|so|actually|you know|I mean|kind of|sort of)\\b",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public ResultT<AnalysisResult> analyze(
            TranscriptionService.TranscriptionResult transcription,
            String sessionType
    ) {
        log.info("Mock analysis for {} session, duration: {}s", sessionType, transcription.durationSeconds());

        String text = transcription.fullText();
        List<TranscriptionService.Segment> segments = transcription.segments();

        // Calculate basic metrics
        int totalWords = countWords(text);
        double durationMinutes = transcription.durationSeconds() / 60.0;
        int wordsPerMinute = (int) Math.round(totalWords / durationMinutes);

        // Find filler words
        List<FillerWordOccurrence> fillerWords = findFillerWords(segments);
        int fillerWordCount = fillerWords.stream().mapToInt(FillerWordOccurrence::count).sum();

        // Find pauses (gaps between segments)
        List<PauseOccurrence> pauses = findPauses(segments);
        int pauseCount = pauses.size();

        // Calculate clarity score (simplified)
        double clarityScore = calculateClarityScore(wordsPerMinute, fillerWordCount, totalWords);

        // Calculate average sentence length
        double avgSentenceLength = calculateAverageSentenceLength(text);

        Metrics metrics = new Metrics(
                wordsPerMinute,
                totalWords,
                durationMinutes,
                fillerWordCount,
                fillerWords,
                pauseCount,
                pauses,
                avgSentenceLength,
                clarityScore
        );

        // Generate feedback notes
        List<FeedbackNote> feedbackNotes = generateFeedbackNotes(metrics, segments, sessionType);

        // Generate summary
        String overallSummary = generateSummary(metrics, sessionType);
        List<String> strengths = identifyStrengths(metrics);
        List<String> areasForImprovement = identifyImprovements(metrics);

        AnalysisResult result = new AnalysisResult(
                metrics,
                feedbackNotes,
                overallSummary,
                strengths,
                areasForImprovement
        );

        log.info("Mock analysis completed: {} WPM, {} filler words, {} clarity score",
                wordsPerMinute, fillerWordCount, clarityScore);

        return ResultT.success(result);
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.split("\\s+").length;
    }

    private List<FillerWordOccurrence> findFillerWords(List<TranscriptionService.Segment> segments) {
        List<FillerWordOccurrence> occurrences = new ArrayList<>();

        for (TranscriptionService.Segment segment : segments) {
            Matcher matcher = FILLER_PATTERN.matcher(segment.text());
            while (matcher.find()) {
                String word = matcher.group().toLowerCase();
                occurrences.add(new FillerWordOccurrence(word, segment.startSeconds(), 1));
            }
        }

        return occurrences;
    }

    private List<PauseOccurrence> findPauses(List<TranscriptionService.Segment> segments) {
        List<PauseOccurrence> pauses = new ArrayList<>();

        for (int i = 1; i < segments.size(); i++) {
            double gapStart = segments.get(i - 1).endSeconds();
            double gapEnd = segments.get(i).startSeconds();
            double duration = gapEnd - gapStart;

            if (duration >= 1.5) { // Only count pauses >= 1.5 seconds
                String type = duration > 3.0 ? "awkward" : (duration > 2.0 ? "natural" : "dramatic");
                pauses.add(new PauseOccurrence(gapStart, gapEnd, duration, type));
            }
        }

        return pauses;
    }

    private double calculateClarityScore(int wpm, int fillerCount, int totalWords) {
        // Ideal WPM is 120-150
        double wpmScore = 1.0;
        if (wpm < 100) wpmScore = 0.7;
        else if (wpm < 120) wpmScore = 0.85;
        else if (wpm > 180) wpmScore = 0.7;
        else if (wpm > 160) wpmScore = 0.85;

        // Filler word penalty
        double fillerRatio = totalWords > 0 ? (double) fillerCount / totalWords : 0;
        double fillerScore = Math.max(0, 1.0 - (fillerRatio * 10)); // Heavy penalty for fillers

        return Math.round((wpmScore * 0.5 + fillerScore * 0.5) * 100) / 100.0;
    }

    private double calculateAverageSentenceLength(String text) {
        if (text == null || text.isBlank()) return 0;
        String[] sentences = text.split("[.!?]+");
        int totalWords = 0;
        for (String sentence : sentences) {
            totalWords += countWords(sentence.trim());
        }
        return sentences.length > 0 ? (double) totalWords / sentences.length : 0;
    }

    private List<FeedbackNote> generateFeedbackNotes(
            Metrics metrics,
            List<TranscriptionService.Segment> segments,
            String sessionType
    ) {
        List<FeedbackNote> notes = new ArrayList<>();

        // Filler word feedback
        for (FillerWordOccurrence filler : metrics.fillerWords()) {
            notes.add(new FeedbackNote(
                    "FILLER_WORDS",
                    "WARNING",
                    "Filler word detected: \"" + filler.word() + "\"",
                    filler.timestampSeconds(),
                    null
            ));
        }

        // Pause feedback
        for (PauseOccurrence pause : metrics.pauses()) {
            if ("awkward".equals(pause.type())) {
                notes.add(new FeedbackNote(
                        "PAUSES",
                        "WARNING",
                        String.format("Long pause detected (%.1f seconds). Consider filling with content or transitioning more smoothly.", pause.durationSeconds()),
                        pause.startSeconds(),
                        pause.endSeconds()
                ));
            }
        }

        // Pacing feedback
        if (metrics.wordsPerMinute() < 100) {
            notes.add(new FeedbackNote(
                    "PACING",
                    "INFO",
                    "Your speaking pace is slower than average. Consider picking up the pace slightly to maintain engagement.",
                    null, null
            ));
        } else if (metrics.wordsPerMinute() > 170) {
            notes.add(new FeedbackNote(
                    "PACING",
                    "WARNING",
                    "Your speaking pace is faster than recommended. Slow down to improve clarity and comprehension.",
                    null, null
            ));
        }

        // Clarity feedback
        if (metrics.clarityScore() < 0.7) {
            notes.add(new FeedbackNote(
                    "CLARITY",
                    "CRITICAL",
                    "Clarity score is below average. Focus on reducing filler words and maintaining a steady pace.",
                    null, null
            ));
        }

        // General suggestions
        notes.add(new FeedbackNote(
                "SUGGESTION",
                "INFO",
                "Consider using more transitional phrases between topics to improve flow.",
                null, null
        ));

        if (metrics.fillerWordCount() > 5) {
            notes.add(new FeedbackNote(
                    "SUGGESTION",
                    "INFO",
                    "Practice pausing briefly instead of using filler words. Silent pauses can be powerful.",
                    null, null
            ));
        }

        return notes;
    }

    private String generateSummary(Metrics metrics, String sessionType) {
        StringBuilder summary = new StringBuilder();
        summary.append("This ").append(sessionType).append(" was ");

        if (metrics.clarityScore() >= 0.85) {
            summary.append("excellent overall. ");
        } else if (metrics.clarityScore() >= 0.7) {
            summary.append("good with some areas for improvement. ");
        } else {
            summary.append("in need of practice to improve clarity and delivery. ");
        }

        summary.append(String.format("You spoke at %d words per minute ", metrics.wordsPerMinute()));

        if (metrics.wordsPerMinute() >= 120 && metrics.wordsPerMinute() <= 150) {
            summary.append("which is ideal. ");
        } else if (metrics.wordsPerMinute() < 120) {
            summary.append("which is slightly slow. ");
        } else {
            summary.append("which is faster than recommended. ");
        }

        if (metrics.fillerWordCount() > 0) {
            summary.append(String.format("You used %d filler words that could be reduced with practice.",
                    metrics.fillerWordCount()));
        }

        return summary.toString();
    }

    private List<String> identifyStrengths(Metrics metrics) {
        List<String> strengths = new ArrayList<>();

        if (metrics.wordsPerMinute() >= 120 && metrics.wordsPerMinute() <= 150) {
            strengths.add("Excellent speaking pace");
        }
        if (metrics.fillerWordCount() <= 3) {
            strengths.add("Minimal use of filler words");
        }
        if (metrics.clarityScore() >= 0.85) {
            strengths.add("High clarity score");
        }
        if (metrics.pauses().stream().noneMatch(p -> "awkward".equals(p.type()))) {
            strengths.add("Good use of natural pauses");
        }

        if (strengths.isEmpty()) {
            strengths.add("Completed the presentation");
        }

        return strengths;
    }

    private List<String> identifyImprovements(Metrics metrics) {
        List<String> improvements = new ArrayList<>();

        if (metrics.wordsPerMinute() < 100) {
            improvements.add("Increase speaking pace for better engagement");
        } else if (metrics.wordsPerMinute() > 170) {
            improvements.add("Slow down to improve comprehension");
        }

        if (metrics.fillerWordCount() > 5) {
            improvements.add("Reduce filler words (um, uh, like)");
        }

        if (metrics.clarityScore() < 0.7) {
            improvements.add("Focus on clearer articulation");
        }

        long awkwardPauses = metrics.pauses().stream()
                .filter(p -> "awkward".equals(p.type()))
                .count();
        if (awkwardPauses > 2) {
            improvements.add("Work on smoother transitions to avoid long pauses");
        }

        return improvements;
    }
}
