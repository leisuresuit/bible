# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/leisuresuit/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Minimize only (Shrink), do not obfuscate or optimize
-dontobfuscate

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx Serialization
-keepattributes *Annotation*, Enums
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName *;
}
-keepclassmembers class org.tjc.bible.data.abs.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# App Models
-keep class org.tjc.bible.domain.model.** { *; }
