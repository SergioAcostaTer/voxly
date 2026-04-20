# Daily Checklist for Developers

**How to effectively contribute to Voxly using the LLMs documentation**

---

## 🌅 Morning Check-in (5 minutes)

- [ ] Read [STATUS_DASHBOARD.md](STATUS_DASHBOARD.md) to understand current status
- [ ] Check [progress.md](progress.md) to see what others are working on
- [ ] Identify your task from [backlog.md](backlog.md) or ask your team lead
- [ ] Review any blockers from [issues-and-fixes.md](issues-and-fixes.md)

---

## 🔄 During Development (As You Work)

### Before You Start
- [ ] Read your feature story in [backlog.md](backlog.md) completely
- [ ] Check for dependencies listed in the story
- [ ] Review [ai-instructions.md](ai-instructions.md) for coding standards
- [ ] Look at similar implementations in [completed-features.md](completed-features.md)

### As You Code
- [ ] Follow Spring Boot conventions (backend)
- [ ] Follow React TypeScript patterns (frontend)
- [ ] Check error handling in [ai-instructions.md](ai-instructions.md)
- [ ] Add tests as you go

### When You Hit Issues
- [ ] Check [issues-and-fixes.md](issues-and-fixes.md) for solutions
- [ ] If blocked on T3: Note in [progress.md](progress.md) and move to alternate task
- [ ] Document any new issues you find

---

## ✅ When You Complete Your Work (Next Day)

### 1. Update Documentation (Important!)

**Edit [backlog.md](backlog.md):**
```
Change:   ⏳ TODO  →  🔄 IN_PROGRESS  (when starting)
Change:   🔄 IN_PROGRESS  →  ✅ DONE  (when finishing)
Add:      Status: Complete ✅
Add link: Backend: <file path>
Add link: Frontend: <file path> (if applicable)
```

**Example:**
```markdown
### US.1.1 - User Registration ✅ DONE
**Status:** COMPLETE ✅
- ✅ Backend: [api/identity/AuthController.java]()
- ✅ Frontend: [pages/AuthPage.tsx]()
```

**Edit [completed-features.md](completed-features.md):**
```markdown
#### US.X.X - Your Feature ✅
**Status:** DONE  
**Completion Date:** 2026-04-20  
**Story Points:** X

**Deliverables:**
- ✅ Backend: [path/to/file]()
- ✅ Frontend: [path/to/file]()
- ✅ Tests: Implemented

**Files:**
- [Backend link]()
- [Frontend link]()
```

**Edit [progress.md](progress.md):**
```
Update sprint statistics at top:
| Total Story Points Planned | XX |
| Completed | XX (+Y from your work) |
| In Progress | XX (-Y) |
| Todo | XX (-Y) |
```

### 2. Create Pull Request

Format:
```
Title: feat: implement US.X.X - Feature Name

Body:
## Feature
- US.X.X - [Link to backlog.md]()

## Changes
- [Backend] Implemented AuthController.register()
- [Frontend] Created RegistrationForm component
- [Tests] Added 5 test cases

## How to Test
1. Navigate to /register
2. Fill in email, password, name
3. Click submit
4. Verify user created in database

## Checklist
- [x] Follows coding standards (see ai-instructions.md)
- [x] Tests pass (>70% coverage)
- [x] No console errors
- [x] Updated LLMs documentation
- [x] Linked to backlog
```

### 3. Update Status Files

**[STATUS_DASHBOARD.md](STATUS_DASHBOARD.md):**
- Update % progress
- Update story points completed
- Move task from "In Progress" to "What's Ready to Go"

**[progress.md](progress.md):**
- Update team assignments
- Move completed items to past work
- Pick next task for team member

---

## 📋 Task-by-Task Guides

### If You're Working on Backend (Spring Boot)

1. Check dependencies in [backlog.md](backlog.md)
2. Review similar services in [completed-features.md](completed-features.md)
3. Follow patterns from `AuthService`, `UserService`, etc.
4. Add DTOs in appropriate package
5. Create controller endpoint
6. Implement service logic
7. Add repository if needed
8. Write tests
9. Document in swagger/OpenAPI

**Common Locations:**
- Controllers: `api/<feature>/`
- Services: `application/<feature>/`
- Entities: `domain/<feature>/`
- Repositories: `infrastructure/<feature>/`

### If You're Working on Frontend (React)

1. Check requirements in [backlog.md](backlog.md)
2. Look at similar components in [completed-features.md](completed-features.md)
3. Start with page component
4. Add form/UI components
5. Connect to API using `lib/api.ts`
6. Handle loading and error states
7. Add TypeScript types from `types/`
8. Style with Tailwind CSS
9. Test on mobile and desktop

**Common Locations:**
- Pages: `pages/`
- Components: `ui/`
- API client: `lib/api.ts`
- Types: `types/`
- Auth: `auth/`

### If You're Working on T3 (External Services) 🔴 CRITICAL

1. Read [issues-and-fixes.md](issues-and-fixes.md) section 2, 3, 4
2. Choose service to implement first (suggested order):
   - Cloud Storage (enables US.11.1, US.13.1)
   - ChatGPT API (enables US.18.1, US.26.1, US.24.1)
   - OAuth2 (enables US.8.1)
3. Create integration test first
4. Implement service client
5. Connect to existing endpoints
6. Document configuration in `application.yml`
7. Add error handling
8. Test with sample data

**Impact:** Completing T3 unblocks 40% of remaining work!

---

## 🐛 If You Find a Bug

1. **Reproduce it** - Understand exactly when it happens
2. **Document in [issues-and-fixes.md](issues-and-fixes.md)**
   ```markdown
   **Issue:** Bug title
   **Severity:** [Critical/High/Medium/Low]
   **Component:** [Frontend/Backend/Database]
   **Description:** [What's wrong]
   **Impact:** [What breaks]
   **Suggested Fix:** [How to fix it]
   ```
3. **Update [backlog.md](backlog.md)** - Create bug story
4. **Communicate with team** - Discuss priority
5. **Fix it** - Or assign to someone

---

## 💭 If You Have Improvement Ideas

1. **Document in [issues-and-fixes.md](issues-and-fixes.md)** under "Enhancements"
   ```markdown
   **Type:** Enhancement
   **Priority:** [HIGH/MEDIUM/LOW]
   **Description:** [What improvement]
   **Benefits:** [Why it matters]
   **Effort:** [Story points estimate]
   ```
2. **Discuss with team lead**
3. **Add to [backlog.md](backlog.md)** if approved
4. **Schedule in [roadmap.md](roadmap.md)** if needed

---

## 🎓 Code Review Checklist

When reviewing someone's pull request, check:

- [ ] Follows [ai-instructions.md](ai-instructions.md) standards
- [ ] Tests included (70%+ coverage)
- [ ] LLMs docs updated ([backlog.md](backlog.md), [completed-features.md](completed-features.md))
- [ ] No console errors/warnings
- [ ] Error handling appropriate
- [ ] Performance acceptable
- [ ] Security considerations addressed
- [ ] Naming conventions followed
- [ ] Code is readable and maintainable
- [ ] Tests pass locally

---

## 📊 Weekly Sync Checklist

**Every Monday (or at sprint planning):**

- [ ] Review [STATUS_DASHBOARD.md](STATUS_DASHBOARD.md)
- [ ] Update [progress.md](progress.md) with new sprint planning
- [ ] Prioritize [backlog.md](backlog.md) for new stories
- [ ] Check blockers in [issues-and-fixes.md](issues-and-fixes.md)
- [ ] Update [roadmap.md](roadmap.md) if timeline changed
- [ ] Create burndown chart in [progress.md](progress.md)
- [ ] Assign tasks for next week

---

## 🚀 Before Release

**Before deploying to production:**

- [ ] All features marked ✅ DONE
- [ ] All tests passing
- [ ] [STATUS_DASHBOARD.md](STATUS_DASHBOARD.md) shows 100% for current phase
- [ ] [completed-features.md](completed-features.md) updated
- [ ] [issues-and-fixes.md](issues-and-fixes.md) resolved or documented
- [ ] Performance tested
- [ ] Security reviewed
- [ ] Documentation complete
- [ ] [roadmap.md](roadmap.md) milestone updated

---

## 📞 Quick Reference

| Need | File | Section |
|------|------|---------|
| What to do today? | [STATUS_DASHBOARD.md](STATUS_DASHBOARD.md) | Next 7 Days |
| How to code? | [ai-instructions.md](ai-instructions.md) | Development Standards |
| What task? | [backlog.md](backlog.md) | Look for ⏳ TODO |
| Is this done? | [completed-features.md](completed-features.md) | Check if listed |
| Is something blocking me? | [issues-and-fixes.md](issues-and-fixes.md) | Known Blockers |
| When will it launch? | [roadmap.md](roadmap.md) | Timeline |
| What's my progress? | [progress.md](progress.md) | Sprint Status |

---

## ✨ Pro Tips

1. **Update docs first** - Changes to backlog before you start code
2. **Small commits** - One feature = one commit
3. **Test early** - Write tests as you code
4. **Document as you go** - Don't leave it for the end
5. **Check dependencies** - See what tasks are blocked on yours
6. **Communicate blockers** - Don't wait, tell team immediately
7. **Celebrate wins** - Update STATUS_DASHBOARD when done!

---

## 🎯 Success Metrics

You're doing great when:
- ✅ Tasks moved to ✅ DONE regularly
- ✅ Documentation always in sync with code
- ✅ PRs reference backlog items
- ✅ No surprise bugs at sprint end
- ✅ Timeline stays on track
- ✅ Team knows what you're working on

---

**Last Updated:** 2026-04-20  
**Remember:** Updating documentation is just as important as writing code!

**Let's build Voxly! 🚀**
