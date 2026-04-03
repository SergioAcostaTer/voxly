package com.pigs.voxly.infrastructure.shared.dev;

import com.pigs.voxly.application.evaluation.dto.EvaluationResponse;
import com.pigs.voxly.application.identity.ports.CurrentUserProvider;
import com.pigs.voxly.application.sessions.dto.SessionListResponse;
import com.pigs.voxly.application.sessions.dto.SessionResponse;
import com.pigs.voxly.application.shared.ports.SpeechAnalysisService;
import com.pigs.voxly.api.feedback.FeedbackController;
import com.pigs.voxly.api.progress.ProgressController;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DevMockDataService {

    private static final String DEV_EMAIL = "edumarreroglezz@gmail.com";

    private final CurrentUserProvider currentUserProvider;

    public DevMockDataService(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    public boolean isDevUser() {
        return currentUserProvider.getEmail()
                .map(email -> email.equalsIgnoreCase(DEV_EMAIL))
                .orElse(false);
    }

    public Optional<UUID> getDevUserId() {
        return currentUserProvider.getUserId();
    }

    // Mock Sessions
    private static final UUID SESSION_1_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESSION_2_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SESSION_3_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID EVAL_1_ID = UUID.fromString("aaaaaaa1-1111-1111-1111-111111111111");
    private static final UUID EVAL_2_ID = UUID.fromString("aaaaaaa2-2222-2222-2222-222222222222");
    private static final UUID EVAL_3_ID = UUID.fromString("aaaaaaa3-3333-3333-3333-333333333333");

    public SessionListResponse getMockSessions() {
        var userId = getDevUserId().orElse(UUID.randomUUID());
        var now = Instant.now();

        var sessions = List.of(
                new SessionResponse(
                        SESSION_1_ID,
                        userId,
                        "Q4 Sales Pitch Practice",
                        "Practicing my quarterly sales pitch for the board meeting",
                        "PITCH",
                        "COMPLETED",
                        new SessionResponse.MediaFileResponse(
                                "sessions/1/video.mp4",
                                "sales-pitch.mp4",
                                "video/mp4",
                                52428800L,
                                185.5,
                                "/api/v1/files/sessions/1/video.mp4"
                        ),
                        EVAL_1_ID,
                        now.minus(2, ChronoUnit.DAYS),
                        now.minus(2, ChronoUnit.DAYS)
                ),
                new SessionResponse(
                        SESSION_2_ID,
                        userId,
                        "Team Standup Presentation",
                        "Weekly team update presentation",
                        "PRESENTATION",
                        "COMPLETED",
                        new SessionResponse.MediaFileResponse(
                                "sessions/2/video.mp4",
                                "standup.mp4",
                                "video/mp4",
                                31457280L,
                                120.0,
                                "/api/v1/files/sessions/2/video.mp4"
                        ),
                        EVAL_2_ID,
                        now.minus(5, ChronoUnit.DAYS),
                        now.minus(5, ChronoUnit.DAYS)
                ),
                new SessionResponse(
                        SESSION_3_ID,
                        userId,
                        "Interview Practice - Software Engineer",
                        "Practicing answers for upcoming technical interview",
                        "INTERVIEW",
                        "COMPLETED",
                        new SessionResponse.MediaFileResponse(
                                "sessions/3/video.mp4",
                                "interview-prep.mp4",
                                "video/mp4",
                                78643200L,
                                300.0,
                                "/api/v1/files/sessions/3/video.mp4"
                        ),
                        EVAL_3_ID,
                        now.minus(7, ChronoUnit.DAYS),
                        now.minus(7, ChronoUnit.DAYS)
                )
        );

        return new SessionListResponse(sessions, 1, 10, 3, 1);
    }

    public SessionResponse getMockSession(UUID sessionId) {
        return getMockSessions().sessions().stream()
                .filter(s -> s.id().equals(sessionId))
                .findFirst()
                .orElse(getMockSessions().sessions().get(0));
    }

    public EvaluationResponse getMockEvaluation(UUID sessionId) {
        var now = Instant.now();
        UUID evalId;
        String sessionType;
        String transcription;
        int wpm;
        int totalWords;
        int fillerCount;
        int pauseCount;
        double clarity;
        String summary;

        if (sessionId.equals(SESSION_1_ID)) {
            evalId = EVAL_1_ID;
            sessionType = "PITCH";
            transcription = "Good morning everyone. Today I'm excited to present our Q4 results and outlook for the coming year. " +
                    "Our team has achieved remarkable growth, with revenue up 35% compared to last quarter. " +
                    "The key drivers were our new product launch and expanded market reach. " +
                    "Looking ahead, we're projecting continued momentum with three major initiatives planned. " +
                    "First, we'll be expanding into the European market. Second, launching our mobile platform. " +
                    "And third, deepening partnerships with enterprise clients. " +
                    "I'm confident that with your support, we'll exceed our targets. Thank you for your time. " +
                    "I'm happy to take any questions you might have.";
            wpm = 142;
            totalWords = 438;
            fillerCount = 3;
            pauseCount = 8;
            clarity = 8.7;
            summary = "Excellent pitch with clear structure and confident delivery. Minor improvements suggested for pacing in the middle section.";
        } else if (sessionId.equals(SESSION_2_ID)) {
            evalId = EVAL_2_ID;
            sessionType = "PRESENTATION";
            transcription = "Hi team, quick update from this week. We shipped the new authentication feature on Tuesday. " +
                    "QA found two minor bugs which were fixed by Thursday. Customer feedback has been positive so far. " +
                    "Next week we're focusing on the dashboard redesign. Any blockers? No? Great, let's sync again Monday.";
            wpm = 128;
            totalWords = 256;
            fillerCount = 1;
            pauseCount = 4;
            clarity = 9.2;
            summary = "Concise and well-structured standup. Great use of time and clear communication of priorities.";
        } else {
            evalId = EVAL_3_ID;
            sessionType = "INTERVIEW";
            transcription = "Thank you for asking about my experience with distributed systems. In my previous role at TechCorp, " +
                    "I led the migration of our monolithic application to a microservices architecture. " +
                    "We used Kubernetes for orchestration and implemented event-driven communication with Kafka. " +
                    "The biggest challenge was ensuring data consistency across services, which we solved using the saga pattern. " +
                    "The result was a 60% improvement in deployment frequency and 40% reduction in incident response time. " +
                    "I'm passionate about building scalable systems and would love to bring this experience to your team.";
            wpm = 135;
            totalWords = 675;
            fillerCount = 5;
            pauseCount = 12;
            clarity = 8.4;
            summary = "Strong technical response with good examples. Consider reducing filler words and adding more pauses for emphasis on key achievements.";
        }

        var feedbackJson = """
                [
                    {"timestamp": 15.0, "category": "pacing", "message": "Good opening pace, clear and measured", "severity": "info"},
                    {"timestamp": 45.0, "category": "clarity", "message": "Excellent articulation of key points", "severity": "info"},
                    {"timestamp": 78.0, "category": "filler", "message": "Minor filler word detected: 'um'", "severity": "warning"},
                    {"timestamp": 95.0, "category": "engagement", "message": "Strong conclusion with call to action", "severity": "suggestion"},
                    {"timestamp": 120.0, "category": "pacing", "message": "Consider a brief pause before key statistics", "severity": "suggestion"}
                ]
                """;

        return new EvaluationResponse(
                evalId,
                sessionId,
                "COMPLETED",
                sessionType,
                new EvaluationResponse.TranscriptionData(transcription, 185.5, "en"),
                new EvaluationResponse.MetricsData(wpm, totalWords, fillerCount, pauseCount, clarity),
                new EvaluationResponse.FeedbackData(summary, feedbackJson),
                null,
                now.minus(2, ChronoUnit.DAYS),
                now.minus(2, ChronoUnit.DAYS)
        );
    }

    public FeedbackController.FeedbackResponse getMockFeedback(UUID sessionId) {
        var eval = getMockEvaluation(sessionId);

        List<SpeechAnalysisService.FeedbackNote> notes = List.of(
                new SpeechAnalysisService.FeedbackNote("pacing", "info", "Good opening pace, clear and measured", 15.0, 20.0),
                new SpeechAnalysisService.FeedbackNote("clarity", "info", "Excellent articulation of key points", 45.0, 55.0),
                new SpeechAnalysisService.FeedbackNote("filler", "warning", "Minor filler word detected: 'um'", 78.0, 79.0),
                new SpeechAnalysisService.FeedbackNote("engagement", "suggestion", "Strong conclusion with call to action", 95.0, 100.0),
                new SpeechAnalysisService.FeedbackNote("pacing", "suggestion", "Consider a brief pause before key statistics", 120.0, 125.0)
        );

        return new FeedbackController.FeedbackResponse(
                sessionId,
                eval.id(),
                notes,
                eval.feedback() != null ? eval.feedback().overallSummary() : "Great presentation overall!",
                List.of("Clear structure", "Confident delivery", "Good pacing"),
                List.of("Reduce filler words", "Add more pauses for emphasis")
        );
    }

    public ProgressController.ProgressSummary getMockProgressSummary() {
        return new ProgressController.ProgressSummary(
                3L,           // totalSessions
                3L,           // completedSessions
                8.77,         // averageClarityScore
                135,          // averageWordsPerMinute
                3,            // averageFillerWords
                3             // recentSessionCount
        );
    }

    public List<ProgressController.SessionTrend> getMockProgressTrends() {
        var now = Instant.now();
        return List.of(
                new ProgressController.SessionTrend(
                        SESSION_1_ID,
                        "Q4 Sales Pitch Practice",
                        now.minus(2, ChronoUnit.DAYS),
                        8.7,
                        142,
                        3
                ),
                new ProgressController.SessionTrend(
                        SESSION_2_ID,
                        "Team Standup Presentation",
                        now.minus(5, ChronoUnit.DAYS),
                        9.2,
                        128,
                        1
                ),
                new ProgressController.SessionTrend(
                        SESSION_3_ID,
                        "Interview Practice - Software Engineer",
                        now.minus(7, ChronoUnit.DAYS),
                        8.4,
                        135,
                        5
                )
        );
    }
}
