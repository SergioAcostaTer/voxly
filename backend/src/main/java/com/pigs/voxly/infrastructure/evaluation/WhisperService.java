package com.pigs.voxly.infrastructure.evaluation;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WhisperService {
    private static final Logger logger = LoggerFactory.getLogger(WhisperService.class);
    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    @Value("${openai.apiKey}")
    private String apiKey;

    @Value("${openai.whisper.model:whisper-1}")
    private String model;

    @Value("${openai.whisper.language:en}")
    private String language;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WhisperService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Transcribe audio file using Whisper API
     */
    public WhisperResponse transcribeAudio(File audioFile) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new WhisperException("OpenAI API key not configured");
        }

        if (!audioFile.exists()) {
            throw new WhisperException("Audio file does not exist: " + audioFile.getAbsolutePath());
        }

        logger.info("Sending audio file to Whisper API: {}", audioFile.getName());

        try {
            // Prepare multipart request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(audioFile));
            body.add("model", model);
            body.add("language", language);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(apiKey);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Call API with timeout handling
            String response = restTemplate.postForObject(WHISPER_API_URL, requestEntity, String.class);

            if (response == null) {
                throw new WhisperException("Empty response from Whisper API");
            }

            JsonNode jsonNode = objectMapper.readTree(response);

            // Check for error in response
            if (jsonNode.has("error")) {
                String errorMsg = jsonNode.get("error").get("message").asText();
                throw new WhisperException("Whisper API error: " + errorMsg);
            }

            String text = jsonNode.get("text").asText();
            int wordCount = text.split("\\s+").length;

            logger.info("Transcription completed. Words: {}", wordCount);

            return new WhisperResponse(text, language, wordCount);

        } catch (WhisperException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Whisper API call failed: {}", e.getMessage(), e);
            throw new WhisperException("Transcription failed: " + e.getMessage(), e);
        }
    }

    // Response DTOs
    public static class WhisperResponse {
        public String text;
        public String language;
        public int wordCount;

        public WhisperResponse(String text, String language, int wordCount) {
            this.text = text;
            this.language = language;
            this.wordCount = wordCount;
        }

        public String getText() {
            return text;
        }

        public String getLanguage() {
            return language;
        }

        public int getWordCount() {
            return wordCount;
        }
    }

    public static class WhisperException extends RuntimeException {
        public WhisperException(String message) {
            super(message);
        }

        public WhisperException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
