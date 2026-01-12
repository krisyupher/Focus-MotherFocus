# Keystore Setup Guide

This guide covers creating, managing, and securing your Android app signing keystore for the FocusMother application.

---

## Table of Contents

1. [Overview](#overview)
2. [Generate Release Keystore](#generate-release-keystore)
3. [Keystore Information](#keystore-information)
4. [Configure Gradle](#configure-gradle)
5. [GitHub Secrets Setup](#github-secrets-setup)
6. [Backup Procedures](#backup-procedures)
7. [Certificate Pinning](#certificate-pinning)
8. [Troubleshooting](#troubleshooting)

---

## Overview

Android requires all APKs to be digitally signed with a certificate before they can be installed. The keystore contains:

- **Private key**: Used to sign your app (KEEP SECRET!)
- **Certificate**: Public key that identifies the app publisher

**CRITICAL WARNINGS:**

- If you lose your keystore, you **CANNOT** update your app on Google Play
- Store backups in **multiple secure locations**
- Never commit the keystore to Git
- Use strong passwords (minimum 12 characters)
- Keep passwords in a password manager (LastPass, 1Password, Bitwarden)

---

## Generate Release Keystore

### Step 1: Generate the Keystore

Open a terminal and run:

```bash
keytool -genkey -v -keystore focusmother-release.keystore \
  -alias focusmother \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_KEYSTORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD
```

**Interactive Mode (Recommended):**

```bash
keytool -genkey -v -keystore focusmother-release.keystore \
  -alias focusmother \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

You will be prompted for:

```
Enter keystore password: [Enter a strong password]
Re-enter new password: [Confirm password]

What is your first and last name?
  [Unknown]: Your Name

What is the name of your organizational unit?
  [Unknown]: Development

What is the name of your organization?
  [Unknown]: FocusMother

What is the name of your City or Locality?
  [Unknown]: Your City

What is the name of your State or Province?
  [Unknown]: Your State

What is the two-letter country code for this unit?
  [Unknown]: US

Is CN=Your Name, OU=Development, O=FocusMother, L=Your City, ST=Your State, C=US correct?
  [no]: yes

Enter key password for <focusmother>
  (RETURN if same as keystore password): [Press Enter or use different password]
```

### Step 2: Verify Keystore Creation

```bash
# List keystore contents
keytool -list -v -keystore focusmother-release.keystore

# You should see:
# - Alias name: focusmother
# - Creation date
# - Certificate fingerprints (SHA1, SHA256)
# - Validity: Valid until [date 27 years from now]
```

### Step 3: Save Important Information

Create a secure note with:

```
FOCUSMOTHER ANDROID KEYSTORE INFORMATION
========================================

Keystore Filename: focusmother-release.keystore
Keystore Password: [YOUR_KEYSTORE_PASSWORD]
Key Alias: focusmother
Key Password: [YOUR_KEY_PASSWORD]

Generated Date: [DATE]
Valid Until: [DATE]

SHA1 Fingerprint: [Copy from keytool output]
SHA256 Fingerprint: [Copy from keytool output]

Backup Locations:
1. [Primary backup location]
2. [Secondary backup location]
3. [Encrypted cloud backup]
```

Store this note in:
- Password manager (1Password, LastPass, Bitwarden)
- Encrypted USB drive
- Secure cloud storage (encrypted)

---

## Keystore Information

### Recommended Keystore Parameters

| Parameter | Recommended Value | Explanation |
|-----------|------------------|-------------|
| `keyalg` | RSA | Industry standard algorithm |
| `keysize` | 2048 | Minimum recommended size (4096 for higher security) |
| `validity` | 10000 days | ~27 years (must outlive app lifetime) |
| `alias` | focusmother | Easy to remember identifier |
| `password length` | 12+ characters | Strong security requirement |

### Password Requirements

**Keystore Password:**
- Minimum 12 characters
- Include uppercase, lowercase, numbers, symbols
- NOT the same as your GitHub password
- NOT shared with other projects

**Example Strong Password:**
```
FocM!2026#Secure$Keystore
```

Use a password generator for maximum security.

---

## Configure Gradle

### Step 1: Create keystore.properties

Create `keystore.properties` in the project root (same level as `build.gradle.kts`):

```properties
storeFile=../focusmother-release.keystore
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=focusmother
keyPassword=YOUR_KEY_PASSWORD
```

**CRITICAL: This file must NEVER be committed to Git!**

Verify it's in `.gitignore`:

```bash
grep "keystore.properties" .gitignore
# Should return: keystore.properties
```

### Step 2: Update app/build.gradle.kts

Add signing configuration:

```kotlin
import java.util.Properties
import java.io.FileInputStream

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    // ... existing config ...

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
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

### Step 3: Test Local Signing

```bash
# Build signed release APK
./gradlew assembleRelease

# Verify signature
$ANDROID_HOME/build-tools/*/apksigner verify --verbose \
  app/build/outputs/apk/release/app-release.apk

# Should output:
# Verified using v1 scheme (JAR signing): true
# Verified using v2 scheme (APK Signature Scheme v2): true
```

---

## GitHub Secrets Setup

### Step 1: Encode Keystore to Base64

The keystore must be base64 encoded to store as a GitHub Secret.

**On Linux/macOS:**
```bash
base64 -i focusmother-release.keystore -o keystore.base64.txt
```

**On Windows (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("focusmother-release.keystore")) | Out-File keystore.base64.txt
```

**On Windows (Git Bash):**
```bash
base64 -w 0 focusmother-release.keystore > keystore.base64.txt
```

### Step 2: Add GitHub Secrets

1. Go to your GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**

Add these 4 secrets:

| Secret Name | Value Source |
|-------------|-------------|
| `KEYSTORE_FILE` | Contents of `keystore.base64.txt` |
| `KEYSTORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | `focusmother` |
| `KEY_PASSWORD` | Your key password |

**For each secret:**
- Click **New repository secret**
- Enter the name (exactly as shown above)
- Paste the value
- Click **Add secret**

### Step 3: Verify Secrets

After adding all secrets:

1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Verify you see 4 secrets:
   - KEYSTORE_FILE
   - KEYSTORE_PASSWORD
   - KEY_ALIAS
   - KEY_PASSWORD

**Security Note:** Once added, you cannot view secret values. If you lose them, you must update the secrets.

### Step 4: Test CI Build

Trigger a release workflow to test:

```bash
# Create a test tag
git tag -a v0.0.1-test -m "Test release signing"
git push origin v0.0.1-test

# Watch the workflow in GitHub Actions
# Verify no keystore errors
# Verify APK is signed correctly
```

---

## Backup Procedures

### Critical Backups Checklist

- [ ] Original keystore file (`focusmother-release.keystore`)
- [ ] Keystore passwords (in password manager)
- [ ] SHA1 and SHA256 fingerprints
- [ ] `keystore.properties` file (optional, can be recreated)

### Recommended Backup Locations

#### 1. Primary Backup (Physical)

```bash
# Copy to encrypted USB drive
cp focusmother-release.keystore /Volumes/USB_DRIVE/keystore-backup/

# Or create encrypted archive
zip -e focusmother-keystore-backup.zip focusmother-release.keystore
# Enter encryption password
```

Store USB drive in:
- Fireproof safe
- Safety deposit box
- Secure office location

#### 2. Secondary Backup (Cloud - Encrypted)

**Option A: Encrypted Cloud Storage**

Use services with zero-knowledge encryption:
- Tresorit
- SpiderOak
- ProtonDrive

**Option B: Manual Encryption**

```bash
# Encrypt with GPG
gpg -c focusmother-release.keystore
# Creates: focusmother-release.keystore.gpg

# Upload to Google Drive, Dropbox, etc.
```

To decrypt:
```bash
gpg focusmother-release.keystore.gpg
```

#### 3. Team Backup (for Organizations)

For team access:
- Use 1Password Teams or LastPass Enterprise
- Store keystore as "Secure File" attachment
- Restrict access to authorized team members only
- Enable 2FA on password manager

### Backup Verification

Test backups quarterly:

```bash
# Copy backup to new location
cp /path/to/backup/focusmother-release.keystore ./test-keystore.jks

# Verify it works
keytool -list -v -keystore test-keystore.jks

# Delete test copy
rm test-keystore.jks
```

---

## Certificate Pinning

For added security, pin your app's signing certificate in your app.

### Extract Certificate Pins

```bash
# Get SHA256 fingerprint of your certificate
keytool -list -v -keystore focusmother-release.keystore | grep SHA256

# Output: SHA256: AA:BB:CC:DD:EE:FF:... (32 hex pairs)
```

### Add to network_security_config.xml

Edit `app/src/main/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Certificate pinning for app signing -->
    <base-config>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" overridePins="false" />
        </trust-anchors>
    </base-config>

    <!-- Your app's signing certificate (optional but recommended) -->
    <!--
    <pin-set>
        <pin digest="SHA-256">YOUR_CERT_SHA256_PIN_HERE</pin>
    </pin-set>
    -->
</network-security-config>
```

---

## Troubleshooting

### Issue 1: "Keystore was tampered with, or password was incorrect"

**Cause:** Wrong password or corrupted keystore

**Solution:**
1. Verify password is correct (check password manager)
2. Try backup keystore if primary is corrupted
3. If all else fails and you haven't published yet, generate new keystore

### Issue 2: "Failed to load keystore: focusmother-release.keystore"

**Cause:** Keystore file path is incorrect

**Solution:**
Check `keystore.properties`:
```properties
# Path should be relative to app/ directory
storeFile=../focusmother-release.keystore

# Or absolute path
storeFile=/full/path/to/focusmother-release.keystore
```

### Issue 3: CI Build - "Keystore file not found"

**Cause:** `KEYSTORE_FILE` secret not configured or incorrectly encoded

**Solution:**
1. Verify secret is added in GitHub Settings → Secrets
2. Re-encode keystore:
   ```bash
   base64 -w 0 focusmother-release.keystore > keystore.base64.txt
   ```
3. Update `KEYSTORE_FILE` secret with new contents

### Issue 4: "APK signature verification failed"

**Cause:** Signing configuration mismatch

**Solution:**
1. Verify all 4 secrets are set correctly
2. Check keystore alias matches: `focusmother`
3. Test signing locally first:
   ```bash
   ./gradlew clean assembleRelease
   ```

### Issue 5: "Cannot recover key" when building

**Cause:** Key password is different from keystore password and not provided

**Solution:**
Ensure `keyPassword` is set in `keystore.properties` or GitHub Secrets

---

## Key Rotation (Advanced)

If you need to rotate your keystore (security breach, compromise):

**WARNING:** You **CANNOT** change the keystore for an existing app on Google Play!

For new apps or major version changes:

```bash
# Generate new keystore
keytool -genkey -v -keystore focusmother-release-v2.keystore \
  -alias focusmother-v2 \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Update keystore.properties
# Update GitHub Secrets
# Build and test

# For Play Store: You must create a NEW app listing
```

---

## Quick Reference

### Generate Keystore
```bash
keytool -genkey -v -keystore focusmother-release.keystore \
  -alias focusmother -keyalg RSA -keysize 2048 -validity 10000
```

### View Keystore Info
```bash
keytool -list -v -keystore focusmother-release.keystore
```

### Encode for GitHub
```bash
base64 -w 0 focusmother-release.keystore > keystore.base64.txt
```

### Verify APK Signature
```bash
$ANDROID_HOME/build-tools/*/apksigner verify --verbose app-release.apk
```

### Build Signed Release
```bash
./gradlew assembleRelease
```

---

## Security Best Practices

1. **Never** commit keystore or passwords to Git
2. **Always** use strong, unique passwords
3. **Store** backups in multiple secure locations
4. **Test** backups quarterly
5. **Encrypt** keystores before cloud storage
6. **Use** password manager for credentials
7. **Limit** access to keystore (need-to-know basis)
8. **Monitor** access logs if using team password manager
9. **Rotate** passwords if team members leave
10. **Document** keystore creation date and validity

---

## Emergency Contact

If keystore is lost or compromised:

1. **Check all backup locations immediately**
2. **Review Git history** (if accidentally committed, contact GitHub Support)
3. **For compromised keystore:**
   - Revoke Play Store publish access
   - Generate new keystore
   - Prepare for new app submission

**Prevention is critical - maintain multiple backups!**

---

**Document Version:** 1.0.0
**Last Updated:** 2026-01-11
**Next Review:** 2026-04-11 (Quarterly)
