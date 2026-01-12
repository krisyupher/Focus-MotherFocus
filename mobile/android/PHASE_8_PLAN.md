# Phase 8: Beta Launch Prep - Implementation Plan

**Date:** 2026-01-11
**Status:** 🚀 **IN PROGRESS**
**Target Completion:** 1 week

---

## 🎯 Phase 8 Objectives

Prepare the FocusMother Android application for limited beta release with production-ready infrastructure, legal compliance, and deployment automation.

---

## 📋 Task Breakdown

### Priority 1: Production Readiness (Critical - Day 1-2)

#### 1.1 Add Certificate Pins ⚡ URGENT
**Status:** Not Started
**Time:** 1 hour
**Agent:** Manual (requires server certificates)

**Steps:**
1. Obtain certificate pins for api.anthropic.com
2. Obtain certificate pins for api.readyplayer.me
3. Update `network_security_config.xml` with actual pins
4. Set expiration dates (1 year ahead)
5. Document pin rotation procedure
6. Test connectivity with pins enabled

**Command to get pins:**
```bash
# For api.anthropic.com
openssl s_client -connect api.anthropic.com:443 -servername api.anthropic.com < /dev/null 2>/dev/null | \
openssl x509 -pubkey -noout | \
openssl pkey -pubin -outform der | \
openssl dgst -sha256 -binary | \
base64

# For api.readyplayer.me
openssl s_client -connect api.readyplayer.me:443 -servername api.readyplayer.me < /dev/null 2>/dev/null | \
openssl x509 -pubkey -noout | \
openssl pkey -pubin -outform der | \
openssl dgst -sha256 -binary | \
base64
```

**Files to Modify:**
- `res/xml/network_security_config.xml`

---

#### 1.2 Set Up Release Signing Configuration
**Status:** Not Started
**Time:** 30 minutes
**Agent:** Manual

**Steps:**
1. Generate release keystore:
   ```bash
   keytool -genkey -v -keystore focusmother-release.keystore \
   -alias focusmother -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Create `keystore.properties` (add to .gitignore)
3. Update `app/build.gradle.kts` with signing config
4. Document keystore backup procedure
5. Store keystore securely (encrypted backup)

**Files to Create:**
- `keystore.properties` (git-ignored)
- `KEYSTORE_BACKUP.md` (documentation)

**Files to Modify:**
- `app/build.gradle.kts` (signing config)
- `.gitignore` (add keystore files)

---

#### 1.3 Final Security Review
**Status:** Not Started
**Time:** 1 hour
**Agent:** security-scanner

**Steps:**
1. Run comprehensive security scan with certificate pins
2. Verify no hardcoded secrets
3. Test data deletion functionality
4. Verify backup exclusions working
5. Test ProGuard obfuscation in release build
6. Perform MITM attack testing

**Files to Create:**
- `PHASE_8_SECURITY_REVIEW.md`

---

### Priority 2: Legal & Privacy (Critical - Day 2-3)

#### 2.1 Create Privacy Policy
**Status:** Not Started
**Time:** 2 hours
**Agent:** Manual (legal document)

**Required Disclosures:**
- Claude API usage (conversations sent to Anthropic)
- Ready Player Me usage (selfie photo processing)
- Local usage statistics storage
- Android permissions (Usage Stats, Camera, Notifications)
- Data retention policy (30 days for conversations)
- Data deletion instructions
- User rights (GDPR compliance)
- Contact information

**Files to Create:**
- `PRIVACY_POLICY.md`
- `app/src/main/assets/privacy_policy.html` (in-app display)

---

#### 2.2 Create Terms of Service
**Status:** Not Started
**Time:** 1.5 hours
**Agent:** Manual (legal document)

**Required Sections:**
- Service description
- User responsibilities
- API usage limits (free vs premium)
- Content policy
- Liability disclaimers
- Termination policy
- Modification rights
- Governing law

**Files to Create:**
- `TERMS_OF_SERVICE.md`
- `app/src/main/assets/terms_of_service.html` (in-app display)

---

#### 2.3 Implement Privacy/Terms Display in App
**Status:** Not Started
**Time:** 45 minutes
**Agent:** None

**Steps:**
1. Create PrivacyPolicyActivity (WebView)
2. Create TermsOfServiceActivity (WebView)
3. Add links in SettingsScreen
4. Add links in OnboardingActivity
5. Require acceptance during onboarding

**Files to Create:**
- `ui/legal/PrivacyPolicyActivity.kt`
- `ui/legal/TermsOfServiceActivity.kt`

**Files to Modify:**
- `ui/settings/SettingsScreen.kt`
- `ui/onboarding/OnboardingActivity.kt`
- `AndroidManifest.xml`

---

### Priority 3: Monitoring & Reliability (Important - Day 3-4)

#### 3.1 Implement Crash Reporting (Firebase Crashlytics)
**Status:** Not Started
**Time:** 1.5 hours
**Agent:** None

**Steps:**
1. Add Firebase to project (google-services.json)
2. Add Crashlytics dependencies
3. Configure ProGuard mapping file upload
4. Add crash reporting initialization
5. Test crash reporting (forced crash in debug)
6. Add custom crash keys (user ID, avatar status)

**Files to Modify:**
- `app/build.gradle.kts`
- `build.gradle.kts` (project level)
- `FocusMotherApplication.kt`

**Files to Create:**
- `google-services.json` (from Firebase Console)

---

#### 3.2 Implement Analytics (Optional - Non-Tracking)
**Status:** Optional
**Time:** 1 hour
**Agent:** None

**Privacy-Preserving Analytics:**
- Feature usage counters (local only)
- Agreement success rate tracking
- Avatar creation success rate
- Crash frequency monitoring

**Note:** NO user tracking, NO personal data, NO third-party analytics

**Files to Create:**
- `util/AnalyticsTracker.kt` (local metrics only)

---

### Priority 4: CI/CD & Deployment (Important - Day 4-5)

#### 4.1 Set Up CI/CD Pipeline
**Status:** Not Started
**Time:** 3 hours
**Agent:** devops-cicd-architect

**Steps:**
1. Create GitHub Actions workflow
2. Set up automated testing on push
3. Configure release build automation
4. Set up automated signing (encrypted secrets)
5. Configure automatic APK upload
6. Add deployment approval gates

**Files to Create:**
- `.github/workflows/android-ci.yml`
- `.github/workflows/release.yml`
- `CI_CD_SETUP.md` (documentation)

**Tasks for devops-cicd-architect:**
- Design complete CI/CD pipeline
- Implement GitHub Actions workflows
- Configure security scanning in pipeline
- Set up artifact storage
- Document deployment process

---

#### 4.2 Prepare Play Store Listing
**Status:** Not Started
**Time:** 4 hours
**Agent:** Manual

**Required Assets:**

**Screenshots (7-8 required):**
1. Dashboard view with active monitoring
2. Conversation with Zordon (intervention)
3. Agreement negotiation flow
4. Active agreements with countdown
5. Avatar setup wizard
6. Settings screen
7. Analytics/stats screen
8. (Optional) Onboarding flow

**Graphics:**
- Feature graphic (1024x500px)
- App icon (512x512px)
- Promo video (optional, 30-120 seconds)

**Text Content:**
- App title: "FocusMother - AI Screen Time Coach"
- Short description (80 chars)
- Full description (4000 chars)
- Keywords/tags
- Category: Health & Fitness / Productivity
- Content rating: PEGI 12+ (AI interaction)

**Files to Create:**
- `play_store/LISTING.md`
- `play_store/SHORT_DESCRIPTION.txt`
- `play_store/FULL_DESCRIPTION.md`
- `play_store/screenshots/` (directory with images)
- `play_store/graphics/` (feature graphic, icon)

---

#### 4.3 Configure Play Console
**Status:** Not Started
**Time:** 2 hours
**Agent:** Manual (requires Google Play Console access)

**Steps:**
1. Create app listing in Play Console
2. Upload app bundle (AAB format)
3. Configure content rating questionnaire
4. Set up target audience (age restrictions)
5. Create closed testing track
6. Add beta testers email list
7. Configure rollout percentage (start with 10%)
8. Set up opt-in URL for beta

**Documentation:**
- `PLAY_CONSOLE_SETUP.md`

---

### Priority 5: Production API Infrastructure (Future - Optional)

#### 5.1 Backend Proxy Setup (Optional for Beta)
**Status:** Deferred to post-beta
**Time:** 8-10 hours (full backend implementation)
**Agent:** None (complex backend task)

**Current Approach (Beta):**
- Users provide their own Claude API key
- Direct API calls from app to Anthropic
- Rate limiting enforced client-side

**Future Production Approach:**
- Firebase Cloud Functions proxy
- Server-side API key management
- User authentication with Firebase Auth
- Server-side rate limiting (50 req/hour)
- API cost tracking per user
- Premium tier verification

**Reasoning for Deferral:**
- Beta testing can use user-provided API keys
- Backend proxy is complex (8-10 hours minimum)
- Can validate product-market fit first
- Reduces initial infrastructure costs
- Can implement after beta feedback

**Documentation:**
- `BACKEND_PROXY_PLAN.md` (future implementation)

---

## 📊 Phase 8 Timeline

| Day | Tasks | Agent |
|-----|-------|-------|
| **Day 1** | Certificate pins, Release signing, Security review | security-scanner |
| **Day 2** | Privacy policy, Terms of service | Manual |
| **Day 3** | Legal UI integration, Crashlytics setup | None |
| **Day 4** | CI/CD pipeline setup | devops-cicd-architect |
| **Day 5** | Play Store assets creation | Manual |
| **Day 6** | Play Console configuration, Testing | Manual |
| **Day 7** | Final review, Beta deployment | Manual |

---

## ✅ Success Criteria

### Technical Readiness
- [ ] Certificate pins added and tested
- [ ] Release signing configured
- [ ] Crashlytics integrated and tested
- [ ] CI/CD pipeline operational
- [ ] ProGuard mappings uploaded
- [ ] Release build tested on multiple devices

### Legal Compliance
- [ ] Privacy policy complete and accessible
- [ ] Terms of service complete and accessible
- [ ] GDPR compliance documented
- [ ] Data deletion functionality verified
- [ ] User consent flow implemented

### Play Store Readiness
- [ ] 7-8 high-quality screenshots captured
- [ ] Feature graphic created
- [ ] App description written (optimized for ASO)
- [ ] Content rating completed
- [ ] Closed beta track configured
- [ ] Beta testers invited (target: 50 initial testers)

### Security & Quality
- [ ] Final security scan passed (zero CRITICAL/HIGH issues)
- [ ] Manual testing on 3+ devices completed
- [ ] Battery drain < 5% per day verified
- [ ] Memory leaks checked with Profiler
- [ ] Crash-free rate > 99.5% target set

---

## 🚫 Out of Scope for Phase 8

These items are deferred to post-beta based on user feedback:

1. **Backend API Proxy** - Defer until beta validation (8-10 hours)
2. **Multi-language Support** - Start with English only
3. **Tablet Optimization** - Phone-first for beta
4. **Wear OS Companion** - Future consideration
5. **Widget Support** - Post-beta enhancement
6. **Dark Theme Toggle** - Future UI polish
7. **Backup/Restore** - Post-beta feature
8. **Export Data** - CSV export post-beta

---

## 📈 Beta Testing Goals

### User Acquisition (Week 1-2)
- Target: 50 beta testers
- Sources: Product Hunt, Reddit (r/androidapps, r/nosurf), Friends & Family
- Opt-in link from Play Console

### Metrics to Track
- Onboarding completion rate (Target: >80%)
- Avatar creation success rate (Target: >90%)
- Agreement success rate (Target: >60%)
- Daily active users retention (Target: >40% Day 7)
- Crash-free sessions (Target: >99.5%)
- Average session duration
- Most triggered app categories

### Feedback Collection
- In-app feedback form (Settings → Send Feedback)
- Email: feedback@focusmother.app (to be created)
- Beta tester Discord/Telegram group (optional)
- Weekly feedback surveys

---

## 🔒 Security Checklist (Final Review)

Before beta release:
- [ ] No API keys in source code
- [ ] No secrets in Git history
- [ ] Certificate pinning enabled
- [ ] ProGuard enabled for release
- [ ] Debug logs stripped
- [ ] Backup exclusions verified
- [ ] Network traffic uses HTTPS only
- [ ] Adult content blocklist encrypted
- [ ] Data deletion tested
- [ ] Permissions justified in manifest

---

## 📝 Documentation Deliverables

Phase 8 will produce:
1. `PHASE_8_PLAN.md` - This document
2. `PRIVACY_POLICY.md` - Legal privacy document
3. `TERMS_OF_SERVICE.md` - Legal terms document
4. `KEYSTORE_BACKUP.md` - Keystore management guide
5. `CI_CD_SETUP.md` - DevOps documentation
6. `PLAY_CONSOLE_SETUP.md` - Store setup guide
7. `BETA_TESTING_GUIDE.md` - Beta tester instructions
8. `PHASE_8_COMPLETE.md` - Final completion summary
9. `BACKEND_PROXY_PLAN.md` - Future backend implementation

---

## 🎯 Next Immediate Actions

**Today (Priority Order):**
1. ✅ Create Phase 8 plan (this document)
2. ⏳ Add certificate pins to network security config
3. ⏳ Set up release signing configuration
4. ⏳ Create privacy policy and terms of service

**Tomorrow:**
5. Implement legal document display in app
6. Set up Firebase Crashlytics
7. Use devops-cicd-architect agent for CI/CD pipeline

**This Week:**
8. Create Play Store assets
9. Configure Play Console for closed beta
10. Deploy beta and invite first testers

---

**Phase 8 Status:** 🚀 **READY TO BEGIN**
**Estimated Completion:** 7 days
**Production Readiness After Phase 8:** 100%
