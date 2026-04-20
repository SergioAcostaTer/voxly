# Feature Backlog

**Current Sprint:** Sprint 1 (Foundation)  
**Total Features:** 43 (38 User Stories + 5 Technical Stories)

---

## 🔴 Phase 1: Authentication & User Management

## 🔴 Phase 1: Authentication & User Management

### US.1.1 - User Registration ✅ DONE
**Owner:** Eduardo Marrero González  
**Priority:** 🔴 CRITICAL  
**Complexity:** Medium  
**Story Points:** 5

**Description:** Users register with name, email, password, and optional professional profile.

**Status:** COMPLETE ✅
- ✅ Backend: `AuthController.register()` endpoint implemented
- ✅ Password hashing with BCrypt
- ✅ Email validation
- ✅ Duplicate email checking
- ✅ Frontend: Registration form with validation
- ✅ Error handling and feedback

**Backend Location:** `api/identity/AuthController.java`  
**Frontend Location:** `pages/AuthPage.tsx`  
**Tests:** Implemented in AuthController

---

### US.1.2 - Professional Profile Definition 🔄 IN PROGRESS
**Priority:** 🟡 HIGH  
**Complexity:** Low  
**Story Points:** 2

**Status:** Partial - DTOs exist, frontend needs work
- ✅ Backend DTO structure created
- ⏳ Frontend UI for profile definition
- ⏳ Profile validation

**Dependencies:** US.1.1

---

### US.2.1 - User Login ✅ DONE
**Priority:** 🔴 CRITICAL  
**Complexity:** Medium  
**Story Points:** 5

**Status:** COMPLETE ✅
- ✅ Backend: `AuthController.login()` with JWT token generation
- ✅ Cookie-based token storage
- ✅ Authentication token handling
- ✅ Failed login error handling
- ✅ Frontend: Login form with email/password
- ✅ Token storage in AuthContext

**Backend Location:** `api/identity/AuthController.java`  
**Frontend Location:** `auth/AuthContext.tsx`  
**Tests:** Implemented

---

### US.3.1 - User Logout ✅ DONE
**Priority:** 🟡 HIGH  
**Complexity:** Low  
**Story Points:** 1

**Status:** COMPLETE ✅
- ✅ Backend: `AuthController.logout()` endpoint
- ✅ Cookie clearing
- ✅ Token revocation
- ✅ Frontend: Logout button in UI

**Backend Location:** `api/identity/AuthController.java`

---

### US.4.1 - Password Recovery 🔄 IN PROGRESS
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 5

**Status:** Partial - Backend DTOs prepared, needs implementation
- ✅ Backend: DTOs created (RequestPasswordResetRequest, ResetPasswordRequest)
- ✅ Email service configured
- ⏳ Password reset token generation
- ⏳ Email sending logic
- ⏳ Token validation
- ⏳ Frontend UI for password recovery

**Dependencies:** T3 (Email Service Integration)

---

### US.5.1 - View Profile ✅ DONE
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

**Status:** COMPLETE ✅
- ✅ Backend: `UserController.getCurrentUser()` endpoint
- ✅ Frontend: Profile display in DashboardPage
- ✅ User data fetching and display

**Backend Location:** `api/identity/UserController.java`

---

### US.6.1 - Edit Profile ✅ DONE
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

**Status:** COMPLETE ✅
- ✅ Backend: `UserController.updateProfile()` endpoint
- ✅ UpdateProfileRequest DTO
- ✅ Frontend: Edit form (needs UI completion)

**Backend Location:** `api/identity/UserController.java`

---

### US.7.1 - Change Password 🔄 IN PROGRESS
**Priority:** 🟡 HIGH  
**Complexity:** Low  
**Story Points:** 2

**Status:** Partial - Backend prepared
- ✅ DTOs created (ResetPasswordRequest)
- ⏳ Backend implementation
- ⏳ Frontend UI

**Backend Location:** `api/identity/AuthController.java`

---

### US.8.1 - External Provider Authentication ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 5

**Status:** NOT STARTED
- OAuth2 integration needed
- Google provider setup
- Microsoft provider setup

**Dependencies:** T3 (OAuth2 Configuration)

---

### US.9.1 - Two-Factor Authentication 🔄 IN PROGRESS
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 5

**Status:** Partial - Backend endpoints exist
- ✅ Backend: `UserController.enableTwoFactor()` endpoint
- ✅ Backend: `UserController.disableTwoFactor()` endpoint
- ✅ DTOs: RequestTwoFactorCodeRequest created
- ⏳ 2FA logic implementation
- ⏳ Frontend UI for 2FA setup

**Backend Location:** `api/identity/UserController.java`

---

## 🟠 Phase 2: Session Management & Content Upload

### US.10.1 - Create Session ✅ DONE
**Priority:** 🔴 CRITICAL  
**Complexity:** Medium  
**Story Points:** 5

**Status:** COMPLETE ✅
- ✅ Backend: `SessionController.createSession()` endpoint
- ✅ Session entity created
- ✅ Repository setup
- ✅ Frontend: Session creation flow

**Backend Location:** `api/sessions/SessionController.java`

---

### US.11.1 - Upload Slides 🔄 IN PROGRESS
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 5

**Status:** Partial - Upload endpoint exists, storage integration needed
- ✅ Backend: `SessionController.uploadMedia()` endpoint
- ✅ Multipart file handling
- ✅ Frontend: Upload UI (basic)
- ⏳ Cloud storage integration (T3)
- ⏳ File validation

**Dependencies:** T3 (Cloud Storage)

---

### US.12.1 - Record Presentation ⏳ TODO
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Status:** NOT STARTED
- Frontend: MediaRecorder API implementation needed
- Recording UI
- Permission handling

---

### US.12.2 - Pause/Resume Recording ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 3

**Status:** Blocked by US.12.1

---

### US.13.1 - Upload Video File ✅ DONE (Partial)
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Status:** Partial - Endpoint works, cloud storage integration pending
- ✅ Backend: `SessionController.uploadMedia()` handles video
- ✅ Multipart file support
- ✅ File validation
- ✅ Frontend: Upload component with progress
- ⏳ Chunked upload for large files
- ⏳ Cloud storage integration (T3)

**Dependencies:** T3 (Cloud Storage)

---

### US.13.2 - Upload Progress Feedback ✅ DONE (Partial)
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

**Status:** Partial - Backend ready, frontend UI needs work
- ✅ Backend supports multipart progress
- ⏳ Frontend progress bar UI

---

### US.14.1 - Delete Session ✅ DONE
**Priority:** 🟡 HIGH  
**Complexity:** Low  
**Story Points:** 2

**Status:** COMPLETE ✅
- ✅ Backend: `SessionController.deleteSession()` endpoint
- ✅ Frontend: Delete button (needs UI)

**Backend Location:** `api/sessions/SessionController.java`

---

### US.15.1 - View Session Details ✅ DONE
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 3

**Status:** COMPLETE ✅
- ✅ Backend: `SessionController.getSession()` endpoint
- ✅ Frontend: SessionDetailPage created
- ✅ Data display

**Backend Location:** `api/sessions/SessionController.java`  
**Frontend Location:** `pages/SessionDetailPage.tsx`

---

### US.16.1 - View All Sessions ✅ DONE
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 3

**Status:** COMPLETE ✅
- ✅ Backend: `SessionController.getUserSessions()` with pagination
- ✅ Frontend: SessionsPage with list display
- ✅ Pagination support

**Backend Location:** `api/sessions/SessionController.java`  
**Frontend Location:** `pages/SessionsPage.tsx`

---

### US.17.1 - View Session Data Information ✅ DONE
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

**Status:** COMPLETE ✅
- ✅ Backend: Included in `getSession()` endpoint
- ✅ Frontend: Metadata display in detail page

---

## 🟡 Phase 3: Analysis & Feedback

### US.18.1 - Request Session Analysis 🔄 IN PROGRESS
**Owner:** Wail Ben El Hassane Boudhar  
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Status:** 60% - Backend working, need AI integration
- ✅ Backend: `SessionController.requestAnalysis()` endpoint
- ✅ EvaluationController with startEvaluation() method
- ✅ Session ownership validation
- ✅ Video presence validation
- ⏳ Analysis queue/job processing
- ⏳ Frontend button and status UI
- ⏳ ChatGPT integration (T3)

**Backend Location:** `api/sessions/SessionController.java`, `api/evaluation/EvaluationController.java`

---

### US.18.2 - View Analysis Status 🔄 IN PROGRESS
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 3

**Status:** Partial - Backend ready
- ✅ Backend: Status tracking in EvaluationService
- ⏳ Frontend: Status UI in detail page

**Dependencies:** US.18.1

---

### US.19.1 - View Transcription ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 5

**Status:** NOT STARTED
- Needs transcription service integration
- Frontend display component

---

### US.25.1 - Timestamped Transcription ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 5

**Status:** NOT STARTED
- Depends on US.19.1
- Frontend timeline display

---

### US.20.1 - Generate Evaluation Report 🔄 IN PROGRESS
**Priority:** 🟡 HIGH  
**Complexity:** High  
**Story Points:** 8

**Status:** Partial - Backend structure ready
- ✅ EvaluationService exists
- ⏳ Report generation logic
- ⏳ Metrics aggregation
- ⏳ Frontend report display

---

### US.21.1 - View Evaluation Report 🔄 IN PROGRESS
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 3

**Status:** Partial - Pages created
- ✅ Frontend: Report display structure
- ⏳ Report formatting and styling

---

### US.22.1 - Analysis Completion Notification ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Medium  
**Story Points:** 3

**Status:** NOT STARTED
- Email notification setup
- Frontend notification display

**Dependencies:** T3 (Email Service)

---

### US.23.1 - Calculate Presentation Metrics ✅ DONE (Partial)
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Status:** Partial - Backend ready
- ✅ EvaluationService structure
- ✅ API endpoints for metrics
- ⏳ Audio analysis algorithms
- ⏳ Metrics calculation implementation

---

### US.26.1 - AI Analysis and Metrics Generation 🔄 IN PROGRESS
**Owner:** Sergio Acosta Quintana  
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Status:** 40% - Backend structure ready, AI integration pending
- ✅ EvaluationController with getEvaluation() method
- ✅ EvaluationService structure
- ✅ API endpoints working
- ⏳ ChatGPT API integration (T3)
- ⏳ Response parsing
- ⏳ Frontend feedback display

---

### US.27.1 - View Calculated Metrics ✅ DONE (Partial)
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 3

**Status:** Partial - Backend ready
- ✅ Backend: Metrics in EvaluationResponse
- ⏳ Frontend: Metrics display UI

---

## 🟢 Phase 4: Feedback & Video Playback

### US.28.1 - Video Playback with Synchronized Notes ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** High  
**Story Points:** 8

**Status:** NOT STARTED
- HTML5 video player implementation
- Timeline synchronization
- Click-to-seek functionality

---

### US.29.1 - View Notes Summary ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

---

### US.30.1 - View Detailed Note Information ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

---

### US.31.1 - Filter Notes by Category ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

---

### US.32.1 - Navigate to Video Timestamp ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

---

## 📊 Phase 5: Progress Tracking

### US.33.1 - View Session History ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Medium  
**Story Points:** 3

---

### US.34.1 - Compare Session Results ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Medium  
**Story Points:** 5

---

### US.35.1 - Set Improvement Goals ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Medium  
**Story Points:** 5

---

### US.36.1 - View Progress Trend ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Medium  
**Story Points:** 5

---

### US.37.1 - Export Progress Summary ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 3

---

### US.38.1 - View Progress by Category ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Medium  
**Story Points:** 3

---

## 🔧 Technical Stories

### T1 - Database Design ✅ DONE
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Status:** COMPLETE ✅
- ✅ PostgreSQL configured and connected
- ✅ JPA entities created (User, Session, Evaluation, etc.)
- ✅ Repositories configured
- ✅ Migrations with Hibernate auto-update

**Configuration:** `application.yml`

---

### T2 - Git Monorepo Setup ✅ DONE
**Priority:** 🔴 CRITICAL  
**Complexity:** Medium  
**Story Points:** 3

**Status:** COMPLETE ✅
- ✅ Backend (Spring Boot with Gradle)
- ✅ Frontend (React with Vite)
- ✅ Docker Compose for services
- ✅ Git repository structure

---

### T3 - External Services Integration 🔄 IN PROGRESS
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Status:** 30% - Partial setup
- ✅ Email service configured (localhost:1025)
- ⏳ Cloud Storage (AWS S3 or Azure Blob) - NOT STARTED
- ⏳ ChatGPT API integration - NOT STARTED
- ⏳ OAuth providers (Google, Microsoft) - NOT STARTED

**Blocking:** US.18.1, US.26.1, US.4.1, US.8.1, US.11.1, US.13.1

---

### T4 - Use Case Specification ✅ DONE
**Status:** COMPLETE ✅
**Reference:** `docs/USE_CASES_AND_USER_STORIES.md`

---

### T5 - API Architecture ✅ DONE (Partial)
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 5

**Status:** 90% - Substantially complete
- ✅ OpenAPI/Swagger configuration
- ✅ JWT Bearer auth scheme defined
- ✅ Spring Security setup
- ✅ CORS configuration (basic)
- ✅ Error handling (ResultMapper)
- ⏳ API versioning refinement
- ⏳ Rate limiting

**Configuration:** `api/config/OpenApiConfig.java`

---

## 📈 Updated Backlog Statistics

| Status | Count | Story Points |
|--------|-------|--------------|
| ✅ Done | 15 | 65+ |
| 🔄 In Progress | 10 | 60 |
| ⏳ Todo | 18 | 20 |
| **Total** | **43** | **145** |

**Completion:** ~45% of total work done

---

**Last Updated:** 2026-04-20 (SYNCED WITH ACTUAL CODEBASE)  
**Next Review:** 2026-04-27
**Owner:** Eduardo Marrero González  
**Priority:** 🔴 CRITICAL  
**Complexity:** Medium  
**Story Points:** 5

**Description:** Users register with name, email, password, and optional professional profile.

**Backend Tasks:**
- [ ] Create User entity and UserRepository
- [ ] Implement registration endpoint POST /api/auth/register
- [ ] Add password hashing (BCrypt)
- [ ] Implement email validation
- [ ] Add duplicate email check

**Frontend Tasks:**
- [ ] Create registration form component
- [ ] Add form validation
- [ ] Implement error handling
- [ ] Add success feedback

**Tests:**
- [ ] Valid registration succeeds
- [ ] Duplicate email rejected
- [ ] Weak password rejected
- [ ] Missing fields rejected

**Dependencies:** T1 (Database Design), T5 (API Architecture)

---

### US.1.2 - Professional Profile Definition ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Low  
**Story Points:** 2

**Description:** Users define their professional profile during or after registration.

**Dependencies:** US.1.1

---

### US.2.1 - User Login ⏳ TODO
**Priority:** 🔴 CRITICAL  
**Complexity:** Medium  
**Story Points:** 5

**Description:** Users login with email and password to access platform.

**Backend Tasks:**
- [ ] Implement login endpoint POST /api/auth/login
- [ ] Generate JWT token
- [ ] Return authentication token
- [ ] Handle failed login attempts

**Frontend Tasks:**
- [ ] Create login form
- [ ] Store authentication token (localStorage/sessionStorage)
- [ ] Setup authenticated API calls

---

### US.3.1 - User Logout ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Low  
**Story Points:** 1

---

### US.4.1 - Password Recovery ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 5

**Backend Tasks:**
- [ ] Implement password reset endpoint
- [ ] Generate reset token
- [ ] Send reset email
- [ ] Validate reset token

**Dependencies:** T3 (Email Service Integration)

---

### US.5.1 - View Profile ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

---

### US.6.1 - Edit Profile ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

---

### US.7.1 - Change Password ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Low  
**Story Points:** 2

---

### US.8.1 - External Provider Authentication ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 5

**Description:** OAuth2 integration with Google and Microsoft

**Dependencies:** T3 (External Services Integration)

---

### US.9.1 - Two-Factor Authentication ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 5

---

## 🟠 Phase 2: Session Management

### US.10.1 - Create Session ⏳ TODO
**Priority:** 🔴 CRITICAL  
**Complexity:** Medium  
**Story Points:** 5

**Backend Tasks:**
- [ ] Create Session entity
- [ ] Implement create session endpoint POST /api/sessions
- [ ] Generate session ID
- [ ] Set initial status to "CREATED"

**Frontend Tasks:**
- [ ] Create "New Session" button/page
- [ ] Show session creation confirmation

---

### US.11.1 - Upload Slides ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 5

**Backend Tasks:**
- [ ] Create file upload endpoint
- [ ] Store slides to cloud storage
- [ ] Associate slides with session

**Dependencies:** T3 (Cloud Storage Integration)

---

### US.12.1 - Record Presentation ⏳ TODO
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Frontend Tasks:**
- [ ] Implement browser MediaRecorder API
- [ ] Create recording UI
- [ ] Show recording timer
- [ ] Handle browser permissions

---

### US.12.2 - Pause/Resume Recording ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 3

---

### US.13.1 - Upload Video File ⏳ TODO
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Backend Tasks:**
- [ ] Create video upload endpoint
- [ ] Implement chunked upload for large files
- [ ] Store to cloud storage
- [ ] Validate video format

**Frontend Tasks:**
- [ ] Create file upload UI with drag-and-drop
- [ ] Show upload progress bar

---

### US.13.2 - Upload Progress Feedback ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

---

### US.14.1 - Delete Session ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Low  
**Story Points:** 2

---

### US.15.1 - View Session Details ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 3

---

### US.16.1 - View All Sessions ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 3

---

### US.17.1 - View Session Data Information ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

---

## 🟡 Phase 3: Analysis & Feedback

### US.18.1 - Request Session Analysis 🔄 IN PROGRESS
**Owner:** Wail Ben El Hassane Boudhar  
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Description:** Triggers analysis of session video and generates feedback.

**Backend Tasks:**
- [ ] Create analysis request endpoint POST /api/sessions/{id}/analyze
- [ ] Validate session ownership and video presence
- [ ] Update session status to PENDING
- [ ] Queue analysis job

**Frontend Tasks:**
- [ ] Add "Request Analysis" button
- [ ] Show analysis status
- [ ] Disable duplicate requests

**Dependencies:** T3 (AI Service Integration), US.13.1

---

### US.18.2 - View Analysis Status ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 3

---

### US.19.1 - View Transcription ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 5

---

### US.25.1 - Timestamped Transcription ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 5

**Dependencies:** US.19.1

---

### US.20.1 - Generate Evaluation Report ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** High  
**Story Points:** 8

**Backend Tasks:**
- [ ] Create report generation logic
- [ ] Aggregate metrics and feedback
- [ ] Format report data

**Frontend Tasks:**
- [ ] Create report display component
- [ ] Add export functionality

**Dependencies:** US.18.1, US.26.1

---

### US.21.1 - View Evaluation Report ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 3

---

### US.22.1 - Analysis Completion Notification ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Medium  
**Story Points:** 3

**Backend Tasks:**
- [ ] Implement notification service
- [ ] Send notifications on analysis completion

**Dependencies:** T3 (Email/Push Notification Service)

---

### US.23.1 - Calculate Presentation Metrics ⏳ TODO
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Backend Tasks:**
- [ ] Analyze video for speaking metrics
- [ ] Calculate pace, filler words, pauses
- [ ] Calculate speech clarity metrics

---

### US.26.1 - AI Analysis and Metrics Generation 🔄 IN PROGRESS
**Owner:** Sergio Acosta Quintana  
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Description:** AI-powered analysis generating feedback on presentation.

**Backend Tasks:**
- [ ] Integrate ChatGPT API
- [ ] Create analysis prompt engineering
- [ ] Parse AI responses
- [ ] Store feedback in database

**Frontend Tasks:**
- [ ] Display AI feedback
- [ ] Show analysis progress

**Dependencies:** T3 (ChatGPT Integration)

---

### US.27.1 - View Calculated Metrics ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** Medium  
**Story Points:** 3

---

## 🟢 Phase 4: Feedback & Progress

### US.28.1 - Video Playback with Synchronized Notes ⏳ TODO
**Priority:** 🟡 HIGH  
**Complexity:** High  
**Story Points:** 8

**Frontend Tasks:**
- [ ] Implement HTML5 video player
- [ ] Synchronize notes with video timeline
- [ ] Create click-to-seek functionality
- [ ] Add playback controls

---

### US.29.1 - View Notes Summary ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

---

### US.30.1 - View Detailed Note Information ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

---

### US.31.1 - Filter Notes by Category ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

---

### US.32.1 - Navigate to Video Timestamp ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 2

---

### US.24.1 - Metrics Calculation 🔄 IN PROGRESS
**Owner:** Gorka Eymard Santana Cabrera  
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Description:** Calculate objective presentation metrics from video/audio.

**Backend Tasks:**
- [ ] Implement audio analysis
- [ ] Calculate speaking rate (WPM)
- [ ] Detect filler words
- [ ] Analyze speech clarity

---

## 📊 Phase 5: Progress Tracking

### US.33.1 - View Session History ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Medium  
**Story Points:** 3

---

### US.34.1 - Compare Session Results ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Medium  
**Story Points:** 5

---

### US.35.1 - Set Improvement Goals ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Medium  
**Story Points:** 5

---

### US.36.1 - View Progress Trend ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Medium  
**Story Points:** 5

**Frontend Tasks:**
- [ ] Create chart component (Chart.js or similar)
- [ ] Display progress over time

---

### US.37.1 - Export Progress Summary ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Low  
**Story Points:** 3

---

### US.38.1 - View Progress by Category ⏳ TODO
**Priority:** 🟢 MEDIUM  
**Complexity:** Medium  
**Story Points:** 3

---

## 🔧 Technical Stories

### T1 - Database Design ⏳ TODO
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Tasks:**
- [ ] Design database schema
- [ ] Create entity models
- [ ] Setup JPA repositories
- [ ] Create migration scripts

---

### T2 - Git Monorepo Setup ⏳ TODO
**Priority:** 🔴 CRITICAL  
**Complexity:** Medium  
**Story Points:** 3

**Tasks:**
- [ ] Configure gradle for monorepo
- [ ] Setup CI/CD pipeline

---

### T3 - External Services Integration ⏳ TODO
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 8

**Services:**
- [ ] Cloud Storage (AWS S3 or Azure Blob)
- [ ] ChatGPT API integration
- [ ] Email Service (SendGrid or similar)
- [ ] OAuth Providers (Google, Microsoft)

---

### T4 - Use Case Specification ✅ DONE
**Status:** Completed  
**Reference:** `docs/USE_CASES_AND_USER_STORIES.md`

---

### T5 - API Architecture ⏳ TODO
**Priority:** 🔴 CRITICAL  
**Complexity:** High  
**Story Points:** 5

**Tasks:**
- [ ] Define API versioning strategy
- [ ] Configure Spring Security
- [ ] Setup JWT authentication
- [ ] Configure CORS
- [ ] Create error handling standard

---

## 📈 Backlog Statistics

| Status | Count |
|--------|-------|
| ✅ Done | 1 |
| 🔄 In Progress | 3 |
| ⏳ Todo | 39 |
| **Total** | **43** |

| Priority | Count |
|----------|-------|
| 🔴 Critical | 12 |
| 🟡 High | 19 |
| 🟢 Medium | 12 |

---

**Last Updated:** 2026-04-20  
**Next Review:** 2026-04-27
