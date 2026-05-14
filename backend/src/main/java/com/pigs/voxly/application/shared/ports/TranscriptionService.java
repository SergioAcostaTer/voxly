package com.pigs.voxly.application.shared.ports;

import com.pigs.voxly.sharedKernel.domain.results.ResultT;

import java.nio.file.Path;
import java.util.List;

/**
 * Port interface for audio/video transcription services.
 * Implementations can use OpenAI Whisper, Azure Speech, Google Speech-to-Text, etc.
 */
public interface TranscriptionService {

    /**
     * Transcribes an audio/video file and returns timestamped segments.
     *
     * @param filePath the path to the media file
     * @param language the language code (e.g., "en", "es") or null for auto-detect
     * @return ResultT containing transcription result on success
     */
    ResultT<TranscriptionResult> transcribe(Path filePath, String language);

    /**
     * Represents a complete transcription result.
     */
    record TranscriptionResult(
            String fullText,
            List<Segment> segments,
            List<Word> words,
            String detectedLanguage,
            double durationSeconds,
            String rawJson
    ) {}

    /**
     * Represents a single transcription segment with timing information.
     */
    record Segment(
            String text,
            double startSeconds,
            double endSeconds,
            double confidence
    ) {}

    /**
     * Represents a word-level timestamp.
     */
    record Word(
            String word,
            double startSeconds,
            double endSeconds,
            double confidence
    ) {}
}
