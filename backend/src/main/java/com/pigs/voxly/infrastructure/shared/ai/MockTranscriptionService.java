package com.pigs.voxly.infrastructure.shared.ai;

import com.pigs.voxly.application.shared.ports.TranscriptionService;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * Mock implementation of TranscriptionService for development and testing.
 * Returns realistic sample data without calling external APIs.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockTranscriptionService implements TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(MockTranscriptionService.class);

    @Override
    public ResultT<TranscriptionResult> transcribe(Path filePath, String language) {
        log.info("Mock transcription for file: {}", filePath);

        // Simulate processing delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<Segment> segments = List.of(
                new Segment(
                        "Hello everyone, thank you for joining this presentation today.",
                        0.0, 4.5, 0.95
                ),
                new Segment(
                        "Um, so today I want to talk about, uh, our new product features.",
                        4.5, 9.2, 0.92
                ),
                new Segment(
                        "First, let me show you the main dashboard.",
                        9.2, 12.8, 0.97
                ),
                new Segment(
                        "As you can see here, we have, like, several new components.",
                        12.8, 17.5, 0.91
                ),
                new Segment(
                        "The analytics section has been completely redesigned.",
                        17.5, 21.3, 0.96
                ),
                new Segment(
                        "Um, basically, we focused on making it more intuitive.",
                        21.3, 25.8, 0.89
                ),
                new Segment(
                        "Let me walk you through each feature one by one.",
                        25.8, 29.5, 0.94
                ),
                new Segment(
                        "So, uh, the first thing you'll notice is the improved navigation.",
                        29.5, 35.2, 0.90
                ),
                new Segment(
                        "We've added shortcuts that make it faster to access common tasks.",
                        35.2, 40.1, 0.93
                ),
                new Segment(
                        "Any questions so far?",
                        40.1, 42.0, 0.98
                ),
                new Segment(
                        "Great, let me continue with the reporting features.",
                        44.5, 48.2, 0.95
                ),
                new Segment(
                        "You can now, um, generate custom reports with just a few clicks.",
                        48.2, 53.8, 0.88
                ),
                new Segment(
                        "The data visualization options have been expanded significantly.",
                        53.8, 58.5, 0.94
                ),
                new Segment(
                        "In conclusion, I believe these changes will, uh, improve your workflow.",
                        58.5, 64.2, 0.91
                ),
                new Segment(
                        "Thank you for your attention. Are there any questions?",
                        64.2, 68.0, 0.97
                )
        );

        String fullText = segments.stream()
                .map(Segment::text)
                .reduce((a, b) -> a + " " + b)
                .orElse("");

        TranscriptionResult result = new TranscriptionResult(
                fullText,
                segments,
                language != null ? language : "en",
                68.0
        );

        log.info("Mock transcription completed: {} segments, {} seconds duration",
                segments.size(), result.durationSeconds());

        return ResultT.success(result);
    }
}
