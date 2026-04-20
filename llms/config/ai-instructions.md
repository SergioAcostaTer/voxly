# AI Developer Instructions

**For LLM/AI Agents Developing Voxly**

## 🎯 Project Context

**Voxly** is an AI-powered presentation practice platform that helps users improve their public speaking skills through:
- Real-time presentation recording and analysis
- Automated transcription with timestamps
- AI-powered feedback generation
- Progress tracking and metrics
- Session history and comparisons

## 📋 Development Standards

### Code Quality
- Follow Spring Boot conventions for backend
- Use TypeScript strict mode for frontend
- Write meaningful commit messages
- Include JSDoc/JavaDoc for public methods
- Add unit tests for new features

### Naming Conventions
- Backend: `CamelCase` for classes, `camelCase` for methods/variables
- Frontend: `PascalCase` for components, `camelCase` for functions/variables
- Database: `snake_case` for columns and tables
- Git branches: `feature/feature-name`, `bugfix/bug-name`, `hotfix/issue-name`

### API Standards
- RESTful endpoints following standard conventions
- Consistent error responses with proper HTTP status codes
- Input validation on all endpoints
- Authentication checks on protected routes
- CORS properly configured

## 🔧 Tech Stack Reference

### Backend
- **Framework:** Spring Boot
- **Build Tool:** Gradle
- **Language:** Java
- **Key Dependencies:**
  - Spring Web (REST API)
  - Spring Data JPA (Database)
  - Spring Security (Authentication/Authorization)
  - JWT for token-based auth

### Frontend
- **Framework:** React with TypeScript
- **Build Tool:** Vite
- **Styling:** Tailwind CSS
- **Package Manager:** npm

### Infrastructure
- **Database:** PostgreSQL
- **Containerization:** Docker
- **External Services:**
  - ChatGPT API (for analysis and feedback)
  - Cloud Storage (for video/slide uploads)
  - Email Service (for notifications)
  - OAuth Providers (Google, Microsoft)

## 📝 When Adding Features

### 1. Analysis Phase
- [ ] Read the user story from `backlog.md`
- [ ] Check `roadmap.md` for dependencies
- [ ] Review `issues-and-fixes.md` for related concerns
- [ ] Identify backend vs frontend work

### 2. Implementation Phase
- [ ] Create feature branch from `develop`
- [ ] Implement backend endpoints/logic
- [ ] Implement frontend UI/components
- [ ] Add tests (minimum 70% coverage)
- [ ] Update API documentation

### 3. Testing Phase
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
- [ ] No console errors/warnings
- [ ] Performance acceptable

### 4. Completion Phase
- [ ] Create Pull Request with description
- [ ] Commit message follows conventions
- [ ] Move feature to `completed-features.md`
- [ ] Update `progress.md`

## 🐛 When Finding Issues

### Report Format
```markdown
**Issue:** [Title]
**Severity:** [Critical/High/Medium/Low]
**Component:** [Frontend/Backend/Database/Infrastructure]
**Description:** [What's wrong]
**Impact:** [What breaks]
**Suggested Fix:** [How to fix it]
**File:** [Affected file]
**Line:** [Line number, if applicable]
```

### Add to `issues-and-fixes.md` with:
- Clear description
- Steps to reproduce (if it's a bug)
- Expected vs actual behavior
- Suggested solution
- Priority level

## 📊 Status Labels

Use these labels when updating files:

| Label | Meaning |
|-------|---------|
| ✅ DONE | Completed and tested |
| 🔄 IN PROGRESS | Currently being developed |
| ⏳ TODO | Not started |
| 🚧 IN REVIEW | Awaiting review/approval |
| ❌ BLOCKED | Cannot proceed (document reason) |
| 🔧 NEEDS FIXES | Completed but has issues |

## 🔗 File References

- **Backend Main:** `src/main/java/com/pigs/voxly/`
- **Frontend Main:** `frontend/src/`
- **Tests Backend:** `src/test/java/com/pigs/voxly/`
- **Database Config:** `src/main/resources/application.yml`
- **API Docs:** Generated from OpenApiConfig.java

## 🚀 Deployment Checklist

- [ ] All tests passing
- [ ] No console errors
- [ ] Environment variables configured
- [ ] Database migrations run
- [ ] API endpoints responding
- [ ] Frontend builds without errors
- [ ] Docker image builds successfully
- [ ] Documentation updated

## 💡 Pro Tips

1. **Before starting:** Run `./gradlew clean build` to ensure project builds
2. **For frontend development:** Use `npm run dev` for hot reload
3. **For backend development:** Use Spring Boot DevTools for faster feedback
4. **Database issues:** Check `docker-compose.yml` for database setup
5. **API testing:** Use Postman or similar tool with endpoints from OpenApiConfig

## 📞 Getting Help

- Check `backlog.md` for feature requirements
- Review `issues-and-fixes.md` for common problems
- Look at test files for usage examples
- Check git history for similar implementations

## 🎓 Learning Resources

- Review existing code in similar modules
- Check test cases for implementation patterns
- Read user stories in `docs/USE_CASES_AND_USER_STORIES.md`
- Consult `roadmap.md` for architectural decisions

---

**Remember:** Always update the tracking files after making changes!

**Last Updated:** 2026-04-20
