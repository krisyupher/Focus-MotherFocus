# CI/CD Quick Reference Guide

Fast reference for common CI/CD operations for FocusMother Android.

---

## Quick Start

### First Time Setup (5 minutes)

```bash
# 1. Generate release keystore
keytool -genkey -v -keystore focusmother-release.keystore \
  -alias focusmother -keyalg RSA -keysize 2048 -validity 10000

# 2. Encode for GitHub
base64 -w 0 focusmother-release.keystore > keystore.base64.txt

# 3. Add to GitHub Secrets (via web UI):
#    - KEYSTORE_FILE (contents of keystore.base64.txt)
#    - KEYSTORE_PASSWORD
#    - KEY_ALIAS (focusmother)
#    - KEY_PASSWORD

# 4. Test locally
./gradlew test lint assembleDebug

# 5. Commit and push
git add .github/workflows/
git commit -m "Add CI/CD pipeline"
git push
```

---

## Common Commands

### Local Development

```bash
# Clean build
./gradlew clean

# Run tests
./gradlew test

# Run lint
./gradlew lint

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires keystore)
./gradlew assembleRelease

# Run all quality checks
./gradlew test lint ktlintCheck detekt

# Generate test coverage
./gradlew jacocoTestReport
```

### Release Process

```bash
# Option 1: Tag-based release
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0

# Option 2: Manual via GitHub UI
# Go to Actions → Android Release → Run workflow

# Check release status
git ls-remote --tags origin

# Delete tag if needed (before release completes)
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0
```

### Troubleshooting

```bash
# View local test results
open app/build/reports/tests/testDebugUnitTest/index.html

# View lint results
open app/build/reports/lint-results-debug.html

# Check APK signature
$ANDROID_HOME/build-tools/*/apksigner verify --verbose \
  app/build/outputs/apk/release/app-release.apk

# List APK contents
unzip -l app/build/outputs/apk/release/app-release.apk

# Check APK size
du -h app/build/outputs/apk/release/app-release.apk
```

---

## GitHub Actions Workflows

### 1. Android CI (android-ci.yml)

**Triggers:**
- Push to `main`, `master`, `develop`
- Pull requests

**What it does:**
- Runs tests
- Checks code quality
- Scans for security issues
- Builds debug APK

**How to view:**
- Go to **Actions** tab → **Android CI**

### 2. Android Release (android-release.yml)

**Triggers:**
- Git tags `v*.*.*`
- Manual workflow dispatch

**What it does:**
- Validates code
- Builds signed APK/AAB
- Creates GitHub Release
- Optionally deploys to Play Store

**How to trigger manually:**
1. Go to **Actions** → **Android Release**
2. Click **Run workflow**
3. Fill in version info
4. Click **Run workflow**

### 3. Health Check (health-check.yml)

**Triggers:**
- After successful release
- Every 6 hours (scheduled)
- Manual trigger

**What it does:**
- Runs smoke tests
- Validates APK
- Checks for vulnerabilities
- Monitors performance

**How to trigger:**
- Go to **Actions** → **Health Check** → **Run workflow**

---

## Version Management

### Version Code Formula

```
version_code = major * 10000 + minor * 100 + patch

Examples:
1.0.0  → 10000
1.2.5  → 10205
2.10.3 → 21003
```

### Semantic Versioning

- **MAJOR**: Breaking changes (1.0.0 → 2.0.0)
- **MINOR**: New features (1.0.0 → 1.1.0)
- **PATCH**: Bug fixes (1.0.0 → 1.0.1)

### Tagging Releases

```bash
# Standard release
git tag -a v1.0.0 -m "Release 1.0.0 - Initial beta"
git push origin v1.0.0

# Hotfix
git tag -a v1.0.1 -m "Hotfix 1.0.1 - Fix crash on startup"
git push origin v1.0.1

# Pre-release (doesn't trigger release workflow)
git tag -a v1.1.0-beta.1 -m "Beta release"
git push origin v1.1.0-beta.1
```

---

## GitHub Secrets Reference

### Required Secrets

| Secret | Description | Example |
|--------|-------------|---------|
| `KEYSTORE_FILE` | Base64 keystore | `MIIKpAIBAzCC...` |
| `KEYSTORE_PASSWORD` | Keystore password | `MySecurePass123!` |
| `KEY_ALIAS` | Key alias | `focusmother` |
| `KEY_PASSWORD` | Key password | `MyKeyPass456!` |

### Optional Secrets

| Secret | Description | When Needed |
|--------|-------------|-------------|
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | Google Play API key | For Play Store deployment |
| `SLACK_WEBHOOK_URL` | Slack notifications | For CI/CD alerts |

### How to Add Secrets

1. Go to repository **Settings**
2. Click **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Enter name and value
5. Click **Add secret**

---

## Workflow Status Checks

### Required Status Checks for PRs

Configure in **Settings** → **Branches** → **Branch protection rules**:

- ✅ `test` (Android CI)
- ✅ `security-scan` (Android CI)
- ✅ `build-quality` (Android CI)

### How to Fix Failing Checks

**Tests failing:**
```bash
./gradlew test --info
# Fix issues in test output
```

**Lint failing:**
```bash
./gradlew lint
# Fix issues in app/build/reports/lint-results-debug.html
```

**Security scan failing:**
```bash
./gradlew dependencyCheckAnalyze
# Review dependency-check-report.html
```

---

## Emergency Procedures

### Rollback a Release

```bash
# 1. Delete the tag
git tag -d v1.0.1
git push origin :refs/tags/v1.0.1

# 2. Delete GitHub Release (via web UI)
# Go to Releases → Click release → Delete

# 3. In Play Console (if deployed):
# Release → Production → Stop rollout
# Promote previous version
```

### Hotfix Process

```bash
# 1. Create hotfix branch from tag
git checkout -b hotfix/1.0.1 v1.0.0

# 2. Fix the issue
# ... make changes ...
git commit -am "Hotfix: Fix critical crash"

# 3. Test thoroughly
./gradlew test connectedAndroidTest

# 4. Merge to main
git checkout main
git merge hotfix/1.0.1

# 5. Tag and release
git tag -a v1.0.1 -m "Hotfix: Fix critical crash"
git push origin main --tags
```

### Cancel In-Progress Release

1. Go to **Actions**
2. Find the running workflow
3. Click **Cancel workflow**
4. Delete the tag if it was already pushed

---

## Artifacts and Reports

### Where to Find Build Artifacts

**CI Artifacts (14 days):**
- Test results: `test-results`
- Lint reports: `lint-results`
- Coverage: `coverage-reports`
- Debug APK: `app-debug-{sha}`

**Release Artifacts (90 days):**
- Release APK: `release-apk-{version}`
- Release AAB: `release-aab-{version}`
- ProGuard mappings: `proguard-mappings-{version}` (365 days!)

**How to download:**
1. Go to **Actions** → Select workflow run
2. Scroll to **Artifacts** section
3. Click artifact name to download

### ProGuard Mappings

**CRITICAL:** Save these for crash deobfuscation!

```bash
# Download from GitHub
# Actions → Release workflow → Artifacts → proguard-mappings-{version}

# Store in secure location for 1+ year
# Upload to Firebase Crashlytics (automatic in workflow)
```

---

## Performance Optimization

### Speed Up CI Builds

```groovy
// In gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx4096m
```

### Reduce APK Size

```kotlin
// In app/build.gradle.kts
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}
```

### Cache Optimization

Workflows already use:
- Gradle dependency caching
- Android SDK caching
- Build cache

---

## Monitoring and Alerts

### What to Monitor

- ✅ CI success rate (target: >95%)
- ✅ Build time (target: <10 min)
- ✅ Test coverage (target: >70%)
- ✅ APK size (warn if >50MB)
- ✅ Crash-free rate (target: >99.5%)

### Setting Up Alerts

**Slack Integration:**
1. Create Slack webhook
2. Add `SLACK_WEBHOOK_URL` secret
3. Workflows will auto-notify on failures

**Email Notifications:**
- Automatic via GitHub (Settings → Notifications)

---

## Best Practices Checklist

### Before Every Commit
- [ ] Run tests locally: `./gradlew test`
- [ ] Run lint: `./gradlew lint`
- [ ] Check no TODOs/FIXMEs in critical code
- [ ] Verify no hardcoded secrets

### Before Every Release
- [ ] All tests passing
- [ ] Version code/name updated
- [ ] Changelog updated
- [ ] ProGuard rules verified
- [ ] APK tested on real device
- [ ] Security scan passed

### After Every Release
- [ ] Download and test APK
- [ ] Verify ProGuard mappings saved
- [ ] Monitor crash reports (first 24h)
- [ ] Check user feedback
- [ ] Update documentation

---

## Useful Links

- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Android Build Gradle](https://developer.android.com/studio/build)
- [ProGuard Manual](https://www.guardsquare.com/manual/home)
- [Play Console](https://play.google.com/console/)
- [Firebase Crashlytics](https://console.firebase.google.com/)

---

## Support

**Issues with CI/CD:**
- Check workflow logs in Actions tab
- Review [CI_CD_SETUP.md](./CI_CD_SETUP.md) for detailed docs
- Open GitHub issue with "ci/cd" label

**Keystore Issues:**
- Review [KEYSTORE_SETUP.md](./KEYSTORE_SETUP.md)
- Check backups immediately
- Contact team lead if lost

---

**Quick Reference Version:** 1.0.0
**Last Updated:** 2026-01-11
