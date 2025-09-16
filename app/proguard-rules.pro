############################
# Retrofit & OkHttp
############################
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

############################
# Gson (keep model fields)
############################
-keep class com.google.gson.** { *; }
-dontwarn sun.misc.Unsafe

# Keep your model classes and their fields
-keep class com.example.pokeverse.model.** { *; }
-keepclassmembers class com.example.pokeverse.model.** {
    <fields>;
}

############################
# Room Database
############################
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

-keep class com.example.pokeverse.data.local.** { *; }   # adjust package
-keep @androidx.room.Dao public interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

############################
# Coroutines
############################
-dontwarn kotlinx.coroutines.**
