# VoxLy - Project Development Plan

## 1. Project Understanding

### Brief Summary
VoxLy is an AI-powered speech coaching platform that helps users improve public speaking, interviews, and high-stakes conversations. The system allows users to record or upload presentations, request AI analysis, receive timestamped transcriptions, calculate objective presentation metrics, and track historical progress across sessions.

### Main Business Goal
Enable users to systematically improve their presentation and communication skills through AI-driven feedback, objective metrics, and progress tracking over time.

### Core Modules
1. **Users (Identity)** - Authentication, profiles, security
2. **Sessions** - Recording/uploading presentations, session management
3. **Evaluation** - AI analysis, transcription, metrics calculation
4. **Feedback** - Timestamped notes, categories, synchronized playback
5. **Progress** - Historical tracking, trends, comparisons

### Key Functional Flows
1. **Session Creation Flow**: User creates session → uploads video/slides OR records presentation → session saved with metadata
2. **Evaluation Flow**: User requests analysis → media validated → transcription generated → metrics calculated → report generated → user notified
3. **Feedback Consumption Flow**: User views report → navigates timestamped notes → jumps to video moments → understands improvement areas
4. **Progress Tracking Flow**: User views history → compares sessions → sees trends → sets goals → tracks improvement

---

## 2. Assumptions Based on Current Repository

### What Already Exists (Verified)

| Component | Status | Notes |
|-----------|--------|-------|
| **Backend Framework** | Complete | Spring Boot 4.0.3, Java 21, Gradle |
| **Architecture** | Complete | DDD + Hexagonal, well-structured |
| **Identity Module** | Complete | Registration, login, 2FA, email verification, password reset, JWT tokens, refresh token rotation |
| **Security** | Complete | Spring Security, OAuth2 Resource Server, CORS, account lockout |
| **Email Service** | Complete | Event-driven, async delivery |
| **Shared Kernel** | Complete | AggregateRoot, Entity, ValueObject, Result pattern, domain events, pagination |
| **Database** | Complete | PostgreSQL (Neon), JPA/Hibernate |
| **Frontend Auth** | Complete | React 19, AuthContext, protected routes, login/register flows |
| **API Documentation** | Complete | OpenAPI 3.0, Swagger UI |

### What Must Be Implemented

| Component | Status | Priority |
|-----------|--------|----------|
| **Sessions Domain Model** | Not started | High |
| **File Upload/Storage** | Not started | High |
| **Evaluation Domain Model** | Not started | High |
| **AI Integration** | Not started | High |
| **Transcription Service** | Not started | High |
| **Metrics Calculation** | Not started | Medium |
| **Feedback Domain Model** | Not started | Medium |
| **Progress Domain Model** | Not started | Medium |
| **Video Player Integration** | Not started | Medium |
| **Dashboard Features** | Basic only | Medium |

### Assumptions to Verify Before Implementation

1. **AI Provider**: Which AI service will be used? (OpenAI Whisper for transcription, GPT for analysis, or custom models?)
2. **Storage Provider**: Where will video/audio files be stored? (AWS S3, Azure Blob, local filesystem?)
3. **Video Processing**: Will videos be processed server-side or client-side? Any transcoding needed?
4. **Real-time Features**: Is real-time feedback during recording required for MVP?
5. **Slide Analysis**: Should slides be analyzed separately from video/audio?
6. **Metric Definitions**: What specific metrics should be calculated? (pace, filler words, eye contact, confidence score?)

### Risks If Parts Are Missing

| Risk | Impact | Mitigation |
|------|--------|------------|
| No AI provider configured | Cannot implement evaluation | Define AI integration strategy first |
| No storage configured | Cannot upload recordings | Implement storage abstraction early |
| Large file handling | Memory issues, timeouts | Chunked uploads, async processing |
| Video format compatibility | Playback issues | Define supported formats, consider transcoding |

---

## 3. Development Phases

### Phase 1 - Repository and Architecture Assessment

**Goal**: Validate existing implementation and establish baseline for new development

**Scope**: Investigation and documentation only, no code changes

**Features Included**:
- Verify all existing endpoints work correctly
- Document current database schema
- Map existing patterns to new modules
- Identify any technical debt to address

**Files/Layers Affected**:
- Review all files under `backend/src/main/java/com/pigs/voxly/`
- Review `frontend/src/` structure
- Check `application.yml` configuration

**Dependencies**: None (first phase)

**Technical Risks**:
- Existing code may have undiscovered issues
- Configuration may not be production-ready

**Acceptance Criteria**:
- [ ] All existing auth endpoints tested and working
- [ ] Database schema documented
- [ ] Architecture patterns documented
- [ ] Technical debt list created
- [ ] AI/Storage decisions documented

**Suggested Tests**:
- Manual API testing via Swagger UI
- Frontend auth flow testing
- Database connection verification

---

### Phase 2 - Core Foundations Stabilization

**Goal**: Ensure infrastructure is ready for new feature development

**Scope**: Configuration, shared utilities, missing abstractions

**Features Included**:
- Fix Gradle/Java configuration issues (currently broken)
- Add storage abstraction interface (port)
- Add async job/queue infrastructure for analysis processing
- Add file validation utilities
- Configure environment-specific settings

**Files/Layers Affected**:
```
backend/
├── build.gradle (fix main class detection)
├── src/main/java/com/pigs/voxly/
│   ├── application/shared/
│   │   └── ports/
│   │       ├── StorageService.java (new)
│   │       └── TranscriptionService.java (new)
│   ├── infrastructure/shared/
│   │   ├── storage/
│   │   │   └── LocalStorageService.java (new, MVP)
│   │   └── async/
│   │       └── AsyncConfig.java (new)
│   └── sharedKernel/
│       └── validation/
│           └── FileValidator.java (new)
└── src/main/resources/
    ├── application.yml (storage config)
    └── application-prod.yml (new)
```

**Dependencies**: Phase 1 completed

**Technical Risks**:
- Async processing may introduce complexity
- Storage abstraction needs careful design for future providers

**Acceptance Criteria**:
- [ ] `./gradlew bootRun` works without errors
- [ ] Storage port interface defined
- [ ] Local storage implementation working
- [ ] Async task execution configured
- [ ] File validation utilities ready
- [ ] Environment profiles configured

**Suggested Tests**:
- Unit tests for FileValidator
- Integration test for storage service
- Async execution verification test

---

### Phase 3 - Users Module Completion

**Goal**: Complete any missing user functionality required for Sessions

**Scope**: Minor enhancements to existing module

**Features Included**:
- User preferences/settings entity (notification preferences, default analysis options)
- Profile update endpoint
- Account deletion (soft delete)
- User statistics summary endpoint (for dashboard)

**Files/Layers Affected**:
```
backend/src/main/java/com/pigs/voxly/
├── domain/identity/
│   ├── User.java (add preferences)
│   └── valueobjects/
│       └── UserPreferences.java (new)
├── application/identity/
│   ├── UserService.java (add methods)
│   └── dto/
│       ├── UpdateProfileRequest.java (new)
│       └── UserStatisticsResponse.java (new)
├── api/identity/
│   └── UserController.java (add endpoints)
└── infrastructure/identity/persistence/
    └── entity/UserJpaEntity.java (add fields)
```

**Dependencies**: Phase 2 completed

**Technical Risks**:
- Schema migration needed for new fields
- Soft delete may affect existing queries

**Acceptance Criteria**:
- [ ] User can update profile (username, preferences)
- [ ] User can soft-delete account
- [ ] User statistics endpoint returns data
- [ ] Existing auth flows unaffected

**Suggested Tests**:
- Profile update integration tests
- Soft delete verification tests
- Statistics calculation tests

---

### Phase 4 - Sessions Module

**Goal**: Implement complete session lifecycle management

**Scope**: New domain module for practice sessions

**Features Included**:
- Create session with metadata (title, type, description)
- Upload presentation video (chunked upload for large files)
- Upload slides (PDF, PPTX)
- Session status tracking (draft, uploaded, analyzing, completed, failed)
- View session details
- List user sessions (paginated)
- Edit session metadata
- Delete session (cascade to storage)
- Session types: presentation, interview, pitch, freestyle

**Files/Layers Affected**:
```
backend/src/main/java/com/pigs/voxly/
├── domain/sessions/
│   ├── Session.java (aggregate root)
│   ├── SessionId.java
│   ├── SessionRepository.java
│   ├── SessionErrors.java
│   ├── enumerations/
│   │   ├── SessionType.java
│   │   └── SessionStatus.java
│   ├── valueobjects/
│   │   ├── SessionTitle.java
│   │   ├── MediaFile.java
│   │   └── SlideFile.java
│   └── events/
│       ├── SessionCreatedEvent.java
│       ├── SessionMediaUploadedEvent.java
│       ├── SessionAnalysisRequestedEvent.java
│       └── SessionDeletedEvent.java
├── application/sessions/
│   ├── SessionService.java
│   ├── dto/
│   │   ├── CreateSessionRequest.java
│   │   ├── UpdateSessionRequest.java
│   │   ├── SessionResponse.java
│   │   └── SessionListResponse.java
│   └── eventhandlers/
│       └── SessionEventHandlers.java
├── api/sessions/
│   ├── SessionController.java
│   └── dto/ (API-specific DTOs if needed)
└── infrastructure/sessions/
    ├── persistence/
    │   ├── entity/SessionJpaEntity.java
    │   ├── repository/JpaSessionRepository.java
    │   └── mapper/SessionMapper.java
    └── storage/
        └── SessionStorageService.java

frontend/src/
├── pages/
│   ├── SessionsPage.tsx (list)
│   ├── SessionDetailPage.tsx (view/edit)
│   └── NewSessionPage.tsx (create)
├── components/sessions/
│   ├── SessionCard.tsx
│   ├── SessionForm.tsx
│   ├── FileUploader.tsx
│   └── SessionStatusBadge.tsx
└── lib/
    └── sessions-api.ts
```

**Dependencies**: Phase 2 (storage), Phase 3 (user statistics)

**Technical Risks**:
- Large file uploads may timeout
- Storage cleanup on failed uploads
- Concurrent upload handling

**Acceptance Criteria**:
- [ ] User can create new session with title and type
- [ ] User can upload video file (up to 500MB)
- [ ] User can upload slides (PDF/PPTX)
- [ ] Session list shows all user sessions
- [ ] User can view session details
- [ ] User can edit session metadata
- [ ] User can delete session (files cleaned up)
- [ ] Session status reflects current state

**Suggested Tests**:
- Session CRUD integration tests
- File upload integration tests (small and large files)
- Storage cleanup tests
- Pagination tests

---

### Phase 5 - Evaluation Pipeline

**Goal**: Implement AI-powered analysis of presentation sessions

**Scope**: New domain module + async processing infrastructure

**Features Included**:
- Request analysis for uploaded session
- Validate media files before processing
- Generate timestamped transcription (using AI service)
- Calculate presentation metrics:
  - Speaking pace (words per minute)
  - Filler word count and timestamps
  - Pause analysis (awkward silences)
  - Volume consistency
  - Clarity score
- Generate structured evaluation report
- Track analysis status (queued, processing, completed, failed)
- Notify user on completion (via email initially)
- Retry failed analyses

**Files/Layers Affected**:
```
backend/src/main/java/com/pigs/voxly/
├── domain/evaluation/
│   ├── Evaluation.java (aggregate root)
│   ├── EvaluationId.java
│   ├── EvaluationRepository.java
│   ├── EvaluationErrors.java
│   ├── enumerations/
│   │   └── EvaluationStatus.java
│   ├── valueobjects/
│   │   ├── Transcription.java
│   │   ├── TranscriptionSegment.java
│   │   ├── PresentationMetrics.java
│   │   └── EvaluationReport.java
│   ├── entities/
│   │   └── MetricResult.java
│   └── events/
│       ├── EvaluationRequestedEvent.java
│       ├── TranscriptionCompletedEvent.java
│       ├── MetricsCalculatedEvent.java
│       ├── EvaluationCompletedEvent.java
│       └── EvaluationFailedEvent.java
├── application/evaluation/
│   ├── EvaluationService.java
│   ├── TranscriptionProcessor.java
│   ├── MetricsCalculator.java
│   ├── dto/
│   │   ├── RequestEvaluationRequest.java
│   │   ├── EvaluationResponse.java
│   │   ├── TranscriptionResponse.java
│   │   └── MetricsResponse.java
│   ├── ports/
│   │   ├── TranscriptionProvider.java
│   │   └── SpeechAnalysisProvider.java
│   └── eventhandlers/
│       └── EvaluationEventHandlers.java
├── api/evaluation/
│   └── EvaluationController.java
└── infrastructure/evaluation/
    ├── persistence/
    │   ├── entity/EvaluationJpaEntity.java
    │   ├── repository/JpaEvaluationRepository.java
    │   └── mapper/EvaluationMapper.java
    ├── ai/
    │   ├── OpenAITranscriptionProvider.java (Whisper)
    │   └── OpenAISpeechAnalysisProvider.java (GPT)
    └── async/
        └── EvaluationJobProcessor.java

frontend/src/
├── pages/
│   └── EvaluationPage.tsx
├── components/evaluation/
│   ├── TranscriptionView.tsx
│   ├── MetricsChart.tsx
│   ├── EvaluationReport.tsx
│   └── AnalysisProgress.tsx
└── lib/
    └── evaluation-api.ts
```

**Dependencies**: Phase 4 (sessions), AI provider configuration

**Technical Risks**:
- AI API rate limits and costs
- Long processing times for large files
- Transcription accuracy issues
- Metric calculation complexity
- Job queue reliability

**Acceptance Criteria**:
- [ ] User can request analysis for uploaded session
- [ ] System validates media before processing
- [ ] Transcription generated with timestamps
- [ ] At least 5 metrics calculated (pace, fillers, pauses, volume, clarity)
- [ ] Report generated and persisted
- [ ] User notified on completion
- [ ] Failed analyses can be retried
- [ ] Analysis status visible in UI

**Suggested Tests**:
- Transcription provider mock tests
- Metrics calculation unit tests
- Async job processing tests
- End-to-end evaluation flow tests

---

### Phase 6 - Feedback Module

**Goal**: Implement contextual feedback linked to video timestamps

**Scope**: New domain module + video player integration

**Features Included**:
- Generate feedback notes from evaluation results
- Link notes to specific timestamps
- Categorize notes (filler words, pacing, clarity, engagement, improvement suggestions)
- Note severity levels (info, warning, critical)
- Notes summary per category
- Filter notes by category/severity
- Navigate from note to video moment
- Synchronized playback (highlight current segment)

**Files/Layers Affected**:
```
backend/src/main/java/com/pigs/voxly/
├── domain/feedback/
│   ├── FeedbackCollection.java (aggregate root, per evaluation)
│   ├── FeedbackCollectionId.java
│   ├── FeedbackRepository.java
│   ├── FeedbackErrors.java
│   ├── enumerations/
│   │   ├── FeedbackCategory.java
│   │   └── FeedbackSeverity.java
│   ├── entities/
│   │   └── FeedbackNote.java
│   └── valueobjects/
│       └── Timestamp.java
├── application/feedback/
│   ├── FeedbackService.java
│   ├── FeedbackGenerator.java
│   └── dto/
│       ├── FeedbackCollectionResponse.java
│       ├── FeedbackNoteResponse.java
│       └── FeedbackSummaryResponse.java
├── api/feedback/
│   └── FeedbackController.java
└── infrastructure/feedback/
    └── persistence/
        ├── entity/FeedbackCollectionJpaEntity.java
        ├── entity/FeedbackNoteJpaEntity.java
        ├── repository/JpaFeedbackRepository.java
        └── mapper/FeedbackMapper.java

frontend/src/
├── pages/
│   └── FeedbackPage.tsx
├── components/feedback/
│   ├── VideoPlayer.tsx (with timestamp sync)
│   ├── FeedbackTimeline.tsx
│   ├── FeedbackNoteCard.tsx
│   ├── FeedbackCategoryFilter.tsx
│   └── FeedbackSummary.tsx
└── lib/
    └── feedback-api.ts
```

**Dependencies**: Phase 5 (evaluation results)

**Technical Risks**:
- Video player library selection
- Timestamp synchronization accuracy
- Large number of notes performance
- Mobile video playback issues

**Acceptance Criteria**:
- [ ] Feedback notes generated from evaluation
- [ ] Notes linked to timestamps
- [ ] Notes categorized and severity assigned
- [ ] Summary shows counts per category
- [ ] User can filter notes
- [ ] Clicking note jumps to video moment
- [ ] Current transcript segment highlighted during playback

**Suggested Tests**:
- Feedback generation tests
- Category assignment tests
- Frontend playback sync tests
- Filter functionality tests

---

### Phase 7 - Progress Module

**Goal**: Enable users to track improvement over time

**Scope**: New domain module + analytics dashboard

**Features Included**:
- Sessions history with search/filter
- Compare two sessions side-by-side
- Progress trends over time (charts)
- Set improvement goals
- Track progress by metric category
- Export progress report (PDF)
- Milestone achievements (gamification)

**Files/Layers Affected**:
```
backend/src/main/java/com/pigs/voxly/
├── domain/progress/
│   ├── ProgressTracker.java (aggregate root, per user)
│   ├── ProgressTrackerId.java
│   ├── ProgressRepository.java
│   ├── enumerations/
│   │   └── GoalType.java
│   ├── entities/
│   │   ├── Goal.java
│   │   └── Milestone.java
│   ├── valueobjects/
│   │   ├── ProgressSnapshot.java
│   │   └── TrendData.java
│   └── events/
│       ├── GoalSetEvent.java
│       ├── GoalAchievedEvent.java
│       └── MilestoneUnlockedEvent.java
├── application/progress/
│   ├── ProgressService.java
│   ├── TrendCalculator.java
│   ├── ComparisonService.java
│   └── dto/
│       ├── ProgressDashboardResponse.java
│       ├── SessionComparisonResponse.java
│       ├── TrendResponse.java
│       ├── GoalRequest.java
│       └── ProgressExportResponse.java
├── api/progress/
│   └── ProgressController.java
└── infrastructure/progress/
    ├── persistence/
    │   ├── entity/ProgressTrackerJpaEntity.java
    │   ├── repository/JpaProgressRepository.java
    │   └── mapper/ProgressMapper.java
    └── export/
        └── PdfProgressExporter.java

frontend/src/
├── pages/
│   ├── ProgressPage.tsx (dashboard)
│   └── ComparisonPage.tsx
├── components/progress/
│   ├── TrendChart.tsx
│   ├── SessionTimeline.tsx
│   ├── ComparisonView.tsx
│   ├── GoalCard.tsx
│   ├── MilestoneBadge.tsx
│   └── ExportButton.tsx
└── lib/
    └── progress-api.ts
```

**Dependencies**: Phase 5 (evaluation data), Phase 6 (feedback data)

**Technical Risks**:
- Trend calculation performance with many sessions
- Chart library selection
- PDF generation complexity
- Data aggregation accuracy

**Acceptance Criteria**:
- [ ] Sessions history searchable and filterable
- [ ] Two sessions can be compared
- [ ] Trend charts show improvement over time
- [ ] User can set and track goals
- [ ] Progress shown per metric category
- [ ] Progress report exportable as PDF
- [ ] Milestones awarded for achievements

**Suggested Tests**:
- Trend calculation tests
- Comparison logic tests
- Goal tracking tests
- PDF export tests

---

### Phase 8 - Quality, Observability and Hardening

**Goal**: Production readiness

**Scope**: Cross-cutting concerns

**Features Included**:
- Comprehensive test coverage (unit, integration, e2e)
- Structured logging (JSON format)
- Health checks and metrics endpoints
- Performance optimization (caching, query optimization)
- Security audit (OWASP checks)
- Error scenario handling
- Data consistency validation
- Permission checks hardening
- Rate limiting
- Input sanitization review

**Files/Layers Affected**:
```
backend/
├── src/main/java/com/pigs/voxly/
│   ├── infrastructure/shared/
│   │   ├── logging/LoggingConfig.java
│   │   ├── metrics/MetricsConfig.java
│   │   ├── cache/CacheConfig.java
│   │   └── ratelimit/RateLimitConfig.java
│   └── api/shared/
│       └── HealthController.java
├── src/test/java/com/pigs/voxly/
│   ├── domain/ (unit tests)
│   ├── application/ (integration tests)
│   └── api/ (e2e tests)
└── src/main/resources/
    └── logback-spring.xml

frontend/
├── src/
│   └── lib/
│       └── error-boundary.tsx
└── tests/ (e2e tests)
```

**Dependencies**: All previous phases

**Technical Risks**:
- Test coverage gaps
- Performance issues under load
- Security vulnerabilities

**Acceptance Criteria**:
- [ ] 80%+ test coverage on critical paths
- [ ] Structured JSON logs in production
- [ ] Health check endpoint returning status
- [ ] Key metrics exposed (request count, latency, error rate)
- [ ] No critical security issues
- [ ] Error scenarios handled gracefully
- [ ] Rate limiting prevents abuse

**Suggested Tests**:
- Load testing with JMeter/k6
- Security scanning with OWASP ZAP
- Penetration testing checklist

---

### Phase 9 - Future Improvements

**Goal**: Post-MVP enhancements

**Scope**: Nice-to-have features

**Features Included**:
- Personalized AI feedback based on user history
- Real-time coaching during recording
- Industry-specific scenarios (sales pitch, job interview, TED talk)
- Benchmarking against anonymized peer data
- Coach mode (for trainers to review students)
- Teacher dashboards (for education)
- Mobile app (React Native)
- Advanced analytics (engagement prediction, A/B testing)
- Multi-language support
- Accessibility improvements
- Social features (share progress, peer review)

**Dependencies**: Phase 8 complete, product validation

**Technical Risks**:
- Feature scope creep
- Technical complexity of real-time features
- Cost of advanced AI features

**Acceptance Criteria**:
- Defined per feature as needed

---

## 4. Recommended Implementation Order

```
Phase 1 ─── Phase 2 ─── Phase 3 ─┬─ Phase 4 ─── Phase 5 ─── Phase 6 ─── Phase 7 ─── Phase 8 ─── Phase 9
  │           │           │      │
  │           │           │      └─ (parallel track for frontend)
  │           │           │
  ▼           ▼           ▼
[Assess]  [Stabilize] [Users]     [Sessions] → [Evaluation] → [Feedback] → [Progress] → [Harden]
```

### Why This Order?

1. **Phase 1 first**: Cannot build on unstable foundation. Must understand what exists.

2. **Phase 2 before features**: Storage and async processing are prerequisites for Sessions and Evaluation.

3. **Phase 3 minor**: Users module 95% complete. Only small additions needed for dashboard integration.

4. **Phase 4 (Sessions) before Phase 5 (Evaluation)**: Cannot analyze content that doesn't exist yet.

5. **Phase 5 (Evaluation) before Phase 6 (Feedback)**: Feedback is generated FROM evaluation results.

6. **Phase 6 (Feedback) before Phase 7 (Progress)**: Progress trends depend on feedback/evaluation data.

7. **Phase 8 after features**: Cannot harden what doesn't exist. Quality phase needs complete system.

8. **Phase 9 deferred**: Only after MVP validated with real users.

---

## 5. MVP Definition

### Must Have (MVP Scope)

| Feature | Phase | Priority |
|---------|-------|----------|
| Fix Gradle build issues | 2 | Critical |
| Local file storage | 2 | Critical |
| Create session | 4 | Critical |
| Upload video | 4 | Critical |
| Request analysis | 5 | Critical |
| Generate transcription | 5 | Critical |
| Calculate basic metrics (pace, fillers) | 5 | Critical |
| View evaluation report | 5 | Critical |
| View feedback notes | 6 | Critical |
| Navigate to timestamp | 6 | Critical |
| Sessions history | 7 | High |

### Can Wait (Post-MVP)

| Feature | Phase | Reason |
|---------|-------|--------|
| Slide upload | 4 | Nice-to-have, video is core |
| Full metrics suite | 5 | Start with 3-4 metrics |
| PDF export | 7 | Manual review sufficient initially |
| Goals/Milestones | 7 | Engagement feature, not core |
| Compare sessions | 7 | Trends chart sufficient |
| Real-time coaching | 9 | Complex, requires product validation |
| Multi-language | 9 | English first |

### Should Not Start Yet

| Feature | Reason |
|---------|--------|
| Mobile app | Web MVP first |
| Coach/Teacher dashboards | B2C before B2B |
| Social features | Privacy concerns, scope creep |
| Benchmarking | Needs user data volume |

---

## 6. Blockers and Questions to Verify in Repository

### Critical Blockers

1. **Gradle Build Failure**: `VoxLyApplication` main method not detected
   - Status: **BLOCKING** - Must fix before any development
   - Action: Investigate compilation issue

2. **AI Provider Not Configured**
   - Question: Which service? OpenAI? Azure? Custom?
   - Action: Need decision to implement Phase 5

3. **Storage Provider Not Configured**
   - Question: Local filesystem for MVP? S3 for production?
   - Action: Need decision for Phase 2

### Questions to Verify

| Question | Impact | Default Assumption |
|----------|--------|-------------------|
| What is the max video file size? | Upload handling, storage costs | 500MB |
| Which video formats supported? | Validation, transcoding | MP4, WebM |
| What transcription service? | Integration code | OpenAI Whisper |
| What analysis AI model? | Integration code | GPT-4 |
| Is real-time recording needed for MVP? | Complexity | No, upload only |
| Database hosting in production? | Config | Neon (current) |
| Email service in production? | Config | SendGrid or similar |

### Technical Debt to Address

1. **Java 21 vs 24 mismatch**: build.gradle configured for Java 24, using 21
2. **CORS configuration**: Hardcoded localhost ports
3. **Secrets in application.yml**: Should use environment variables
4. **No test coverage**: Zero tests currently
5. **No CI/CD pipeline**: Manual deployment only

---

## 7. Next Immediate Action

### Step 1: Fix the Gradle Build Issue

The most critical blocker is the broken build. The `VoxLyApplication.main()` method is not being compiled into the bytecode, preventing the application from running.

**Immediate actions**:

1. Verify `VoxLyApplication.java` file encoding (should be UTF-8)
2. Check for invisible characters in the source file
3. Clean Gradle caches completely: `./gradlew clean --no-daemon`
4. Verify Lombok annotation processor is working
5. Test with explicit main class in `build.gradle` (already added)
6. If persists, recreate `VoxLyApplication.java` from scratch

**Command to execute**:
```bash
cd backend
rm -rf build .gradle ~/.gradle/caches
./gradlew clean compileJava --no-daemon --info
```

**Verification**:
```bash
javap -public build/classes/java/main/com/pigs/voxly/VoxLyApplication.class
```

Should show:
```
public static void main(java.lang.String[]);
```

### After Build Fixed

Proceed to **Phase 1** - Full architecture assessment and documentation.

---

## Appendix A: File Structure Reference

### Current Backend Structure
```
backend/src/main/java/com/pigs/voxly/
├── VoxLyApplication.java
├── api/
│   ├── config/OpenApiConfig.java
│   ├── identity/
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   ├── cookie/CookieHelper.java
│   │   └── dto/
│   └── shared/GlobalExceptionHandler.java
├── application/
│   └── identity/
│       ├── AuthService.java
│       ├── UserService.java
│       ├── dto/
│       ├── eventhandlers/
│       └── ports/
├── domain/
│   └── identity/
│       ├── User.java
│       ├── UserId.java
│       ├── UserRepository.java
│       ├── UserErrors.java
│       ├── enumerations/
│       ├── entities/
│       ├── events/
│       └── valueobjects/
├── infrastructure/
│   └── identity/
│       ├── config/
│       ├── email/
│       ├── persistence/
│       └── security/
└── sharedKernel/
    └── domain/
        ├── ddd/
        ├── events/
        ├── exceptions/
        ├── guards/
        ├── results/
        ├── types/
        └── pagination/
```

### Proposed New Modules
```
backend/src/main/java/com/pigs/voxly/
├── domain/
│   ├── sessions/      (Phase 4)
│   ├── evaluation/    (Phase 5)
│   ├── feedback/      (Phase 6)
│   └── progress/      (Phase 7)
├── application/
│   ├── sessions/      (Phase 4)
│   ├── evaluation/    (Phase 5)
│   ├── feedback/      (Phase 6)
│   └── progress/      (Phase 7)
├── api/
│   ├── sessions/      (Phase 4)
│   ├── evaluation/    (Phase 5)
│   ├── feedback/      (Phase 6)
│   └── progress/      (Phase 7)
└── infrastructure/
    ├── sessions/      (Phase 4)
    ├── evaluation/    (Phase 5)
    ├── feedback/      (Phase 6)
    ├── progress/      (Phase 7)
    └── shared/
        ├── storage/   (Phase 2)
        └── async/     (Phase 2)
```

---

## Appendix B: Technology Stack Summary

| Layer | Technology | Version |
|-------|------------|---------|
| Backend Framework | Spring Boot | 4.0.3 |
| Language | Java | 21 |
| Build Tool | Gradle | 9.3.1 |
| Database | PostgreSQL | 15+ (Neon) |
| ORM | JPA/Hibernate | (Spring managed) |
| Security | Spring Security + JWT | (Spring managed) |
| API Docs | SpringDoc OpenAPI | 3.0.2 |
| Email | Spring Mail | (Spring managed) |
| Frontend Framework | React | 19 |
| Frontend Language | TypeScript | 5.9.3 |
| Build Tool (FE) | Vite | 7.3.1 |
| Styling | Tailwind CSS | 3.4.17 |
| Routing | React Router | 7.13.1 |

---

*Document Version: 1.0*
*Created: 2024*
*Last Updated: Phase 0 - Initial Planning*
