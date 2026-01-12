# CI/CD Pipeline Setup Documentation

This document provides comprehensive guidance for setting up and using the GitHub Actions CI/CD pipeline for the FocusMother Android application.

---

## Table of Contents

1. [Overview](#overview)
2. [Pipeline Architecture](#pipeline-architecture)
3. [Setup Instructions](#setup-instructions)
4. [GitHub Secrets Configuration](#github-secrets-configuration)
5. [Workflow Details](#workflow-details)
6. [Release Process](#release-process)
7. [Rollback Procedures](#rollback-procedures)
8. [Monitoring and Telemetry](#monitoring-and-telemetry)
9. [Troubleshooting](#troubleshooting)
10. [Best Practices](#best-practices)

---

## Overview

The FocusMother CI/CD pipeline consists of three main workflows:

1. **android-ci.yml** - Continuous Integration (triggered on push/PR)
2. **android-release.yml** - Release automation (manual or tag-based)
3. **health-check.yml** - Post-deployment validation and monitoring

### Key Features

- Automated testing (unit tests, lint, security scans)
- Release APK/AAB building with code signing
- ProGuard mapping file generation and preservation
- GitHub Release creation with changelog
- Optional Google Play Store deployment
- Post-deployment health checks and smoke testing
- Security vulnerability scanning
- APK size monitoring and optimization checks

---

## Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Developer Workflow                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Push to Branch  │
                    └──────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │         Android CI Workflow             │
        ├─────────────────────────────────────────┤
        │  • Checkout Code                        │
        │  • Run Lint Checks                      │
        │  • Run Unit Tests                       │
        │  • Generate Coverage Report             │
        │  • Build Debug APK                      │
        │  • Security Scanning                    │
        │  • Code Quality Checks                  │
        └─────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │   CI Passed?      │
                    └─────────┬─────────┘
                              │ YES
                              ▼
        ┌─────────────────────────────────────────┐
        │      Create Version Tag (v1.0.0)        │
        └─────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │       Android Release Workflow          │
        ├─────────────────────────────────────────┤
        │  • Pre-Release Validation               │
        │  • Build Signed APK/AAB                 │
        │  • Generate ProGuard Mappings           │
        │  • Create GitHub Release                │
        │  • Optional: Deploy to Play Store       │
        └─────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │       Health Check Workflow             │
        ├─────────────────────────────────────────┤
        │  • Smoke Tests                          │
        │  • APK Validation                       │
        │  • Dependency Vulnerability Check       │
        │  • Performance Analysis                 │
        └─────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │   Healthy?        │
                    └─────────┬─────────┘
                              │ YES
                              ▼
        ┌─────────────────────────────────────────┐
        │      Deploy to Production / Beta        │
        └─────────────────────────────────────────┘
```

---

## Setup Instructions

### 1. Initial Repository Setup

Ensure your GitHub repository has Actions enabled:

1. Go to your repository on GitHub
2. Click **Settings** → **Actions** → **General**
3. Under "Actions permissions", select **Allow all actions and reusable workflows**

### 2. Generate Release Keystore

Create a keystore for signing release builds:

```bash
# Generate keystore (run this locally, NOT in CI)
keytool -genkey -v -keystore focusmother-release.keystore \
  -alias focusmother \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Answer the prompts:
# - Password for keystore (SAVE THIS!)
# - Password for key alias (SAVE THIS!)
# - Your name, organization, etc.
```

**CRITICAL: Backup this keystore securely!** If you lose it, you cannot update your app on Google Play.

### 3. Encode Keystore to Base64

The keystore must be base64 encoded to store as a GitHub Secret:

```bash
# On Linux/macOS
base64 -i focusmother-release.keystore -o keystore.base64.txt

# On Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("focusmother-release.keystore")) | Out-File keystore.base64.txt

# On Windows (Git Bash)
base64 -w 0 focusmother-release.keystore > keystore.base64.txt
```

### 4. Update build.gradle.kts for Signing

Add signing configuration to `app/build.gradle.kts`:

```kotlin
android {
    // ... existing config ...

    signingConfigs {
        create("release") {
            // These will be loaded from keystore.properties in CI
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))

                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 5. Update .gitignore

Create or update `.gitignore` in the project root:

```gitignore
# Keystore files - NEVER commit these!
*.keystore
*.jks
keystore.properties
keystore.base64.txt

# Build artifacts
*.apk
*.aab
*.ap_
*.dex

# Gradle files
.gradle/
build/
*/build/

# Local configuration
local.properties

# IDE files
.idea/
*.iml
.vscode/
*.swp
*.swo

# OS files
.DS_Store
Thumbs.db

# Logs
*.log

# NDK
obj/
.externalNativeBuild/
.cxx/
```

---

## GitHub Secrets Configuration

Navigate to your GitHub repository → **Settings** → **Secrets and variables** → **Actions**.

### Required Secrets

Add the following secrets:

| Secret Name | Description | How to Obtain |
|-------------|-------------|---------------|
| `KEYSTORE_FILE` | Base64-encoded keystore | Contents of `keystore.base64.txt` |
| `KEYSTORE_PASSWORD` | Keystore password | Password you set when creating keystore |
| `KEY_ALIAS` | Key alias name | Usually "focusmother" |
| `KEY_PASSWORD` | Key password | Password for the key alias |

### Optional Secrets (for Play Store deployment)

| Secret Name | Description | How to Obtain |
|-------------|-------------|---------------|
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | Google Play service account JSON | From Google Play Console |
| `SLACK_WEBHOOK_URL` | Slack webhook for notifications | From Slack workspace settings |

### Setting Up Google Play Service Account

1. Go to [Google Play Console](https://play.google.com/console/)
2. Navigate to **Setup** → **API access**
3. Create a new service account or use existing
4. Grant **Release Manager** permissions
5. Download the JSON key file
6. Copy the **entire contents** of the JSON file as the secret value

---

## Workflow Details

### 1. Android CI Workflow (`android-ci.yml`)

**Triggers:**
- Push to `main`, `master`, or `develop` branches
- Pull requests to these branches

**Jobs:**

#### test
- Runs lint checks
- Executes unit tests
- Generates test coverage reports
- Builds debug APK
- Uploads artifacts

#### security-scan
- Dependency vulnerability scanning (OWASP)
- Secret scanning with TruffleHog
- Hardcoded secret detection
- Security configuration checks

#### build-quality
- Kotlin code style checks (ktlint)
- Static code analysis (detekt)
- APK size monitoring
- TODO/FIXME detection

**Artifacts Generated:**
- `lint-results` (14 days retention)
- `test-results` (14 days retention)
- `coverage-reports` (14 days retention)
- `app-debug-{sha}` (14 days retention)

---

### 2. Android Release Workflow (`android-release.yml`)

**Triggers:**
- Manual workflow dispatch (Actions tab → Run workflow)
- Git tags matching `v*.*.*` pattern (e.g., `v1.0.0`)

**Jobs:**

#### validate
- Extracts version information
- Runs all tests
- Runs lint checks
- Validates build readiness

#### build
- Decodes keystore from secrets
- Updates version in build.gradle.kts
- Builds signed release APK
- Builds signed release AAB
- Verifies APK signature
- Generates ProGuard mapping files

#### release
- Downloads build artifacts
- Generates changelog from git commits
- Creates GitHub Release
- Attaches APK, AAB, and mapping.txt

#### deploy-play-store (optional)
- Only runs if `deploy_to_play` input is `true`
- Uploads AAB to Google Play Internal Testing track
- Uploads ProGuard mappings for crash deobfuscation

#### health-check
- Post-release validation summary

**Artifacts Generated:**
- `release-apk-{version}` (90 days retention)
- `release-aab-{version}` (90 days retention)
- `proguard-mappings-{version}` (365 days retention)

---

### 3. Health Check Workflow (`health-check.yml`)

**Triggers:**
- After successful release workflow
- Scheduled (every 6 hours)
- Manual workflow dispatch

**Jobs:**

#### smoke-test
- Runs critical smoke tests
- Validates core functionality

#### apk-validation
- Downloads latest release APK
- Verifies signature
- Analyzes APK structure
- Checks for security issues

#### dependency-check
- Checks for outdated dependencies
- Runs OWASP vulnerability scan

#### performance-check
- Analyzes method count
- Static memory leak detection
- Resource usage analysis

#### health-summary
- Aggregates all health check results
- Generates health report
- Triggers rollback recommendation if critical failures

**Artifacts Generated:**
- `smoke-test-results-{run}` (7 days retention)
- `dependency-check-report-{run}` (7 days retention)
- `health-report-{run}` (30 days retention)

---

## Release Process

### Standard Release Process

#### 1. Prepare for Release

```bash
# Ensure you're on the main branch and it's up to date
git checkout main
git pull origin main

# Run tests locally first
./gradlew test lint

# Ensure all changes are committed
git status
```

#### 2. Create Version Tag

```bash
# Tag the release (use semantic versioning)
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push the tag to GitHub
git push origin v1.0.0
```

This will automatically trigger the release workflow.

#### 3. Manual Release (Alternative)

1. Go to GitHub → **Actions** tab
2. Select **Android Release** workflow
3. Click **Run workflow**
4. Fill in the parameters:
   - **Version name**: `1.0.0`
   - **Version code**: `10000` (use formula: major * 10000 + minor * 100 + patch)
   - **Release notes**: Brief description of changes
   - **Deploy to Play**: Check if deploying to Play Store
5. Click **Run workflow**

### Version Code Calculation

Use this formula for version codes:

```
version_code = major * 10000 + minor * 100 + patch

Examples:
- 1.0.0 → 10000
- 1.2.3 → 10203
- 2.5.1 → 20501
```

This allows up to version 99.99.99 before reaching Android's version code limit.

---

## Rollback Procedures

### When to Rollback

Trigger a rollback if:
- Smoke tests fail in health check
- Critical crashes reported (>5% crash rate)
- APK signature verification fails
- Security vulnerabilities discovered
- Critical functionality broken in production

### Rollback Steps

#### 1. Emergency Rollback (Play Store)

If the release was deployed to Google Play:

```bash
# Via Play Console (Recommended)
1. Go to Google Play Console
2. Navigate to Release → Production (or Internal Testing)
3. Click "Manage" on the problematic release
4. Click "Stop rollout" or "Halt rollout"
5. Promote the previous stable version

# Via API (Advanced)
# Use the Play Developer API to halt rollout
```

#### 2. Revert GitHub Release

```bash
# Delete the problematic tag
git tag -d v1.0.1
git push origin :refs/tags/v1.0.1

# Optionally delete the GitHub Release via web UI
# Go to Releases → Click "Delete" on the release
```

#### 3. Fix and Re-Release

```bash
# Create a hotfix branch
git checkout -b hotfix/1.0.2

# Make fixes
# ... fix the issues ...

# Commit and test
git commit -am "Hotfix: Fix critical issue"
./gradlew test lint

# Merge to main
git checkout main
git merge hotfix/1.0.2

# Create new release tag
git tag -a v1.0.2 -m "Hotfix release 1.0.2"
git push origin main --tags
```

### Rollback Criteria Thresholds

| Metric | Threshold | Action |
|--------|-----------|--------|
| Crash-free rate | < 99.0% | Immediate rollback |
| Smoke test failures | Any failure | Block deployment |
| APK size increase | > 50% | Investigate, consider rollback |
| Security vulnerability | CRITICAL/HIGH | Immediate rollback |
| User-reported issues | > 10 in first hour | Monitor, prepare rollback |

---

## Monitoring and Telemetry

### Key Metrics to Monitor

#### Build Metrics
- CI pipeline success rate (target: >95%)
- Average build time (target: <10 minutes)
- Test coverage (target: >70%)
- Number of lint violations (target: 0 errors)

#### Release Metrics
- Release frequency (track over time)
- Time from commit to production (target: <2 hours for emergency fixes)
- APK size trend (monitor for bloat)
- ProGuard mapping file size

#### Health Metrics
- Smoke test pass rate (target: 100%)
- Crash-free session rate (target: >99.5%)
- ANR rate (target: <0.1%)
- App startup time (target: <2 seconds)

### Monitoring Tools Integration

#### Firebase Crashlytics

Add to `app/build.gradle.kts`:

```kotlin
plugins {
    // ... existing plugins ...
    id("com.google.firebase.crashlytics")
}

dependencies {
    // ... existing dependencies ...
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
}
```

Upload ProGuard mappings automatically in release workflow (already configured).

#### Sentry (Alternative)

```kotlin
dependencies {
    implementation("io.sentry:sentry-android:6.x.x")
}
```

Add to `FocusMotherApplication.kt`:

```kotlin
import io.sentry.android.core.SentryAndroid

override fun onCreate() {
    super.onCreate()
    SentryAndroid.init(this) { options ->
        options.dsn = "YOUR_SENTRY_DSN"
        options.environment = if (BuildConfig.DEBUG) "development" else "production"
    }
}
```

### GitHub Actions Insights

Monitor workflow performance:
1. Go to **Actions** → **Workflow name**
2. View run statistics (success rate, duration)
3. Identify bottlenecks in job execution
4. Optimize caching and parallelization

---

## Troubleshooting

### Common Issues

#### 1. "Keystore file not found" Error

**Cause:** KEYSTORE_FILE secret not configured or incorrect base64 encoding.

**Solution:**
```bash
# Re-encode keystore
base64 -w 0 focusmother-release.keystore > keystore.base64.txt

# Copy contents to GitHub Secret
cat keystore.base64.txt | pbcopy  # macOS
cat keystore.base64.txt | clip     # Windows

# Add to GitHub Secrets as KEYSTORE_FILE
```

#### 2. "APK signature verification failed"

**Cause:** Incorrect signing configuration or mismatched passwords.

**Solution:**
- Verify KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD secrets
- Ensure keystore file is not corrupted
- Test signing locally:
  ```bash
  ./gradlew assembleRelease
  $ANDROID_HOME/build-tools/*/apksigner verify app/build/outputs/apk/release/app-release.apk
  ```

#### 3. Tests failing in CI but passing locally

**Cause:** Environment differences or flaky tests.

**Solution:**
- Check for hardcoded paths or timestamps
- Use `Robolectric` for Android framework dependencies
- Add `@Ignore` to flaky tests temporarily
- Run tests with `--info` flag for detailed logs:
  ```bash
  ./gradlew test --info --stacktrace
  ```

#### 4. "Gradle build timeout"

**Cause:** Large dependencies or slow network.

**Solution:**
- Increase timeout in workflow (default: 30 minutes)
- Optimize Gradle caching
- Use `--parallel` flag:
  ```bash
  ./gradlew assembleRelease --parallel --stacktrace
  ```

#### 5. ProGuard mapping file missing

**Cause:** ProGuard not enabled or incorrect path.

**Solution:**
- Ensure `isMinifyEnabled = true` in release build type
- Check mapping file location:
  ```
  app/build/outputs/mapping/release/mapping.txt
  ```
- Verify ProGuard rules don't strip critical code

#### 6. Google Play deployment fails

**Cause:** Service account permissions or incorrect package name.

**Solution:**
- Verify service account has "Release Manager" role
- Check package name matches: `com.focusmother.android`
- Ensure AAB is signed correctly
- Review Play Console error logs

### Debug Workflows Locally

Use [act](https://github.com/nektos/act) to run GitHub Actions locally:

```bash
# Install act
# macOS
brew install act

# Linux
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Run CI workflow locally
act push

# Run release workflow with inputs
act workflow_dispatch -e event.json
```

---

## Best Practices

### 1. Version Management

- Use **semantic versioning**: MAJOR.MINOR.PATCH
  - MAJOR: Breaking changes
  - MINOR: New features (backward compatible)
  - PATCH: Bug fixes
- Always tag releases: `git tag -a v1.0.0 -m "Release 1.0.0"`
- Keep CHANGELOG.md updated

### 2. Security

- **NEVER** commit keystores, passwords, or API keys
- Rotate secrets periodically (every 6 months)
- Use separate keystores for debug and release
- Enable code obfuscation (ProGuard/R8) for release
- Review security scan results before merging

### 3. Testing

- Maintain >70% code coverage
- Write smoke tests for critical user flows
- Use CI to enforce quality gates (no merge if tests fail)
- Test on multiple Android versions (API 28-34)

### 4. Build Optimization

- Enable Gradle build cache: `org.gradle.caching=true` in `gradle.properties`
- Use parallel builds: `org.gradle.parallel=true`
- Configure daemon: `org.gradle.daemon=true`
- Optimize dependencies (remove unused libraries)

### 5. Release Cadence

- **Patch releases**: As needed (critical bugs)
- **Minor releases**: Every 2-4 weeks
- **Major releases**: Every 3-6 months
- **Beta releases**: Weekly during active development

### 6. ProGuard Mapping Preservation

- **CRITICAL**: Keep ProGuard mapping files for at least 1 year
- Mapping files are required to deobfuscate crash reports
- Upload to Firebase Crashlytics automatically
- Store in GitHub artifacts (365 days retention)

### 7. Monitoring

- Set up Firebase Crashlytics before beta release
- Monitor crash-free rate daily
- Review ANR (Application Not Responding) reports
- Track app size growth over time

### 8. Rollout Strategy

For production releases:
1. **Internal Testing**: 5-10 testers (1-2 days)
2. **Closed Beta**: 50-100 testers (1 week)
3. **Open Beta**: 500+ testers (2 weeks)
4. **Staged Rollout**:
   - Day 1: 10% of users
   - Day 3: 25% of users
   - Day 5: 50% of users
   - Day 7: 100% of users (if no critical issues)

### 9. Emergency Hotfix Process

For critical production bugs:

```bash
# 1. Create hotfix branch from the problematic release tag
git checkout -b hotfix/1.0.1 v1.0.0

# 2. Fix the issue
# ... make changes ...

# 3. Test thoroughly
./gradlew test connectedAndroidTest

# 4. Commit and tag
git commit -am "Hotfix: Fix critical crash"
git tag -a v1.0.1 -m "Hotfix: Fix critical crash"

# 5. Merge back to main
git checkout main
git merge hotfix/1.0.1

# 6. Push tag to trigger release
git push origin main --tags
```

### 10. Branch Protection

Configure branch protection rules for `main`:

1. Go to **Settings** → **Branches** → **Add rule**
2. Branch name pattern: `main`
3. Enable:
   - Require pull request reviews (1 approval)
   - Require status checks to pass (CI tests)
   - Require branches to be up to date
   - Include administrators

---

## Quick Reference Commands

```bash
# Local development
./gradlew clean                    # Clean build
./gradlew assembleDebug            # Build debug APK
./gradlew test                     # Run unit tests
./gradlew lint                     # Run lint checks
./gradlew assembleRelease          # Build release APK (requires keystore)
./gradlew bundleRelease            # Build release AAB

# Version tagging
git tag -a v1.0.0 -m "Release 1.0.0"    # Create tag
git push origin v1.0.0                   # Push tag
git tag -d v1.0.0                        # Delete local tag
git push origin :refs/tags/v1.0.0       # Delete remote tag

# Keystore management
keytool -genkey -v -keystore release.keystore -alias myapp -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 release.keystore > keystore.base64.txt
keytool -list -v -keystore release.keystore  # View keystore info

# APK inspection
$ANDROID_HOME/build-tools/*/apksigner verify --verbose app-release.apk
$ANDROID_HOME/build-tools/*/aapt dump badging app-release.apk
unzip -l app-release.apk  # List APK contents
```

---

## Support and Resources

### Official Documentation
- [GitHub Actions for Android](https://docs.github.com/en/actions/automating-builds-and-tests/building-and-testing-java-with-gradle)
- [Android Developers - Build & Test](https://developer.android.com/studio/test)
- [ProGuard Manual](https://www.guardsquare.com/manual/home)
- [Google Play Console Help](https://support.google.com/googleplay/android-developer)

### Tools
- [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics)
- [GitHub Actions Marketplace](https://github.com/marketplace?type=actions&query=android)
- [act - Run Actions locally](https://github.com/nektos/act)

### Contact
For issues or questions about the CI/CD pipeline:
- GitHub Issues: [Create an issue](https://github.com/your-repo/issues/new)
- Email: devops@focusmother.app (if applicable)

---

**Document Version:** 1.0.0
**Last Updated:** 2026-01-11
**Maintained By:** DevOps Team
