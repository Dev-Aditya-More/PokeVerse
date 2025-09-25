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

# Keep your actual model classes
-keep class com.aditya1875.pokeverse.data.remote.model.** { *; }
-keepclassmembers class com.aditya1875.pokeverse.data.remote.model.** {
    <fields>;
}

############################
# Room Database
############################
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

-keep class com.aditya1875.pokeverse.data.local.** { *; }
-keep @androidx.room.Dao public interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

############################
# Coroutines
############################
-dontwarn kotlinx.coroutines.**

############################
# Koin DI
############################
-keep class org.koin.** { *; }
-keep class com.aditya1875.pokeverse.di.** { *; }

# Keep Kotlin Flow interfaces
-keep class kotlinx.coroutines.flow.StateFlow { *; }
-keep class kotlinx.coroutines.flow.MutableStateFlow { *; }
