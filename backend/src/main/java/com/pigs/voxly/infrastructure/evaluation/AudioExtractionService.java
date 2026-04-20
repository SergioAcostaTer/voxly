package com.pigs.voxly.infrastructure.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class AudioExtractionService {
    private static final Logger logger = LoggerFactory.getLogger(AudioExtractionService.class);

    @Value("${storage.temp-dir:/tmp/voxly-temp}")
    private String tempDir;

    @Value("${storage.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${storage.ffmpeg.audio-format:wav}")
    private String audioFormat;

    @Value("${storage.ffmpeg.audio-samplerate:16000}")
    private String audioSampleRate;

    @Value("${storage.ffmpeg.timeout-seconds:120}")
    private long ffmpegTimeoutSeconds;

    @Value("${storage.ffmpeg.probe-timeout-seconds:15}")
    private long ffprobeTimeoutSeconds;

    /**
     * Extract audio from video file using FFmpeg
     */
    public File extractAudio(File videoFile) throws IOException {
        // Ensure temp directory exists
        Files.createDirectories(Paths.get(tempDir));

        // Create output file
        String audioFilePath = Paths.get(tempDir,
                videoFile.getName().replaceAll("\\.[^.]*$", "") + ".wav").toString();

        File audioFile = new File(audioFilePath);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    ffmpegPath,
                    "-i", videoFile.getAbsolutePath(),
                    "-vn", // No video
                    "-acodec", "pcm_s16le", // Audio codec
                    "-ar", audioSampleRate, // Sample rate for Whisper
                    "-ac", "1", // Mono audio
                    "-y", // Overwrite output file
                    audioFilePath);

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Wait for completion
            boolean finished = process.waitFor(ffmpegTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("FFmpeg extraction timed out after " + ffmpegTimeoutSeconds + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("FFmpeg extraction failed with code: {}", exitCode);
                throw new IOException("FFmpeg extraction failed with code: " + exitCode);
            }

            if (!audioFile.exists()) {
                throw new IOException("Audio file was not created");
            }

            logger.info("Audio extracted successfully: {}", audioFilePath);
            return audioFile;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Audio extraction interrupted", e);
        } catch (Exception e) {
            logger.error("Error extracting audio: {}", e.getMessage(), e);
            throw new IOException("Failed to extract audio: " + e.getMessage(), e);
        }
    }

    /**
     * Get duration of audio file in seconds
     */
    public int getAudioDuration(File audioFile) throws IOException {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    ffmpegPath.replace("ffmpeg", "ffprobe"),
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    audioFile.getAbsolutePath());

            Process process = processBuilder.start();
            byte[] output = process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(ffprobeTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.warn("FFprobe timed out after {} seconds", ffprobeTimeoutSeconds);
                return 0;
            }

            int exitCode = process.exitValue();

            if (exitCode == 0 && output.length > 0) {
                String durationStr = new String(output).trim();
                try {
                    return (int) Double.parseDouble(durationStr);
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse duration: {}", durationStr);
                    return 0;
                }
            }

            return 0;

        } catch (Exception e) {
            logger.warn("Could not determine audio duration: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Clean up temporary files
     */
    public void cleanupFile(File file) {
        if (file != null && file.exists()) {
            if (file.delete()) {
                logger.info("Cleaned up temporary file: {}", file.getAbsolutePath());
            } else {
                logger.warn("Failed to delete temporary file: {}", file.getAbsolutePath());
            }
        }
    }
}
