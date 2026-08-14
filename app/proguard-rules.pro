# Session Tracks ProGuard/R8 rules

# Keep line numbers for readable crash reports, hide original file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Gson -------------------------------------------------------------------
# Gson uses reflection over model classes for backup import/export.
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken

# Backup DTOs are serialized/deserialized by name: keep their fields intact.
-keep class com.sessiontracks.app.data.backup.** { *; }
-keep class com.sessiontracks.app.data.model.** { *; }
-keep class com.sessiontracks.app.data.entity.** { *; }

# --- Room -------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# --- AndroidX / Material ----------------------------------------------------
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Keep enum values used by Gson / bundles.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelables
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
