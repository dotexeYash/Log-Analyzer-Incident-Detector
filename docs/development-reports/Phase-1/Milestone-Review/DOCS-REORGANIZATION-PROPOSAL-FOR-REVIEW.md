# Project Documentation Reorganization Proposal — Phase 1 Development Reports

## 1. Context & Objective
The Java 21 project **`Log-Analyzer-Incident-Detector`** has completed Phase 1 development (Tasks 1 through 6). Over the course of these tasks, multiple investigation, design, implementation, and code review reports were generated directly in the root directory.

To keep the repository clean, professional, and maintainable, we are organizing all task-related reports into a structured `docs/` hierarchy without disturbing core application code, Maven build scripts, or top-level project metadata.

---

## 2. Current Project Root State
Currently, the root folder contains **20 task reports (`TASK-*.md`)**, **1 plain-text diff (`.txt`)**, **1 CLI code export (`.md`)**, plus root-level project files:

```
Log-Analyzer-Incident-Detector/
├── .gitignore                                         [Repository configuration - DO NOT MOVE]
├── pom.xml                                            [Maven build configuration - DO NOT MOVE]
├── README.md                                          [Project documentation entrypoint - DO NOT MOVE]
├── burst-test.log                                     [Sample test log - DO NOT MOVE]
├── security-report.json                               [Sample run output - DO NOT MOVE]
├── security-report.md                                 [Sample run output - DO NOT MOVE]
│
├── CLI-PARSER-CODE.md                                 [Task 3 reference snippet]
├── TASK-1-SECURITY-FIX-REPORT.md                      [Task 1 report]
├── TASK-2-ERROR-BURST-BUG-ANALYSIS.md                 [Task 2 report]
├── TASK-2-FRACTIONAL-BOUNDARY-FIX.md                  [Task 2 report]
├── TASK-2-IMPLEMENTATION-REPORT.md                    [Task 2 report]
├── TASK-2-SOLUTION-B-DESIGN-REPORT.md                 [Task 2 report]
├── TASK-2-TIMESTAMP-SORTING-ANALYSIS.md               [Task 2 report]
├── TASK-3-CLI-VALIDATION-REPORT.md                    [Task 3 report]
├── TASK-3-CLI-VALIDATION-IMPLEMENTATION-REPORT.md     [Task 3 report]
├── TASK-4-DIGIT-NORMALIZATION-INVESTIGATION-REPORT.md [Task 4 report]
├── TASK-4-REVISED-DESIGN-REPORT.md                    [Task 4 report]
├── TASK-4-DIGIT-NORMALIZATION-IMPLEMENTATION-REPORT.md[Task 4 report]
├── TASK-5-TIMESTAMP-PARSING-INVESTIGATION-REPORT.md   [Task 5 report]
├── TASK-5-TIMESTAMP-PARSING-IMPLEMENTATION-REPORT.md  [Task 5 report]
├── TASK-5-POST-APPROVAL-EXECUTION-REPORT.md           [Task 5 report]
├── TASK-5-FINAL-INSPECTION-AND-IMPLEMENTATION-REPORT.md[Task 5 report]
├── TASK-5-STRICT-CALENDAR-VALIDATION-REPORT.md        [Task 5 report]
├── TASK-6-EXCEPTION-HANDLING-INVESTIGATION.md         [Task 6 report]
├── TASK-6-EXCEPTION-HANDLING-IMPLEMENTATION-REPORT.md [Task 6 report]
├── TASK-5-6-FINAL-CODE-REVIEW.md                      [Tasks 5 & 6 joint review]
├── TASK-5-6-COMPLETE-CODE-DIFF.md                     [Tasks 5 & 6 complete diff]
└── TASK-5-6-COMPLETE-CODE-DIFF.txt                    [Tasks 5 & 6 plain text diff]
```

---

## 3. Proposed Directory Structure

```text
docs/
└── development-reports/
    └── Phase-1/
        ├── Task-1/
        │   └── TASK-1-SECURITY-FIX-REPORT.md
        │
        ├── Task-2/
        │   ├── TASK-2-ERROR-BURST-BUG-ANALYSIS.md
        │   ├── TASK-2-FRACTIONAL-BOUNDARY-FIX.md
        │   ├── TASK-2-IMPLEMENTATION-REPORT.md
        │   ├── TASK-2-SOLUTION-B-DESIGN-REPORT.md
        │   └── TASK-2-TIMESTAMP-SORTING-ANALYSIS.md
        │
        ├── Task-3/
        │   ├── TASK-3-CLI-VALIDATION-REPORT.md
        │   ├── TASK-3-CLI-VALIDATION-IMPLEMENTATION-REPORT.md
        │   └── CLI-PARSER-CODE.md
        │
        ├── Task-4/
        │   ├── TASK-4-DIGIT-NORMALIZATION-INVESTIGATION-REPORT.md
        │   ├── TASK-4-REVISED-DESIGN-REPORT.md
        │   └── TASK-4-DIGIT-NORMALIZATION-IMPLEMENTATION-REPORT.md
        │
        ├── Task-5/
        │   ├── TASK-5-TIMESTAMP-PARSING-INVESTIGATION-REPORT.md
        │   ├── TASK-5-TIMESTAMP-PARSING-IMPLEMENTATION-REPORT.md
        │   ├── TASK-5-POST-APPROVAL-EXECUTION-REPORT.md
        │   ├── TASK-5-FINAL-INSPECTION-AND-IMPLEMENTATION-REPORT.md
        │   └── TASK-5-STRICT-CALENDAR-VALIDATION-REPORT.md
        │
        ├── Task-6/
        │   ├── TASK-6-EXCEPTION-HANDLING-INVESTIGATION.md
        │   └── TASK-6-EXCEPTION-HANDLING-IMPLEMENTATION-REPORT.md
        │
        ├── TASK-5-6-FINAL-CODE-REVIEW.md
        ├── TASK-5-6-COMPLETE-CODE-DIFF.md
        └── TASK-5-6-COMPLETE-CODE-DIFF.txt
```

---

## 4. Proposed Source-to-Destination File Mapping

| Source File (Current Root) | Proposed Target Path | Rationale |
| :--- | :--- | :--- |
| `TASK-1-SECURITY-FIX-REPORT.md` | `docs/development-reports/Phase-1/Task-1/TASK-1-SECURITY-FIX-REPORT.md` | Task 1 security anomaly false-positive fix |
| `TASK-2-ERROR-BURST-BUG-ANALYSIS.md` | `docs/development-reports/Phase-1/Task-2/TASK-2-ERROR-BURST-BUG-ANALYSIS.md` | Task 2 sliding window burst bug analysis |
| `TASK-2-FRACTIONAL-BOUNDARY-FIX.md` | `docs/development-reports/Phase-1/Task-2/TASK-2-FRACTIONAL-BOUNDARY-FIX.md` | Task 2 sub-second precision fix |
| `TASK-2-IMPLEMENTATION-REPORT.md` | `docs/development-reports/Phase-1/Task-2/TASK-2-IMPLEMENTATION-REPORT.md` | Task 2 sliding window implementation |
| `TASK-2-SOLUTION-B-DESIGN-REPORT.md` | `docs/development-reports/Phase-1/Task-2/TASK-2-SOLUTION-B-DESIGN-REPORT.md` | Task 2 architectural design document |
| `TASK-2-TIMESTAMP-SORTING-ANALYSIS.md`| `docs/development-reports/Phase-1/Task-2/TASK-2-TIMESTAMP-SORTING-ANALYSIS.md` | Task 2 timestamp chronological sorting analysis |
| `TASK-3-CLI-VALIDATION-REPORT.md` | `docs/development-reports/Phase-1/Task-3/TASK-3-CLI-VALIDATION-REPORT.md` | Task 3 CLI input validation design |
| `TASK-3-CLI-VALIDATION-IMPLEMENTATION-REPORT.md` | `docs/development-reports/Phase-1/Task-3/TASK-3-CLI-VALIDATION-IMPLEMENTATION-REPORT.md` | Task 3 CLI decoupled parser implementation |
| `CLI-PARSER-CODE.md` | `docs/development-reports/Phase-1/Task-3/CLI-PARSER-CODE.md` | Task 3 reference implementation snippet |
| `TASK-4-DIGIT-NORMALIZATION-INVESTIGATION-REPORT.md` | `docs/development-reports/Phase-1/Task-4/TASK-4-DIGIT-NORMALIZATION-INVESTIGATION-REPORT.md` | Task 4 regex over-generalization audit |
| `TASK-4-REVISED-DESIGN-REPORT.md` | `docs/development-reports/Phase-1/Task-4/TASK-4-REVISED-DESIGN-REPORT.md` | Task 4 refined contextual normalization design |
| `TASK-4-DIGIT-NORMALIZATION-IMPLEMENTATION-REPORT.md` | `docs/development-reports/Phase-1/Task-4/TASK-4-DIGIT-NORMALIZATION-IMPLEMENTATION-REPORT.md` | Task 4 normalization implementation & tests |
| `TASK-5-TIMESTAMP-PARSING-INVESTIGATION-REPORT.md` | `docs/development-reports/Phase-1/Task-5/TASK-5-TIMESTAMP-PARSING-INVESTIGATION-REPORT.md` | Task 5 multi-format timestamp audit |
| `TASK-5-TIMESTAMP-PARSING-IMPLEMENTATION-REPORT.md` | `docs/development-reports/Phase-1/Task-5/TASK-5-TIMESTAMP-PARSING-IMPLEMENTATION-REPORT.md` | Task 5 flexible timestamp implementation |
| `TASK-5-POST-APPROVAL-EXECUTION-REPORT.md` | `docs/development-reports/Phase-1/Task-5/TASK-5-POST-APPROVAL-EXECUTION-REPORT.md` | Task 5 post-approval test execution verification |
| `TASK-5-FINAL-INSPECTION-AND-IMPLEMENTATION-REPORT.md` | `docs/development-reports/Phase-1/Task-5/TASK-5-FINAL-INSPECTION-AND-IMPLEMENTATION-REPORT.md` | Task 5 single/double digit date fix |
| `TASK-5-STRICT-CALENDAR-VALIDATION-REPORT.md` | `docs/development-reports/Phase-1/Task-5/TASK-5-STRICT-CALENDAR-VALIDATION-REPORT.md` | Task 5 strict calendar date (`ResolverStyle.STRICT`) addendum |
| `TASK-6-EXCEPTION-HANDLING-INVESTIGATION.md` | `docs/development-reports/Phase-1/Task-6/TASK-6-EXCEPTION-HANDLING-INVESTIGATION.md` | Task 6 comprehensive exception handling audit |
| `TASK-6-EXCEPTION-HANDLING-IMPLEMENTATION-REPORT.md` | `docs/development-reports/Phase-1/Task-6/TASK-6-EXCEPTION-HANDLING-IMPLEMENTATION-REPORT.md` | Task 6 exception hardening implementation |
| `TASK-5-6-FINAL-CODE-REVIEW.md` | `docs/development-reports/Phase-1/TASK-5-6-FINAL-CODE-REVIEW.md` | Phase 1 milestone code review across Tasks 5 & 6 |
| `TASK-5-6-COMPLETE-CODE-DIFF.md` | `docs/development-reports/Phase-1/TASK-5-6-COMPLETE-CODE-DIFF.md` | Phase 1 complete unified Markdown diff |
| `TASK-5-6-COMPLETE-CODE-DIFF.txt` | `docs/development-reports/Phase-1/TASK-5-6-COMPLETE-CODE-DIFF.txt` | Phase 1 complete plain-text diff |

---

## 5. Non-Report Files Kept Untouched at Root

| File | Type | Reason to Keep at Root |
| :--- | :--- | :--- |
| `README.md` | Markdown | Standard repository entrypoint explaining the project |
| `pom.xml` | XML | Maven project descriptor required at project root |
| `.gitignore` | Git Config | Version control configuration |
| `security-report.md` | Markdown | Application output file generated from demo run on `security-attack.log` |
| `security-report.json` | JSON | Application JSON output file from demo run |
| `burst-test.log` | Log data | Test input log file |
| `src/` | Directory | Application source code and unit/integration tests |

---

## 6. Questions for Review

1. **Hierarchy & Scalability:** Does the proposed `docs/development-reports/Phase-1/Task-X/` structure follow industry standards for tracking phased development reports, especially when Phase 2 begins?
2. **Placement of Cross-Task Artifacts:** Is placing `TASK-5-6-FINAL-CODE-REVIEW.md`, `TASK-5-6-COMPLETE-CODE-DIFF.md`, and `TASK-5-6-COMPLETE-CODE-DIFF.txt` directly under `Phase-1/` optimal, or would a subfolder like `Phase-1/Milestone-Review/` or `Phase-1/Tasks-5-6/` be cleaner?
3. **Reference Snippet Handling:** Is moving `CLI-PARSER-CODE.md` into `Task-3/` appropriate given it is an extract created during Task 3?
4. **Git & Build Safety:** Since these are documentation moves, will this cause any side effects with Maven (`mvn test`) or existing source code? (Confirmed: Maven builds only depend on `src/` and `pom.xml`).
