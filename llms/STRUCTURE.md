# VoxLy LLMs Folder Structure

```
llms/
│
├── 📋 README_NEW.md                    ← START HERE! Complete guide to this folder
├── STATUS_CURRENT.md                   ← Current project status dashboard
│
├── 🔧 config/                          [STATIC GUIDELINES]
│   ├── README.md
│   ├── ai-instructions.md              ← Coding standards & patterns
│   └── DEVELOPER_CHECKLIST.md          ← Daily workflow & checklist
│
├── 📋 planning/                        [STRATEGIC PLANNING]
│   ├── README.md
│   ├── STATUS_CURRENT.md               ← Project status & metrics
│   ├── backlog.md                      ← 43 features, 145+ points
│   ├── backlog.json                    ← Machine-readable backlog
│   └── roadmap.md                      ← 9-phase development timeline
│
├── 📜 history/                         [TRACKING & HISTORY]
│   ├── README.md
│   ├── PROGRESS_2026_04_20.md          ← Latest sprint progress
│   ├── completed-features.md           ← Features completed with details
│   ├── issues-and-fixes.md             ← Known issues & solutions
│   ├── SYNC_REPORT_2026_04_20.md       ← Implementation sync report
│   └── SYNC_SESSION_SUMMARY.md         ← Development session logs
│
└── [Legacy files - can be archived]
    ├── README.md                       (superseded by README_NEW.md)
    ├── STATUS_DASHBOARD.md             (superseded by STATUS_CURRENT.md)
    ├── progress.md                     (superseded by PROGRESS_2026_04_20.md)
    └── ...
```

---

## 📂 Quick Reference

### 🟢 Start Here (First Time)
```
llms/
└── README_NEW.md                       ← You are here!
    │
    ├─→ planning/STATUS_CURRENT.md     (What's done?)
    ├─→ config/DEVELOPER_CHECKLIST.md  (Daily workflow)
    ├─→ planning/backlog.md             (What to work on?)
    └─→ config/ai-instructions.md       (How to code?)
```

### 🔵 Developers Should Know
```
config/
├── ai-instructions.md                 (Coding standards)
└── DEVELOPER_CHECKLIST.md             (Daily tasks)

planning/
└── backlog.md                         (Pick your task)

history/
└── completed-features.md              (Reference examples)
```

### 🟠 Project Managers Should Know
```
planning/
├── STATUS_CURRENT.md                  (Metrics)
├── backlog.md                         (Feature list)
└── roadmap.md                         (Timeline)

history/
└── PROGRESS_2026_04_20.md             (Sprint stats)
```

### 🟣 When You Need Help
```
history/
└── issues-and-fixes.md                (Troubleshooting)

planning/
└── STATUS_CURRENT.md                  (Overview)

config/
└── ai-instructions.md                 (Standards)
```

---

## 📊 File Purposes

| File | Location | Purpose | Update Frequency |
|------|----------|---------|------------------|
| **README_NEW.md** | Root | Guide to this folder | Monthly |
| **STATUS_CURRENT.md** | planning/ | Live project status | Weekly |
| **backlog.md** | planning/ | Feature list | Daily (as work progresses) |
| **backlog.json** | planning/ | Machine-readable backlog | Daily |
| **roadmap.md** | planning/ | Phase timeline | Rarely (set at start) |
| **ai-instructions.md** | config/ | Coding standards | Rarely (stable) |
| **DEVELOPER_CHECKLIST.md** | config/ | Daily workflow | Rarely (stable) |
| **PROGRESS_2026_04_20.md** | history/ | Sprint metrics | Weekly |
| **completed-features.md** | history/ | Feature log | When features complete |
| **issues-and-fixes.md** | history/ | Bug tracking | When issues occur |
| **SYNC_REPORT_2026_04_20.md** | history/ | Implementation status | After major milestones |
| **SYNC_SESSION_SUMMARY.md** | history/ | Development logs | Per session |

---

## 🎯 How to Use Each Folder

### `/config/` - Stable Reference
**Purpose:** Guidance that stays consistent  
**Audience:** All developers  
**Update:** Rarely change (maybe quarterly)  

```
When to read:
✓ Starting a new feature
✓ Writing code
✓ Stuck on approach
✓ Need coding standards

When to update:
✗ Rarely - these are guidelines
✓ When significant pattern discovered
```

### `/planning/` - Strategic Direction
**Purpose:** What we're building and timeline  
**Audience:** PMs, developers picking tasks  
**Update:** Regularly (weekly to daily)  

```
When to read:
✓ Understanding project status
✓ Picking a task
✓ Understanding phases
✓ Check timeline

When to update:
✓ When feature status changes
✓ When work completed
✓ When priorities shift
```

### `/history/` - Record Keeping
**Purpose:** Track what's been done and issues  
**Audience:** PMs, debugging, reference  
**Update:** Regularly (per sprint/session)  

```
When to read:
✓ Troubleshooting problems
✓ Learning from past
✓ Checking what's been done
✓ Understanding decisions

When to update:
✓ When feature is completed
✓ When bug is discovered
✓ End of sprint
✓ After major milestone
```

---

## 🔄 Typical Workflow

### Developer's Daily Flow
```
1. Morning: Read planning/STATUS_CURRENT.md
          Check config/DEVELOPER_CHECKLIST.md
          
2. Work:   Pick task from planning/backlog.md
           Reference config/ai-instructions.md while coding
           Check history/completed-features.md for examples
           
3. If stuck: Check history/issues-and-fixes.md
           
4. When done: Update planning/backlog.md status
             Update history/completed-features.md
             Update planning/STATUS_CURRENT.md
```

### Project Manager's Weekly Flow
```
1. Check: planning/STATUS_CURRENT.md → metrics
         history/PROGRESS_2026_04_20.md → sprint stats
         
2. Monitor: planning/backlog.md → status changes
           
3. Assess: planning/roadmap.md → on track?
          history/issues-and-fixes.md → blockers?
          
4. Update: history/PROGRESS_2026_04_20.md
```

---

## 💡 Key Principles

### 1. **Easy to Find**
- Main guide: `README_NEW.md`
- Status: `planning/STATUS_CURRENT.md`
- Current work: `planning/backlog.md`
- Problems: `history/issues-and-fixes.md`

### 2. **Easy to Update**
- Developer updates `backlog.md` as work progresses
- At sprint end, update progress metrics
- When feature done, add to `completed-features.md`

### 3. **Easy to Reference**
- Similar features in `completed-features.md`
- Questions answered in `issues-and-fixes.md`
- Standards in `config/ai-instructions.md`

### 4. **Easy to Understand**
- Folders grouped by function (planning, config, history)
- READMEs in each folder explain purpose
- Quick reference tables provided

---

## 🆕 What's New in This Reorganization

✨ **Changes Made (April 20, 2026):**
- ✅ Created 3 logical subdirectories
- ✅ Created README files for each
- ✅ Created `STATUS_CURRENT.md` dashboard
- ✅ Updated progress with latest metrics
- ✅ Added this structure guide

✨ **Benefits:**
- Clearer organization
- Easier to find what you need
- Better separation of concerns
- Scalable for future documents

---

## 🎯 File Quick Links

**I need to...**
- Know current status → [`planning/STATUS_CURRENT.md`](planning/STATUS_CURRENT.md)
- Pick a task → [`planning/backlog.md`](planning/backlog.md)
- Learn to code here → [`config/ai-instructions.md`](config/ai-instructions.md)
- Check what's done → [`history/completed-features.md`](history/completed-features.md)
- Troubleshoot issue → [`history/issues-and-fixes.md`](history/issues-and-fixes.md)
- Understand timeline → [`planning/roadmap.md`](planning/roadmap.md)
- See sprint stats → [`history/PROGRESS_2026_04_20.md`](history/PROGRESS_2026_04_20.md)
- Understand workflow → [`config/DEVELOPER_CHECKLIST.md`](config/DEVELOPER_CHECKLIST.md)

---

**Last Updated:** April 20, 2026  
**Status:** ✅ Reorganized & Updated  
**Next Review:** April 27, 2026
