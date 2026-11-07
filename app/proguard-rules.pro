# حماية مكتبة FFmpeg
-keep class com.arthenica.ffmpegkit.** { *; }
-keep class dev.ffmpegkit.** { *; }

# حماية مكتبة Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# حماية مكتبة OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**