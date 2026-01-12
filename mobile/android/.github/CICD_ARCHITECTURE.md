# CI/CD Pipeline Architecture

Visual overview of the FocusMother Android CI/CD pipeline.

---

## Pipeline Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         DEVELOPER WORKFLOW                          │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
              Feature Branch                 Main Branch
                    │                             │
                    ▼                             ▼
         ┌─────────────────────┐      ┌─────────────────────┐
         │   Create PR         │      │   Direct Push       │
         └─────────────────────┘      └─────────────────────┘
                    │                             │
                    └──────────────┬──────────────┘
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        ANDROID CI WORKFLOW                          │
│                       (android-ci.yml)                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐      │
│  │  Test Job      │  │ Security Scan  │  │ Build Quality  │      │
│  ├────────────────┤  ├────────────────┤  ├────────────────┤      │
│  │ • Setup JDK 17 │  │ • Setup JDK 17 │  │ • Setup JDK 17 │      │
│  │ • Gradle Cache │  │ • Gradle Cache │  │ • Gradle Cache │      │
│  │ • Run Lint     │  │ • OWASP Check  │  │ • ktlint       │      │
│  │ • Unit Tests   │  │ • TruffleHog   │  │ • detekt       │      │
│  │ • Coverage     │  │ • Secret Scan  │  │ • APK Size     │      │
│  │ • Debug APK    │  │ • Config Check │  │ • TODO Check   │      │
│  └────────────────┘  └────────────────┘  └────────────────┘      │
│         │                     │                    │               │
│         └─────────────────────┴────────────────────┘               │
│                               ▼                                    │
│                      ┌─────────────────┐                          │
│                      │   CI Summary    │                          │
│                      │  (All Jobs)     │                          │
│                      └─────────────────┘                          │
│                               │                                    │
└───────────────────────────────┼────────────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    │   All Checks Pass?    │
                    └───────────┬───────────┘
                                │ YES
                                ▼
                    ┌─────────────────────┐
                    │  Merge to Main      │
                    │  or Create Tag      │
                    └─────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    │                       │
              Manual Trigger          Git Tag (v*.*.*)
                    │                       │
                    └───────────┬───────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     ANDROID RELEASE WORKFLOW                        │
│                     (android-release.yml)                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ PHASE 1: Validate                                           │  │
│  ├─────────────────────────────────────────────────────────────┤  │
│  │ • Extract version from tag or input                        │  │
│  │ • Setup JDK 17                                              │  │
│  │ • Run full test suite                                       │  │
│  │ • Run lint checks                                           │  │
│  │ • Upload validation results                                 │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                               ▼                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ PHASE 2: Build                                              │  │
│  ├─────────────────────────────────────────────────────────────┤  │
│  │ • Decode keystore from GitHub Secrets                       │  │
│  │ • Create keystore.properties                                │  │
│  │ • Update version in build.gradle.kts                        │  │
│  │ • Build signed APK (assembleRelease)                        │  │
│  │ • Build signed AAB (bundleRelease)                          │  │
│  │ • Verify APK signature                                      │  │
│  │ • Rename artifacts with version                             │  │
│  │ • Upload APK, AAB, ProGuard mappings                        │  │
│  │ • Clean up keystore files (security)                        │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                               ▼                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ PHASE 3: Release                                            │  │
│  ├─────────────────────────────────────────────────────────────┤  │
│  │ • Download all artifacts                                    │  │
│  │ • Generate changelog from git commits                       │  │
│  │ • Create GitHub Release                                     │  │
│  │ • Attach APK, AAB, mapping.txt                              │  │
│  │ • Tag release with version                                  │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                               ▼                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ PHASE 4: Deploy (Optional)                                  │  │
│  ├─────────────────────────────────────────────────────────────┤  │
│  │ • Upload AAB to Google Play Internal Testing               │  │
│  │ • Upload ProGuard mappings                                  │  │
│  │ • Set release notes                                         │  │
│  │ • Configure rollout percentage                              │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                               ▼                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ PHASE 5: Health Check                                       │  │
│  ├─────────────────────────────────────────────────────────────┤  │
│  │ • Post-release validation summary                           │  │
│  │ • Trigger health-check workflow                             │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                     │
└───────────────────────────────┬─────────────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      HEALTH CHECK WORKFLOW                          │
│                      (health-check.yml)                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐      │
│  │  Smoke Test    │  │ APK Validation │  │  Dependency    │      │
│  ├────────────────┤  ├────────────────┤  ├────────────────┤      │
│  │ • Run critical │  │ • Download APK │  │ • Check for    │      │
│  │   smoke tests  │  │ • Verify sign  │  │   outdated     │      │
│  │ • Integration  │  │ • Check size   │  │   deps         │      │
│  │   tests        │  │ • Security     │  │ • OWASP scan   │      │
│  │ • Upload       │  │   scan         │  │ • Upload       │      │
│  │   results      │  │ • Validate     │  │   reports      │      │
│  └────────────────┘  └────────────────┘  └────────────────┘      │
│         │                     │                    │               │
│  ┌────────────────┐           │                    │               │
│  │  Performance   │           │                    │               │
│  ├────────────────┤           │                    │               │
│  │ • Method count │           │                    │               │
│  │ • Memory leaks │           │                    │               │
│  │ • Resource     │           │                    │               │
│  │   usage        │           │                    │               │
│  └────────────────┘           │                    │               │
│         │                     │                    │               │
│         └─────────────────────┴────────────────────┘               │
│                               ▼                                    │
│                      ┌─────────────────┐                          │
│                      │ Health Summary  │                          │
│                      ├─────────────────┤                          │
│                      │ • Aggregate     │                          │
│                      │   results       │                          │
│                      │ • Health report │                          │
│                      │ • Rollback      │                          │
│                      │   triggers      │                          │
│                      │ • Slack notify  │                          │
│                      └─────────────────┘                          │
│                               │                                    │
└───────────────────────────────┼────────────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    │   Health Check Pass?  │
                    └───────────┬───────────┘
                                │
                    ┌───────────┴───────────┐
                    │ YES                   │ NO
                    ▼                       ▼
         ┌────────────────────┐  ┌────────────────────┐
         │ Deploy to Beta/    │  │ ROLLBACK           │
         │ Production         │  │ RECOMMENDED        │
         └────────────────────┘  └────────────────────┘
```

---

## Trigger Conditions

### Android CI Workflow
```yaml
on:
  push:
    branches: [ main, master, develop ]
  pull_request:
    branches: [ main, master, develop ]
```

**When it runs:**
- Every push to main/master/develop
- Every pull request creation/update
- Can be manually triggered

### Android Release Workflow
```yaml
on:
  workflow_dispatch:  # Manual trigger with inputs
  push:
    tags:
      - 'v*.*.*'      # Version tags (v1.0.0, v2.1.3, etc.)
```

**When it runs:**
- Git tags matching pattern `v*.*.*`
- Manual trigger with version inputs
- Never automatic on push/PR

### Health Check Workflow
```yaml
on:
  workflow_dispatch:  # Manual trigger
  schedule:
    - cron: '0 */6 * * *'  # Every 6 hours
  workflow_run:
    workflows: ["Android Release"]
    types: [completed]     # After release completes
```

**When it runs:**
- Automatically after release workflow
- Every 6 hours (scheduled)
- Manual trigger anytime

---

## Artifact Flow

```
┌──────────────────────────────────────────────────────────────┐
│                    ARTIFACT LIFECYCLE                        │
└──────────────────────────────────────────────────────────────┘

CI Workflow Artifacts (14 days retention):
┌─────────────────────────────────────────────────────────────┐
│ • test-results/*.xml                                        │
│ • lint-results/*.html, *.xml                                │
│ • coverage-reports/jacoco/**                                │
│ • app-debug-{sha}.apk                                       │
│ • dependency-check-report.html                              │
│ • ktlint-results/**                                         │
│ • detekt-results/**                                         │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
            [Download for local review]


Release Workflow Artifacts (90-365 days):
┌─────────────────────────────────────────────────────────────┐
│ • focusmother-v{version}.apk          (90 days)            │
│ • focusmother-v{version}.aab          (90 days)            │
│ • mapping.txt (ProGuard)              (365 days) ⚠️ KEEP   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ├─────────────────────────────┐
                          ▼                             ▼
            ┌──────────────────────┐      ┌──────────────────────┐
            │  GitHub Release      │      │  Firebase Crashlytics│
            │  (Public download)   │      │  (Deobfuscation)     │
            └──────────────────────┘      └──────────────────────┘


Health Check Artifacts (7-30 days):
┌─────────────────────────────────────────────────────────────┐
│ • smoke-test-results-{run}/*.xml        (7 days)           │
│ • dependency-check-report-{run}.html    (7 days)           │
│ • health-report-{run}.md                (30 days)          │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
            [Monitor and review reports]
```

---

## Secrets Flow (Security)

```
┌─────────────────────────────────────────────────────────────┐
│                    SECRETS MANAGEMENT                       │
└─────────────────────────────────────────────────────────────┘

Developer Local Machine:
┌─────────────────────────────────────────────────────────────┐
│ 1. Generate keystore:                                       │
│    focusmother-release.keystore                             │
│                                                             │
│ 2. Encode to base64:                                        │
│    base64 -w 0 focusmother-release.keystore > keystore.txt  │
│                                                             │
│ 3. Store passwords in password manager                      │
│                                                             │
│ 4. Backup keystore (multiple locations)                     │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
            ┌──────────────────────┐
            │  Upload to GitHub    │
            │  Settings > Secrets  │
            └──────────────────────┘
                          │
                          ▼
GitHub Secrets (Encrypted at rest):
┌─────────────────────────────────────────────────────────────┐
│ • KEYSTORE_FILE            (base64 encoded)                │
│ • KEYSTORE_PASSWORD        (encrypted)                     │
│ • KEY_ALIAS                (encrypted)                     │
│ • KEY_PASSWORD             (encrypted)                     │
│ • PLAY_STORE_SERVICE_ACCOUNT_JSON (optional)               │
│ • SLACK_WEBHOOK_URL        (optional)                      │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
Release Workflow (Ephemeral):
┌─────────────────────────────────────────────────────────────┐
│ 1. Decode KEYSTORE_FILE → release-keystore.jks             │
│ 2. Create keystore.properties (in memory)                   │
│ 3. Build and sign APK/AAB                                   │
│ 4. Delete keystore files (cleanup)                          │
│ 5. Secrets NEVER appear in logs                             │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
            ┌──────────────────────┐
            │  Signed APK/AAB      │
            │  (Ready to deploy)   │
            └──────────────────────┘
```

---

## Error Handling & Rollback

```
┌─────────────────────────────────────────────────────────────┐
│                    ERROR SCENARIOS                          │
└─────────────────────────────────────────────────────────────┘

Test Failure:
┌─────────────────────────────────────────────────────────────┐
│ CI → Test Job Fails                                         │
│  ↓                                                          │
│ CI Summary marks as FAILURE                                 │
│  ↓                                                          │
│ PR cannot be merged (status check required)                 │
│  ↓                                                          │
│ Developer fixes tests → Push again → CI reruns              │
└─────────────────────────────────────────────────────────────┘

Security Vulnerability:
┌─────────────────────────────────────────────────────────────┐
│ CI → Security Scan detects CRITICAL/HIGH CVE                │
│  ↓                                                          │
│ Workflow fails (exit code 1)                                │
│  ↓                                                          │
│ Upload vulnerability report                                 │
│  ↓                                                          │
│ Developer reviews report → Update dependencies              │
│  ↓                                                          │
│ If false positive → Add to owasp-suppressions.xml           │
└─────────────────────────────────────────────────────────────┘

Release Failure:
┌─────────────────────────────────────────────────────────────┐
│ Release → Validation fails                                  │
│  ↓                                                          │
│ Workflow stops (does not build)                             │
│  ↓                                                          │
│ Fix issues → Delete tag → Retag → Trigger again             │
└─────────────────────────────────────────────────────────────┘

Post-Deployment Issue:
┌─────────────────────────────────────────────────────────────┐
│ Health Check → Smoke tests fail                             │
│  ↓                                                          │
│ Health Summary marks as UNHEALTHY                           │
│  ↓                                                          │
│ Rollback recommendation issued                              │
│  ↓                                                          │
│ Manual rollback:                                            │
│  1. Delete GitHub Release                                   │
│  2. Delete git tag                                          │
│  3. Play Console: Stop rollout                              │
│  4. Fix issues → Retag with new version                     │
└─────────────────────────────────────────────────────────────┘

Keystore Issues:
┌─────────────────────────────────────────────────────────────┐
│ Release → Keystore decode fails                             │
│  ↓                                                          │
│ Workflow fails with error message                           │
│  ↓                                                          │
│ Check KEYSTORE_FILE secret is set                           │
│  ↓                                                          │
│ Re-encode keystore: base64 -w 0 keystore.jks                │
│  ↓                                                          │
│ Update GitHub Secret → Retry workflow                       │
└─────────────────────────────────────────────────────────────┘
```

---

## Performance Optimization

```
┌─────────────────────────────────────────────────────────────┐
│                 CACHING STRATEGY                            │
└─────────────────────────────────────────────────────────────┘

Gradle Dependencies:
┌─────────────────────────────────────────────────────────────┐
│ uses: actions/cache@v4                                      │
│ path:                                                       │
│   - ~/.gradle/caches                                        │
│   - ~/.gradle/wrapper                                       │
│ key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*')}}│
│                                                             │
│ Benefit: 30-50% faster builds                               │
└─────────────────────────────────────────────────────────────┘

JDK Setup:
┌─────────────────────────────────────────────────────────────┐
│ uses: actions/setup-java@v4                                 │
│ with:                                                       │
│   cache: 'gradle'  # Automatic caching                      │
│                                                             │
│ Benefit: Faster Java setup                                  │
└─────────────────────────────────────────────────────────────┘

Parallel Execution:
┌─────────────────────────────────────────────────────────────┐
│ CI Workflow:                                                │
│  ├─ test (parallel)                                         │
│  ├─ security-scan (parallel)                                │
│  └─ build-quality (parallel)                                │
│                                                             │
│ Benefit: 3 jobs run simultaneously                          │
│ Total time: ~8-12 min (vs 25+ min sequential)              │
└─────────────────────────────────────────────────────────────┘

Concurrency Control:
┌─────────────────────────────────────────────────────────────┐
│ concurrency:                                                │
│   group: ${{ github.workflow }}-${{ github.ref }}           │
│   cancel-in-progress: true                                  │
│                                                             │
│ Benefit: Cancel duplicate runs on new push                  │
└─────────────────────────────────────────────────────────────┘
```

---

## Monitoring Dashboard (Conceptual)

```
┌─────────────────────────────────────────────────────────────┐
│                 CI/CD HEALTH DASHBOARD                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Build Success Rate:         [████████░░] 95%              │
│  Average Build Time:         8.5 minutes                    │
│  Last 10 Builds:             ✅✅✅✅✅✅✅❌✅✅               │
│                                                             │
│  Test Coverage:              [███████░░░] 73%              │
│  Active Vulnerabilities:     2 (0 HIGH, 2 MEDIUM)          │
│  APK Size Trend:             ↗ 28.5 MB (+1.2 MB)           │
│                                                             │
│  Latest Release:             v1.0.2 (2 days ago)            │
│  Crash-free Rate:            [█████████░] 99.2%            │
│  Health Status:              ✅ HEALTHY                     │
│                                                             │
│  ProGuard Mappings Saved:    ✅ 12 versions                 │
│  Keystore Backup Verified:   ✅ 15 days ago                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Note:** Implement with GitHub Actions insights, custom scripts, or monitoring platforms.

---

## Quick Actions

### Release a New Version
```bash
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

### Manual Release
Go to: Actions → Android Release → Run workflow

### Check CI Status
Go to: Actions → Android CI → Latest run

### Download Artifacts
Go to: Actions → Workflow run → Artifacts section

### Rollback Release
```bash
git tag -d v1.0.1
git push origin :refs/tags/v1.0.1
```

### View Health Check
Go to: Actions → Health Check → Latest run

---

**Architecture Version:** 1.0.0
**Last Updated:** 2026-01-11
**Maintained By:** DevOps Team
