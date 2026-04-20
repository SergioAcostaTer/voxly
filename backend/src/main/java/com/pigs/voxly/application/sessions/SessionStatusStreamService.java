package com.pigs.voxly.application.sessions;

import com.pigs.voxly.domain.evaluation.Evaluation;
import com.pigs.voxly.domain.sessions.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionStatusStreamService {

    private static final Logger log = LoggerFactory.getLogger(SessionStatusStreamService.class);
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId, UUID sessionId) {
        String key = key(userId, sessionId);
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        emitter.onCompletion(() -> emitters.remove(key));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(key);
        });
        emitter.onError(error -> emitters.remove(key));

        emitters.put(key, emitter);
        send(emitter, "connected", new SessionStatusEvent(sessionId, null, null, null, Instant.now(), "connected"));
        return emitter;
    }

    public void publishSessionUpdate(Session session) {
        emitByKey(session.getUserId().getValue(), session.getId().getValue(), new SessionStatusEvent(
                session.getId().getValue(),
                session.getStatus().getName(),
                session.getEvaluationId(),
                null,
                Instant.now(),
                "session"
        ));
    }

    public void publishEvaluationUpdate(Evaluation evaluation) {
        emitByKey(evaluation.getUserId().getValue(), evaluation.getSessionId().getValue(), new SessionStatusEvent(
                evaluation.getSessionId().getValue(),
                null,
                evaluation.getId().getValue(),
                evaluation.getStatus().getName(),
                Instant.now(),
                "evaluation"
        ));
    }

    private void emitByKey(UUID userId, UUID sessionId, SessionStatusEvent event) {
        SseEmitter emitter = emitters.get(key(userId, sessionId));
        if (emitter == null) {
            return;
        }
        send(emitter, "status", event);
    }

    private void send(SseEmitter emitter, String eventName, SessionStatusEvent event) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(event));
        } catch (IOException e) {
            log.debug("Failed to send session status SSE event", e);
            emitter.completeWithError(e);
        }
    }

    private String key(UUID userId, UUID sessionId) {
        return userId + ":" + sessionId;
    }

    public record SessionStatusEvent(
            UUID sessionId,
            String sessionStatus,
            UUID evaluationId,
            String evaluationStatus,
            Instant emittedAt,
            String source
    ) {
    }
}
