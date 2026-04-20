package com.pigs.voxly.application.evaluation;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pigs.voxly.domain.evaluation.Transcription;
import com.pigs.voxly.infrastructure.evaluation.AudioExtractionService;
import com.pigs.voxly.infrastructure.evaluation.TranscriptionRepository;
import com.pigs.voxly.infrastructure.evaluation.WhisperService;

@Service
@Transactional
public class TranscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(TranscriptionService.class);

    private final TranscriptionRepository transcriptionRepository;
    private final AudioExtractionService audioExtractionService;
    private final WhisperService whisperService;

    public TranscriptionService(
            TranscriptionRepository transcriptionRepository,
            AudioExtractionService audioExtractionService,
            WhisperService whisperService) {
        this.transcriptionRepository = transcriptionRepository;
        this.audioExtractionService = audioExtractionService;
        this.whisperService = whisperService;
    }

    /**
     * Request transcription for a session video
     */
    public Transcription requestTranscription(UUID sessionId, UUID userId, File videoFile) {
        // Check if transcription already exists
        Optional<Transcription> existing = transcriptionRepository.findBySessionId(sessionId);
        if (existing.isPresent() && !"FAILED".equals(existing.get().getStatus())) {
            logger.info("Transcription already exists for session: {}", sessionId);
            return existing.get();
        }

        // Create new transcription record
        Transcription transcription = Transcription.builder()
                .sessionId(sessionId)
                .userId(userId)
                .status("PENDING")
                .language("en")
                .build();

        transcriptionRepository.save(transcription);

        logger.info("Transcription requested for session: {}", sessionId);

        // Start async transcription process
        processTranscriptionAsync(transcription, videoFile);

        return transcription;
    }

    /**
     * Async transcription processing
     */
    @Async
    public void processTranscriptionAsync(Transcription transcription, File videoFile) {
        File audioFile = null;
        try {
            // Update status
            transcription.setStatus("PROCESSING");
            transcriptionRepository.save(transcription);

            // Extract audio
            logger.info("Extracting audio from video...");
            audioFile = audioExtractionService.extractAudio(videoFile);

            // Get duration
            int duration = audioExtractionService.getAudioDuration(audioFile);
            transcription.setDurationSeconds(duration);

            // Transcribe with Whisper
            logger.info("Sending to Whisper API...");
            WhisperService.WhisperResponse whisperResponse = whisperService.transcribeAudio(audioFile);

            // Store result
            transcription.setOriginalText(whisperResponse.getText());
            transcription.setLanguage(whisperResponse.getLanguage());
            transcription.setDurationSeconds(duration > 0 ? duration : 60); // Default to 60 if detection fails
            transcription.setWordCount(whisperResponse.getWordCount());
            transcription.setStatus("COMPLETED");
            transcription.setCompletedAt(LocalDateTime.now());

            transcriptionRepository.save(transcription);

            logger.info("Transcription completed for session: {}", transcription.getSessionId());

        } catch (Exception e) {
            logger.error("Transcription failed: {}", e.getMessage(), e);
            transcription.setStatus("FAILED");
            transcription.setErrorMessage(e.getMessage());
            transcriptionRepository.save(transcription);
        } finally {
            // Cleanup temp file
            if (audioFile != null) {
                audioExtractionService.cleanupFile(audioFile);
            }
            audioExtractionService.cleanupFile(videoFile);
        }
    }

    /**
     * Get transcription for session
     */
    public Optional<Transcription> getTranscription(UUID sessionId) {
        return transcriptionRepository.findBySessionId(sessionId);
    }

    /**
     * Get transcription with authorization check
     */
    public Optional<Transcription> getTranscription(UUID sessionId, UUID userId) {
        return transcriptionRepository.findByUserIdAndSessionId(userId, sessionId);
    }
}
