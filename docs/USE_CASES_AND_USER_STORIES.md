# Use Cases and User Stories - Voxly

**Report:** REPORT NUMBER 2  
**Team:** TEAM NUMBER 13  
**Date:** 28/02/2026  
**Team Members:** Sergio Acosta Quintana, Wail Ben El Hassane Boudhar, Gorka Eymard Santana Cabrera, Eduardo Marrero González

---

## Use Cases Mapping

| UC | Use Case | User Stories |
|---|---|---|
| UC1 | User Registration | US.1.1, US.1.2 |
| UC2 | User Authentication | US.2.1, US.3.1, US.4.1 |
| UC3 | Profile Management | US.5.1, US.6.1, US.7.1 |
| UC4 | External Authentication | US.8.1, US.9.1 |
| UC5 | Session Creation | US.10.1 |
| UC6 | Content Upload | US.11.1, US.13.1, US.13.2 |
| UC7 | Recording | US.12.1, US.12.2 |
| UC8 | Session Management | US.14.1, US.15.1, US.16.1, US.17.1 |
| UC9 | Analysis Request | US.18.1, US.18.2 |
| UC10 | Transcription | US.19.1, US.25.1 |
| UC11 | Evaluation Report | US.20.1, US.21.1 |
| UC12 | Notifications | US.22.1 |
| UC13 | Metrics Calculation | US.23.1, US.26.1, US.27.1 |
| UC14 | Video Playback | US.28.1 |
| UC15 | Feedback Notes | US.29.1, US.30.1, US.31.1, US.32.1 |
| UC16 | Progress Tracking | US.33.1, US.34.1, US.35.1, US.36.1, US.37.1, US.38.1 |

---

## User Stories by Use Case

### UC1: User Registration

#### US.1.1 - Basic Registration
**As a user**, I want to register by providing my name, email and password so that I can create an account in the platform.

**Status:** Not Started  
**Owner:** Eduardo Marrero González

**Data:**
- Name (string, mandatory)
- Email (string, mandatory, unique)
- Password (string, mandatory, minimum 8 characters)
- Professional profile (optional)

**Restrictions:**
- All fields are mandatory
- Email must be unique in the system
- Password must be secure (minimum 8 characters, alphanumeric recommended)
- Email format must be valid

**Scenarios:**
1. Successful registration with valid data
2. Error due to duplicate email
3. Error due to invalid format or empty fields
4. Error due to weak password

**Tests:**
- T1: Verify successful registration and redirection to the home page
- T2: Validate that the system rejects an already registered email
- T3: Confirm error if the password is weak or the email has an invalid format
- T4: Check that registration fails if any data is missing

---

#### US.1.2 - Professional Profile Definition
**As a user**, I want to define my professional profile during registration so that the system can personalize my experience.

**Status:** Not Started  
**Related to:** US.1.1

---

### UC2: User Authentication

#### US.2.1 - Login
**As a user**, I want to log in with my email and password so that I can access my sessions and personal data.

**Status:** Not Started

---

#### US.3.1 - Logout
**As a user**, I want to log out of my account so that I can protect my access when I stop using the platform.

**Status:** Not Started

---

#### US.4.1 - Password Recovery
**As a user**, I want to recover my password through an email reset link so that I can regain access if I forget my credentials.

**Status:** Not Started

---

### UC3: Profile Management

#### US.5.1 - View Profile
**As a user**, I want to view my profile information so that I can verify my registered data.

**Status:** Not Started

---

#### US.6.1 - Edit Profile
**As a user**, I want to edit my profile information so that I can keep my personal data updated.

**Status:** Not Started

---

#### US.7.1 - Change Password
**As a user**, I want to change my password from my profile settings so that I can maintain secure access.

**Status:** Not Started

---

### UC4: External Authentication

#### US.8.1 - External Provider Authentication
**As a user**, I want to authenticate using an external provider (Google or Microsoft) so that I can access the platform without creating new credentials.

**Status:** Not Started

---

#### US.9.1 - Two-Factor Authentication
**As a user**, I want to enable two-factor authentication so that I can add an extra layer of security to my account.

**Status:** Not Started

---

### UC5: Session Creation

#### US.10.1 - Create Session
**As a user**, I want to create a new practice session so that I can organize a new presentation attempt.

**Status:** Not Started

---

### UC6: Content Upload

#### US.11.1 - Upload Slides
**As a user**, I want to upload slides to my session so that they can be associated with my presentation.

**Status:** Not Started

---

#### US.13.1 - Upload Video File
**As a user**, I want to upload a presentation video file so that it can be stored and analyzed by the system.

**Status:** Not Started

---

#### US.13.2 - Upload Progress Feedback
**As a user**, I want to see upload progress feedback so that I know when the upload is complete.

**Status:** Not Started

---

### UC7: Recording

#### US.12.1 - Record Presentation
**As a user**, I want to record my presentation directly in the platform so that I can practice without external tools.

**Status:** Not Started

---

#### US.12.2 - Pause and Resume Recording
**As a user**, I want to pause and resume the recording so that I can manage interruptions during practice.

**Status:** Not Started

---

### UC8: Session Management

#### US.14.1 - Delete Session
**As a user**, I want to delete a session so that I can remove unwanted practice attempts.

**Status:** Not Started

---

#### US.15.1 - View Session Details
**As a user**, I want to view the details of a session so that I can review its associated video, slides and status.

**Status:** Not Started

---

#### US.16.1 - View All Sessions
**As a user**, I want to view all my sessions so that I can access my previous practice attempts.

**Status:** Not Started

---

#### US.17.1 - View Session Data Information
**As a user**, I want to view session data information so that I can check its metadata and current state.

**Status:** Not Started

---

### UC9: Analysis Request

#### US.18.1 - Request Session Analysis
**As a user**, I want to request the analysis of a session so that I can obtain feedback on my presentation.

**Status:** Not Started  
**Owner:** Wail Ben El Hassane Boudhar

**Data:**
- Session ID (identifier, mandatory)
- User ID (identifier, mandatory)
- Uploaded video file (file, mandatory)
- Associated slides (file, optional)
- Session metadata (title, date, duration)

**Restrictions:**
- User must be authenticated
- Session must exist and belong to the user
- Video file must be successfully uploaded before requesting analysis
- Only supported video formats allowed
- Analysis cannot be requested twice simultaneously for the same session

**Scenarios:**
1. Successful analysis request (valid session with uploaded video)
2. Error due to missing video file
3. Error due to requesting analysis on a session that does not belong to the user
4. Error due to analysis already in progress
5. Error due to unsupported or corrupted video file

**Tests:**
- T1: Verify that the system changes the session status to "Pending" after a successful analysis request
- T2: Validate that the system prevents analysis if the session has no uploaded video
- T3: Confirm that the system rejects analysis requests for sessions owned by another user
- T4: Check that the system blocks duplicate analysis requests while one is already in progress
- T5: Validate that the system shows an error message if the video format is not supported or the file is corrupted

---

#### US.18.2 - View Analysis Status
**As a user**, I want to see the analysis status so that I know whether it is pending, processing or completed.

**Status:** Not Started

---

### UC10: Transcription

#### US.19.1 - View Transcription
**As a user**, I want to view the generated transcription so that I can review what I said during my presentation.

**Status:** Not Started

---

#### US.25.1 - Automatic Timestamped Transcription
**As a user**, I want the system to generate a timestamped transcription automatically so that I can read my speech synchronized with time markers.

**Status:** Not Started

---

### UC11: Evaluation Report

#### US.20.1 - Generate Evaluation Report
**As a user**, I want to generate an evaluation report so that I can receive structured feedback about my performance.

**Status:** Not Started

---

#### US.21.1 - View Evaluation Report
**As a user**, I want to view the evaluation report so that I can analyze my scores and recommendations.

**Status:** Not Started

---

### UC12: Notifications

#### US.22.1 - Analysis Completion Notification
**As a user**, I want to receive a notification when the analysis is completed so that I do not need to check manually.

**Status:** Not Started

---

### UC13: Metrics Calculation

#### US.23.1 - Calculate Presentation Metrics
**As a user**, I want the system to calculate presentation metrics so that I can obtain objective performance indicators.

**Status:** Not Started

---

#### US.26.1 - AI Analysis and Metrics Generation
**As a user**, I want the system to calculate presentation metrics as part of the analysis process so that the evaluation includes quantitative data.

**Status:** Not Started  
**Owner:** Sergio Acosta Quintana

**Data:**
- User ID (identifier, mandatory)
- Session ID (identifier, mandatory)
- Input text (string, mandatory, minimum length required)
- Analysis status (enum: pending, processing, completed)
- Generated feedback (string, output)

**Restrictions:**
- User must be authenticated
- Session must exist and belong to the user
- Input text must not be empty and must meet minimum length
- Only one analysis request per session at a time
- Supported text formats only

**Scenarios:**
1. Successful AI analysis request (valid session and input)
2. Error due to empty or insufficient input text
3. Error due to requesting analysis for a session that does not belong to the user
4. Error due to analysis already in progress
5. Error due to unsupported format or AI service failure

**Tests:**
- T1: Verify that the system changes the session status to "Pending" after a successful analysis request
- T2: Validate that the system prevents analysis if the input text is empty
- T3: Confirm that the system rejects analysis requests for sessions owned by another user
- T4: Check that the system blocks duplicate analysis requests
- T5: Validate that the system updates the status to "Completed" after successful processing

---

#### US.27.1 - View Calculated Metrics
**As a user**, I want to view calculated presentation metrics so that I can clearly understand my performance indicators.

**Status:** Not Started

---

### UC14: Video Playback

#### US.28.1 - Play Video with Synchronized Notes
**As a user**, I want to play my presentation video with synchronized notes so that I can review feedback in context.

**Status:** Not Started

---

### UC15: Feedback Notes

#### US.29.1 - View Notes Summary
**As a user**, I want to view a summary of notes so that I can have an overall perspective of my feedback.

**Status:** Not Started

---

#### US.30.1 - View Detailed Note Information
**As a user**, I want to view detailed information about a note so that I can better understand the specific issue or strength.

**Status:** Not Started

---

#### US.31.1 - Filter Notes by Category
**As a user**, I want to filter notes by category so that I can focus on a particular aspect of my presentation.

**Status:** Not Started

---

#### US.32.1 - Navigate to Video Timestamp
**As a user**, I want to navigate directly to the moment of the video associated with a note so that I can quickly review that segment.

**Status:** Not Started

---

### UC16: Progress Tracking

#### US.33.1 - View Session History
**As a user**, I want to view the history of my sessions so that I can review my past performance records.

**Status:** Not Started

---

#### US.34.1 - Compare Session Results
**As a user**, I want to compare results between sessions so that I can identify differences and improvements over time.

**Status:** Not Started

---

#### US.35.1 - Set Improvement Goals
**As a user**, I want to set improvement goals so that I can guide my future practice sessions toward specific targets.

**Status:** Not Started

---

#### US.36.1 - View Progress Trend
**As a user**, I want to view my progress trend over time so that I can detect patterns of improvement or stagnation.

**Status:** Not Started

---

#### US.37.1 - Export Progress Summary
**As a user**, I want to export a summary of my progress so that I can share it or keep it as a performance record.

**Status:** Not Started

---

#### US.38.1 - View Progress by Category
**As a user**, I want to view my progress by category so that I can focus on the evolution of a specific metric.

**Status:** Not Started

---

## Technical Stories

### T1 - Database Design
**Type:** Technical  
**Status:** Not Started  
**Description:** Design and creation of database schemas and models.

---

### T2 - Git Monorepo Setup
**Type:** Technical  
**Status:** Not Started  
**Description:** Initialization of Git monorepo with all services and applications set up.

---

### T3 - External Services Integration
**Type:** Technical  
**Status:** Not Started  
**Description:** Integration and configuration of external services (storage, AI, notifications...).

**Services to integrate:**
- Cloud Storage (for video/slide uploads)
- AI Service (for analysis and feedback generation)
- Email Service (for notifications and password recovery)
- External Authentication Providers (Google, Microsoft)

---

### T4 - Use Case Specification
**Type:** Technical  
**Status:** Not Started  
**Description:** Design and creation of use case diagrams and detailed use case specifications.

---

### T5 - API Architecture
**Type:** Technical  
**Status:** Not Started  
**Description:** Definition of API architecture and security configuration.

**Key components:**
- RESTful API design
- Authentication/Authorization (JWT, OAuth)
- Rate limiting
- Input validation
- CORS configuration
- Error handling standards

---

## Detailed User Story Descriptions

### Detailed: US.1.1 - Basic Registration (Eduardo Marrero González)

**Status:** Not Started

**Data:**
- Name (string, mandatory)
- Email (string, mandatory, unique)
- Password (string, mandatory, minimum 8 characters)
- Professional profile (string, optional)

**Restrictions:**
- Mandatory fields: Name, Email, Password
- Email must be unique in the system
- Secure password (minimum 8 characters)
- Email format must be valid

**Scenarios:**
1. **Successful Registration**: User provides valid data → Account created → Redirected to home page
2. **Duplicate Email Error**: User enters email already in system → System rejects registration
3. **Invalid Format Error**: User enters invalid email or empty fields → System shows validation errors
4. **Weak Password Error**: User enters password with < 8 characters → System rejects password

**Test Cases:**
- ✓ T1: Verify successful registration and redirection to the home page
- ✓ T2: Validate that the system rejects an already registered email
- ✓ T3: Confirm error if the password is weak or the email has an invalid format
- ✓ T4: Check that registration fails if any data is missing

---

### Detailed: US.18.1 - Request Session Analysis (Wail Ben El Hassane Boudhar)

**Status:** Not Started

**Data:**
- Session ID (identifier, mandatory)
- User ID (identifier, mandatory)
- Uploaded video file (file, mandatory)
- Associated slides (file, optional)
- Session metadata (title, date, duration)

**Restrictions:**
- User must be authenticated
- Session must exist and belong to the user
- Video file must be successfully uploaded before requesting analysis
- Only supported video formats allowed (MP4, MOV, AVI, WebM)
- Analysis cannot be requested twice simultaneously for the same session

**Scenarios:**
1. **Successful Request**: Valid session with uploaded video → Status changed to "Pending"
2. **Missing Video**: Session has no video file → Analysis rejected
3. **Not Session Owner**: User tries to analyze another user's session → Request rejected
4. **Duplicate Request**: Analysis already in progress → New request blocked
5. **Unsupported Format**: Video file is corrupted or unsupported format → Error shown

**Test Cases:**
- ✓ T1: Verify that the system changes the session status to "Pending" after successful analysis request
- ✓ T2: Validate that the system prevents analysis if the session has no uploaded video
- ✓ T3: Confirm that the system rejects analysis requests for sessions owned by another user
- ✓ T4: Check that the system blocks duplicate analysis requests while one is already in progress
- ✓ T5: Validate that the system shows an error message if the video format is not supported or the file is corrupted

---

### Detailed: US.24.1 - Metrics Calculation (Gorka Eymard Santana Cabrera)

**Status:** Not Started

**Data:**
- Presentation file (audio/video)
- Duration (integer, in seconds)
- Speech text (string)
- Pacing (float, words per minute)

**Restrictions:**
- Mandatory valid file
- Minimum duration threshold (e.g., 30 seconds)
- Supported formats only (MP4, MOV, WAV, MP3)
- All database interactions must be trimmed

**Scenarios:**
1. **Successful Calculation**: Valid presentation → Metrics calculated and displayed
2. **Unsupported Format**: File format not supported → Error shown
3. **Too Short**: Presentation duration below minimum → Error shown
4. **Corrupted Data**: File is corrupted or empty → Error shown

**Test Cases:**
- ✓ T1: Verify successful calculation of presentation metrics and display of objective performance data
- ✓ T2: Validate that the system rejects an unsupported file format
- ✓ T3: Confirm error if the presentation duration is below the minimum threshold
- ✓ T4: Check that metric calculation fails and shows an error if the input data is empty or corrupted

---

### Detailed: US.26.1 - AI Analysis and Metrics Generation (Sergio Acosta Quintana)

**Status:** Not Started

**Data:**
- User ID (identifier, mandatory)
- Session ID (identifier, mandatory)
- Input text (string, mandatory)
- Analysis status (enum: pending, processing, completed, failed)
- Generated feedback (string)

**Restrictions:**
- User must be authenticated
- Session must exist and belong to the user
- Input text must not be empty
- Minimum input text length required
- Only one analysis request per session at a time
- Supported text formats only

**Scenarios:**
1. **Successful Analysis**: Valid session and input → Status → "Pending" → Processing → "Completed"
2. **Empty Input**: Input text is empty → Analysis rejected
3. **Unauthorized**: User tries to analyze another user's session → Request rejected
4. **Duplicate Request**: Analysis already running → New request blocked
5. **Service Failure**: AI service unavailable → Error shown with retry option

**Test Cases:**
- ✓ T1: Verify that the system changes the session status to "Pending" after successful analysis request
- ✓ T2: Validate that the system prevents analysis if the input text is empty
- ✓ T3: Confirm that the system rejects analysis requests for sessions owned by another user
- ✓ T4: Check that the system blocks duplicate analysis requests
- ✓ T5: Validate that the system updates the status to "Completed" after successful processing

---

## Summary Statistics

| Category | Count |
|----------|-------|
| Total User Stories | 38 |
| Use Cases | 16 |
| Technical Stories | 5 |
| Detailed Descriptions | 4 |
| Test Cases (Total) | 20+ |

---

## Tools and References

| Tool | Purpose |
|------|---------|
| ChatGPT | Rewrite content, improve English phrasing and grammar, assist in generating user stories |

---

**Document Generated:** 2026-04-20  
**Last Updated:** 2026-04-20  
**Status:** Draft
