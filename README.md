# VoxLy - AI-Powered Presentation Analysis Platform

Transform your presentation skills with intelligent speech analysis, transcription, and personalized feedback powered by AI.

## 🎯 Features

### MVP (Minimum Viable Product)
- ✅ **Video Upload & Processing**: Record or upload your presentation videos (MP4, up to 100MB)
- ✅ **AI Transcription**: Automatic speech-to-text using OpenAI Whisper with multi-language support
- ✅ **Speech Analysis**: Comprehensive analysis of your speaking style, pace, clarity, and more
- ✅ **Multi-Language Support**: English (en) and Spanish (es) transcription and analysis
- ✅ **Session Management**: Create, organize, and track your practice sessions
- ✅ **User Authentication**: Secure login with JWT tokens
- ✅ **Clean Dashboard**: Beautiful, intuitive interface for managing sessions

### Planned Features
- Advanced feedback on body language, emotion detection, and audience engagement
- Comparison metrics across multiple sessions
- AI-generated improvement suggestions
- Slide deck analysis integration
- Export reports as PDF

---

## 🏗️ Project Architecture

### Tech Stack

**Backend**
- Java 21 with Spring Boot 4.0.3
- PostgreSQL (Neon) for data persistence
- Hibernate/JPA auto schema update (`ddl-auto=update`)
- OpenAI API (Whisper + GPT-3.5-turbo) for AI capabilities
- Cloudflare R2 for secure file storage
- JWT for authentication

**Frontend**
- React 19 with TypeScript
- Vite for ultra-fast bundling
- Tailwind CSS for styling
- React Router for navigation
- Lucide React for icons

### Project Structure

```
voxly/
├── backend/                    # Java Spring Boot API
│   ├── src/main/java/
│   │   ├── api/               # REST Controllers
│   │   ├── application/       # Business logic
│   │   ├── domain/            # Domain models
│   │   └── infrastructure/    # External services, persistence
│   ├── src/main/resources/
│   │   ├── application.yml    # Configuration (dev)
│   │   ├── application-prod.yml # Configuration (prod)
│   │   └── application-testing.yml # No-login testing profile
│   └── build.gradle           # Dependencies & build config
│
├── frontend/                   # React + TypeScript SPA
│   ├── src/
│   │   ├── pages/            # Page components
│   │   ├── components/       # Reusable components
│   │   ├── auth/             # Authentication logic
│   │   ├── lib/              # API client, utilities
│   │   ├── types/            # TypeScript type definitions
│   │   └── ui/               # UI component library
│   ├── vite.config.ts        # Vite configuration
│   ├── tailwind.config.js    # Tailwind configuration
│   └── package.json          # Dependencies
│
├── .env                       # Environment variables (LOCAL - DO NOT COMMIT)
├── .env.example               # Environment template (SAFE - commit this)
└── docs/                      # Documentation
```

---

## 🚀 Quick Start

### Prerequisites

- **Java 21+** (with Maven or Gradle)
- **Node.js 18+** and npm
- **Docker** and Docker Compose plugin for local infrastructure
- **FFmpeg** (for audio extraction from videos)

### 1️⃣ Clone & Setup Environment

```bash
# Clone the repository
git clone <your-repo-url>
cd voxly

# Copy environment template
cp .env.example .env

# EDIT .env with your actual values
# See next section for required credentials
```

### 2️⃣ Start Local Infrastructure

The repo includes one local infrastructure compose file at `backend/docker-compose.yml`.

It starts:
- PostgreSQL on `localhost:55432`
- MinIO API on `localhost:19100`
- MinIO console on `localhost:19101`
- Mailpit SMTP on `localhost:11025`
- Mailpit inbox UI on `localhost:18025`

Run it with:

```bash
make infra-up
```

Or directly:

```bash
cd backend
docker compose up -d
```

### 3️⃣ Configure Secrets & Credentials

Edit `.env` and fill in these required values:

```env
# DATABASE (Required)
DATABASE_URL=jdbc:postgresql://your-host/your-db
DATABASE_USERNAME=your_user
DATABASE_PASSWORD=your_password

# AI APIs (Choose ONE)
# Option A: OpenAI (Recommended for MVP)
OPENAI_API_KEY=sk-your_key_from_openai
OPENAI_WHISPER_MODEL=whisper-1
OPENAI_GPT_MODEL=gpt-3.5-turbo

# If model vars are omitted, backend defaults are:
# OPENAI_WHISPER_MODEL=whisper-1
# OPENAI_GPT_MODEL=gpt-3.5-turbo

# Option B: Groq (Free alternative, budget-friendly)
# GROQ_API_KEY=gsk_your_groq_key

# STORAGE (Required - S3/R2 only)
CLOUDFLARE_R2_ACCESS_KEY_ID=your_r2_key
CLOUDFLARE_R2_ACCESS_KEY_SECRET=your_r2_secret
CLOUDFLARE_R2_BUCKET_NAME=voxly-sessions
CLOUDFLARE_R2_PUBLIC_URL=https://your-cdn-url.com

# JWT & SECURITY (Change these in production!)
JWT_SECRET_KEY=your_min_32_char_secret_key_here

# APP CONFIG
APP_BASE_URL=http://localhost:3000
SERVER_PORT=8080
```

For local development, `backend/.env.example` already matches the compose defaults for Postgres, MinIO, and Mailpit.

### 4️⃣ Start Backend (API Server)

```bash
cd backend

# Option A: Using Gradle (recommended)
./gradlew clean build
./gradlew bootRun

# Option B: Using Maven
mvn clean install
mvn spring-boot:run

# The API will start at http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

**Backend Health Check:**
```bash
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

### 5️⃣ Start Frontend (React App)

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev

# The app will open at http://localhost:5173
```

**You should see:**
- Landing page with "Get Started" button
- Login/Register forms
- Beautiful Tailwind UI

### 6️⃣ Test the MVP Flow

1. **Create Account**: Go to http://localhost:5173 → Register
2. **Login**: Use your credentials
3. **Start Session**: Click "New Session"
   - Enter title: "My First Presentation"
   - Select type: "Presentation"
   - Select language: "English" (or "Español")
   - Click "Continue"
4. **Upload Video**: Drag & drop an MP4 video or select from computer
5. **Wait for Analysis**: The app will:
   - Extract audio from video
   - Transcribe with Whisper (5-10 min depending on video length)
   - Analyze speech patterns with GPT
   - Display results

---

## 🔧 Configuration Deep Dive

### Multi-Language Support

The app supports transcription and analysis in:
- **English** (en) - Default
- **Spanish** (es) - Full support

When creating a session, select your language. The transcription service will use this language preference.

### Model Defaults (Cheapest Usable)

VoxLy is configured to use low-cost defaults when model variables are not provided:

- `OPENAI_WHISPER_MODEL` default: `whisper-1`
- `OPENAI_GPT_MODEL` default: `gpt-3.5-turbo`
- `OPENAI_GPT_MAX_TOKENS` default: `1500`
- `OPENAI_GPT_TEMPERATURE` default: `0.7`

Recommended policy:

1. Keep defaults for MVP and early beta.
2. Only override model values if quality is clearly insufficient.
3. Upgrade analysis model first; keep transcription model unchanged unless needed.

### Database Schema

The backend now relies on Hibernate/JPA schema auto-update at startup:

- `spring.jpa.hibernate.ddl-auto=update`

For a frictionless local testing workflow without login, run backend with:

- `--spring.profiles.active=testing`

### Storage Options

**Production (Recommended)**
```env
STORAGE_TYPE=s3
CLOUDFLARE_R2_ACCESS_KEY_ID=...
CLOUDFLARE_R2_ACCESS_KEY_SECRET=...
```

VoxLy now runs in **S3/R2-only mode** by default to keep behavior consistent across environments.

**Local Development**
```env
STORAGE_TYPE=s3
CLOUDFLARE_R2_ENDPOINT=http://localhost:19100
CLOUDFLARE_R2_REGION=us-east-1
CLOUDFLARE_R2_ACCESS_KEY_ID=voxlyminio
CLOUDFLARE_R2_ACCESS_KEY_SECRET=voxlyminiosecret
CLOUDFLARE_R2_BUCKET_NAME=voxly-sessions
```

MinIO is only there to emulate the S3-compatible storage contract locally. `ffmpeg` should still live on the machine or inside the backend runtime image, not as a standalone service.

### Logging

Control logging verbosity in `.env`:
```env
LOGGING_LEVEL=INFO
LOGGING_LEVEL_COM_PIGS_VOXLY=DEBUG  # Verbose VoxLy logs
```

---

## 📱 API Endpoints

### Authentication
- `POST /v1/auth/register` - Create new account
- `POST /v1/auth/login` - Login (returns JWT token)
- `POST /v1/auth/refresh-token` - Refresh expired token
- `POST /v1/auth/logout` - Logout

### Sessions
- `POST /v1/sessions` - Create new session
- `GET /v1/sessions` - List user's sessions
- `GET /v1/sessions/{id}` - Get session details
- `PATCH /v1/sessions/{id}` - Update session
- `DELETE /v1/sessions/{id}` - Delete session
- `POST /v1/sessions/{id}/media` - Upload video
- `POST /v1/sessions/{id}/analyze` - Request analysis

### Transcription & Analysis
- `POST /v1/evaluations/{sessionId}/transcribe` - Request transcription
- `GET /v1/evaluations/{sessionId}/transcription` - Get transcription
- `GET /v1/evaluations/{sessionId}` - Get analysis results

Full API docs available at: `http://localhost:8080/swagger-ui.html`

---

## 💸 Cost Analysis (Cheapest-Usable First)

This section is focused on keeping VoxLy as cheap as possible while still usable for real users.

### Pricing Strategy

1. Start with free tiers where possible.
2. Pay only for AI usage and storage growth.
3. Keep quality acceptable, not maximum.
4. Move to higher-cost services only when there is a clear bottleneck.

### Recommended Low-Cost Stack (Default)

| Component | Cheapest Usable Option | Why |
|-----------|------------------------|-----|
| Frontend hosting | Cloudflare Pages / Vercel Hobby | Free, fast CDN, easy deploy |
| Backend hosting | Single small instance (Render/Fly/Railway) | One service is enough for MVP |
| Database | Neon free/small paid plan | PostgreSQL + serverless, good for MVP |
| File storage | Cloudflare R2 | Usually lower cost than S3 for storage + egress model |
| Email | Resend/Brevo free tier | Enough for auth and notifications initially |
| AI transcription | Whisper API with language hint | Good quality/cost balance |
| AI analysis | `gpt-3.5-turbo` (default) | Cheapest model that is still usable for text analysis |

### Monthly Cost Scenarios (Practical Estimates)

These are planning estimates (not billing guarantees). Verify current prices before launch.

| Scenario | Active users/month | Sessions/month | Estimated total/month |
|----------|--------------------|----------------|-----------------------|
| MVP test | 20-50 | 200-500 | $15-$60 |
| Early beta | 100-300 | 1,000-3,000 | $60-$250 |
| Small production | 500-1,000 | 5,000-10,000 | $250-$900 |

### Where the Money Goes First

In most AI products like this, spending usually grows in this order:

1. AI tokens/transcription
2. Video/file storage
3. Backend compute
4. Database
5. Everything else

### Hard Rules to Stay Cheap

Use these defaults unless there is a strong reason not to:

1. Keep `OPENAI_GPT_MODEL=gpt-3.5-turbo`.
2. Keep uploads capped at 100MB (already enforced).
3. Prefer single-pass analysis prompts (avoid long multi-call chains).
4. Store only required metadata and compact JSON outputs.
5. Add retention policy for old source videos (for example 30-90 days).
6. Cache/reuse analysis where possible instead of recomputing.
7. Keep one backend instance until sustained CPU/latency pressure appears.

### Cost Formula You Can Track Weekly

Use this quick formula:

`Total monthly cost ≈ AI + storage + backend + database + email`

Expanded:

`AI ≈ (sessions × avg transcription minutes × transcription unit price) + (sessions × avg analysis tokens × token price)`

This lets you update forecasts fast when usage changes.

### Upgrade Path (Only When Needed)

1. If analysis quality is not enough: move only analysis to a stronger model, keep transcription as-is.
2. If processing is slow: add one worker instance before changing DB/storage tiers.
3. If storage grows too much: enforce lifecycle deletion before paying for bigger compute.

### Recommended Default for VoxLy Today

For the current MVP, the cheapest usable baseline is:

1. `gpt-3.5-turbo` for analysis
2. Whisper API for transcription
3. Cloudflare R2 for media
4. Neon starter/free PostgreSQL
5. One small backend instance

This gives good quality while minimizing burn.

---

## 🐛 Troubleshooting

### Backend Issues

**Error: "Cannot connect to database"**
```bash
# Check DATABASE_URL in .env
# Ensure PostgreSQL is running
# Test connection: psql postgres://user:pass@host/db
```

**Error: "OpenAI API key invalid"**
```bash
# Verify OPENAI_API_KEY starts with 'sk-'
# Check key at https://platform.openai.com/account/api-keys
```

**Error: "Port 8080 already in use"**
```bash
# Change port in .env: SERVER_PORT=8081
# Or kill existing process: lsof -ti:8080 | xargs kill -9
```

### Frontend Issues

**Error: "Cannot find module 'lucide-react'"**
```bash
cd frontend
npm install
```

**React Router Link errors in VS Code**
```bash
# Update to latest React and React Router
npm update react react-dom react-router-dom
```

**Environment variables not loading**
```bash
# Ensure VITE_ prefix for frontend env vars
# Restart dev server: npm run dev
```

### Video Processing Issues

**Error: "FFmpeg not found"**
```bash
# Install FFmpeg
# macOS: brew install ffmpeg
# Ubuntu: sudo apt-get install ffmpeg
# Windows: Download from https://ffmpeg.org/download.html
```

**Error: "File size exceeds maximum"**
```bash
# Max file size: 100MB
# Check: MAX_FILE_SIZE_BYTES=104857600 in .env
# Compress video before upload
```

**Transcription taking too long**
```bash
# Whisper processes in real-time (5 min video ≈ 2-3 min processing)
# Larger files take proportionally longer
# Check API quota at https://platform.openai.com/account/usage
```

---

## 📊 Development Workflow

### Running Tests

**Backend**
```bash
cd backend
./gradlew test
```

**Frontend**
```bash
cd frontend
npm run lint
```

### Building for Production

**Backend**
```bash
cd backend
./gradlew clean build -DskipTests
# Creates: backend/build/libs/voxly-0.0.1-SNAPSHOT.jar
```

**Frontend**
```bash
cd frontend
npm run build
# Creates: frontend/dist/
```

### Local Docker Infrastructure

The compose file is intended for local dependencies only:

```bash
make infra-up
make infra-logs
make infra-down
```

Keep the backend/frontend running normally outside containers unless you specifically want a containerized app workflow.

---

## 🔐 Security Considerations

⚠️ **NEVER commit `.env` file!** It's already in `.gitignore`

### Before Production Deployment

- [ ] Change `JWT_SECRET_KEY` to a strong random value (min 32 chars)
- [ ] Set `JWT_ACCESS_COOKIE_SECURE=true`
- [ ] Use environment variables, not files
- [ ] Enable HTTPS/TLS
- [ ] Set up rate limiting
- [ ] Configure CORS properly
- [ ] Enable database backups
- [ ] Use Cloudflare R2 or similar for file storage
- [ ] Keep OpenAI API key private
- [ ] Monitor API costs

---

## 📈 Performance Tips

### Optimize Video Upload
- Compress videos to 50-100MB before upload
- Use H.264 codec for best compatibility
- Upload during off-peak hours to share API quota

### Database Optimization
- Indexes are created automatically on important columns
- Monitor query performance in production
- Set up database backups

### Frontend Performance
- Vite provides hot module reloading (HMR)
- Tailwind CSS is optimized for production
- Bundle size ~150KB gzipped

---

## 📚 Technology Deep Dives

### Spring Boot Architecture
- **Hexagonal (Ports & Adapters)** for clean architecture
- **Domain-Driven Design** for business logic
- **Repository Pattern** for data access
- **Event Sourcing** for session changes

### React Component Structure
- Functional components with hooks
- Context API for global state (auth)
- React Router v7 for navigation
- Composition over inheritance

### Database Schema

**Users Table**
- id (UUID primary key)
- email, username, password_hash
- created_at, updated_at

**Sessions Table**
- id (UUID)
- user_id (foreign key)
- title, description, session_type
- status, language
- media_storage_path, media_duration_seconds
- created_at, modified_at

**Transcriptions Table**
- id, session_id, user_id
- original_text, word_count, duration_seconds
- language, status
- created_at, completed_at

**Analyses Table**
- id, session_id, transcription_id
- metrics (JSONB), feedback (JSONB)
- status, error_message

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

---

## 📝 License

This project is licensed under the MIT License - see LICENSE file for details.

---

## 🆘 Support

- **Issues**: Report bugs on GitHub Issues
- **Discussions**: Ask questions in GitHub Discussions
- **Email**: support@voxly.app

---

## 🎉 Next Steps

1. ✅ **Complete**: Environment setup & configuration
2. ✅ **Complete**: Multi-language support (EN, ES)
3. ⏳ **In Progress**: Advanced speech analysis metrics
4. ⏳ **Planned**: UI improvements & animations
5. ⏳ **Planned**: Mobile app (React Native)
6. ⏳ **Planned**: Pricing tiers & monetization

---

**Built with ❤️ by the VoxLy team**

*Transform your presentations, one session at a time.*
