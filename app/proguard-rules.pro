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
# حماية مكتبة FFmpeg وملحقاتها
-keep class com.arthenica.ffmpegkit.** { *; }
-keep class dev.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**
-dontwarn dev.ffmpegkit.**

# حماية مكتبة SmartException (التي يشتكي منها البناء)
-keep class com.arthenica.smartexception.** { *; }
-dontwarn com.arthenica.smartexception.**

# حماية SmartException Java
-keep class com.arthenica.smartexception.java.** { *; }
-dontwarn com.arthenica.smartexception.java.**