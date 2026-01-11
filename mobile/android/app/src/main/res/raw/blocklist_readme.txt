ENCRYPTED BLOCKLIST README
==========================

This directory should contain an encrypted blocklist file named "blocklist.enc"
containing adult content app package names.

For privacy and security reasons, the actual blocklist is encrypted using
AES-256-GCM with a device-specific key stored in Android KeyStore.

PRODUCTION DEPLOYMENT:
----------------------
1. Generate the blocklist on a secure Android device using BlocklistEncryption
2. Encrypt the list of package names
3. Save the encrypted ByteArray to this location as "blocklist.enc"
4. The file will be bundled with the app during build

DEVELOPMENT/TESTING:
--------------------
If blocklist.enc is not present, AdultContentManager automatically falls back
to placeholder data from AppCategorySeedData.ADULT_CONTENT

This ensures the app continues working in development without requiring
the actual encrypted blocklist.

GENERATING ENCRYPTED BLOCKLIST:
--------------------------------
Run the following code on an Android device/emulator:

```kotlin
import com.focusmother.android.util.BlocklistEncryption

val packageNames = listOf(
    // Add actual adult content package names here
    "com.example.package1",
    "com.example.package2"
)

val encrypted = BlocklistEncryption.encrypt(packageNames)

// Save encrypted ByteArray to file
File(context.filesDir, "blocklist.enc").writeBytes(encrypted)

// Then copy the file to app/src/main/res/raw/blocklist.enc
```

SECURITY NOTES:
---------------
- The encryption key is device-specific and cannot be extracted
- Blocklist is decrypted only in memory, never logged or persisted decrypted
- Case-insensitive matching prevents bypass attempts
- Fail-safe: Returns empty list if decryption fails
