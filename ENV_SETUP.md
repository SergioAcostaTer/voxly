# Environment Configuration Guide

## Overview

VoxLy uses separate `.env` files for backend and frontend to manage environment-specific configuration. This document explains how to set up your environment.

## Quick Start

### Backend Setup

1. **Copy the example configuration:**
   ```bash
   cd backend
   cp .env.example .env
   ```

2. **Edit `.env` with your values:**
   - Update database credentials
   - Add OpenAI API key
   - Configure Cloudflare R2 or MinIO credentials
   - Set any other required values

3. **Run the backend:**
   ```bash
   ./gradlew bootRun
   ```

The `springboot4-dotenv` library will automatically load variables from `backend/.env`.

### Frontend Setup

1. **Copy the example configuration:**
   ```bash
   cd frontend
   cp .env.example .env
   ```

2. **Edit `.env` with your values:**
   - Set `VITE_API_BASE_URL` to your backend URL (usually `http://localhost:8080` for local development)

3. **Run the frontend:**
   ```bash
   npm run dev
   ```

Vite will automatically load variables prefixed with `VITE_`.

## Environment Variables Reference

### Backend (.env)

#### Database
- `DATABASE_URL` - PostgreSQL connection string
- `DATABASE_USERNAME` - Database username
- `DATABASE_PASSWORD` - Database password
- `DATABASE_DRIVER` - JDBC driver class (default: PostgreSQL)

#### Authentication
- `JWT_SECRET_KEY` - Secret key for JWT signing (min 32 characters)
- `JWT_ISSUER` - JWT issuer name
- `JWT_AUDIENCE` - JWT audience
- `AUTH_DEV_BYPASS_ENABLED` - Enable dev auth bypass (false for production)
- `AUTH_DEV_EMAIL` - Dev user email (only if bypass enabled)
- `AUTH_DEV_PASSWORD` - Dev user password (only if bypass enabled)

#### Cloud Storage (Cloudflare R2 or MinIO)
- `CLOUDFLARE_R2_ENDPOINT` - S3 endpoint URL
- `CLOUDFLARE_R2_REGION` - AWS region (use "auto" for R2)
- `CLOUDFLARE_R2_ACCESS_KEY_ID` - Access key ID
- `CLOUDFLARE_R2_ACCESS_KEY_SECRET` - Access key secret
- `CLOUDFLARE_R2_BUCKET_NAME` - S3 bucket name
- `CLOUDFLARE_R2_PUBLIC_URL` - Public URL for stored files

#### OpenAI API
- `OPENAI_API_KEY` - OpenAI API key (required for transcription)
- `OPENAI_WHISPER_MODEL` - Whisper model version
- `OPENAI_GPT_MODEL` - GPT model version
- `OPENAI_SUPPORTED_LANGUAGES` - Comma-separated language codes
- `OPENAI_DEFAULT_LANGUAGE` - Default language for transcription

#### File Processing
- `STORAGE_TEMP_DIR` - Temporary directory for file processing
- `MAX_FILE_SIZE_BYTES` - Maximum upload size (default: 100MB)
- `FFMPEG_PATH` - Path to FFmpeg executable
- `FFMPEG_AUDIO_FORMAT` - Output audio format (wav, mp3, etc.)
- `FFMPEG_AUDIO_BITRATE` - Audio bitrate
- `FFMPEG_AUDIO_SAMPLERATE` - Audio sample rate

#### Email Configuration
- `SMTP_HOST` - SMTP server hostname
- `SMTP_PORT` - SMTP port
- `SMTP_USERNAME` - SMTP username
- `SMTP_PASSWORD` - SMTP password
- `MAIL_AUTH_ENABLED` - Enable SMTP authentication
- `MAIL_STARTTLS_ENABLED` - Enable STARTTLS
- `SMTP_FROM_EMAIL` - "From" email address
- `ADMIN_EMAIL` - Admin email address

#### Application
- `SERVER_PORT` - Server port (default: 8080)
- `APP_BASE_URL` - Frontend URL (for email links)

### Frontend (.env)

All frontend environment variables must be prefixed with `VITE_` to be accessible in the app.

#### API Configuration
- `VITE_API_BASE_URL` - Backend API base URL (required)
- `VITE_AUTH_BASE_PATH` - Auth endpoint path (default: /v1/auth)
- `VITE_USERS_BASE_PATH` - Users endpoint path (default: /v1/users)

## Environment-Specific Configuration

### Development

**Backend (.env):**
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/voxly
OPENAI_API_KEY=sk-your_dev_key
AUTH_DEV_BYPASS_ENABLED=true
AUTH_DEV_EMAIL=dev@example.com
AUTH_DEV_PASSWORD=devpassword
CLOUDFLARE_R2_ENDPOINT=http://localhost:9000  # MinIO
```

**Frontend (.env):**
```bash
VITE_API_BASE_URL=http://localhost:8080
```

### Production

**Backend (.env):**
```bash
DATABASE_URL=jdbc:postgresql://production-db-host/voxly
OPENAI_API_KEY=sk-your_production_key
AUTH_DEV_BYPASS_ENABLED=false  # CRITICAL: Disable dev bypass
CLOUDFLARE_R2_ENDPOINT=https://your-account.r2.cloudflarestorage.com
CLOUDFLARE_R2_REGION=auto
JWT_SECRET_KEY=<generate-strong-secret>
```

**Frontend (.env):**
```bash
VITE_API_BASE_URL=https://api.yourdomain.com
```

## Security Best Practices

1. **Never commit `.env` files** - The `.gitignore` already excludes them
2. **Use strong secrets** - Generate cryptographically secure secrets for `JWT_SECRET_KEY`
3. **Rotate credentials regularly** - Especially API keys and database passwords
4. **Use `.env.example`** - Keep example files committed for documentation
5. **Restrict file permissions** - `.env` files contain sensitive data
6. **Use environment variables in CI/CD** - Don't store secrets in build configs

## Troubleshooting

### Backend not loading .env variables

1. Verify the `.env` file exists in the `backend/` directory
2. Check that file paths and values are correct
3. Ensure you're running `./gradlew bootRun` from the `backend/` directory
4. Check Spring Boot logs for "DotEnv" loading messages
5. Verify `springboot4-dotenv` is in the build.gradle dependencies

### Frontend not accessing environment variables

1. Variables must be prefixed with `VITE_`
2. Restart the dev server after changing `.env` values
3. Access variables via `import.meta.env.VITE_*`
4. Variables are embedded at build time (not runtime)

### Missing required variables

Backend will use defaults from `application.yml` if env vars are not set:
```yaml
database:
  url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/voxly}
```

The part after the colon is the default value.

## Next Steps

1. Copy both `.env.example` files to `.env`
2. Update with your configuration
3. Test the application:
   - Backend: `cd backend && ./gradlew bootRun`
   - Frontend: `cd frontend && npm run dev`
4. Verify all services start without errors
