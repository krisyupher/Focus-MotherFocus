#!/usr/bin/env kotlin

/**
 * Script to generate encrypted blocklist file for testing.
 *
 * This script creates a dummy encrypted blocklist containing test package names.
 * In production, this would contain actual adult content app package names.
 *
 * Usage: Run this script from Android Studio or command line to generate
 * app/src/main/res/raw/blocklist.enc
 *
 * Note: This is a placeholder script. The actual encryption would require
 * Android KeyStore which is only available on Android devices. For testing,
 * we'll create a simple text file that AdultContentManager can fall back to.
 */

fun main() {
    println("Generating dummy encrypted blocklist...")

    val testPackages = listOf(
        "com.example.adult.test1",
        "com.example.adult.test2",
        "com.example.adult.test3",
        "com.test.adult.placeholder1",
        "com.test.adult.placeholder2"
    )

    val content = testPackages.joinToString("\n")

    println("Blocklist content:")
    println(content)

    println("\nIn production, this would be encrypted using BlocklistEncryption.")
    println("For development/testing, AdultContentManager falls back to AppCategorySeedData.ADULT_CONTENT")
    println("\nTo create actual encrypted blocklist:")
    println("1. Run this on an Android device/emulator")
    println("2. Use BlocklistEncryption.encrypt(testPackages)")
    println("3. Save the ByteArray to res/raw/blocklist.enc")
}

main()
