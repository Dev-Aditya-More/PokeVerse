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

-keep class com.aditya1875.pokeverse.feature.**.data.source.remote.model.** { *; }
-keepclassmembers class com.aditya1875.pokeverse.feature.**.data.source.remote.model.** {
    <fields>;
}
# Prevent Gson from breaking on Kotlin data classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Moshi (you're using moshi-kotlin, needs these)
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class ** {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepnames class * extends com.squareup.moshi.JsonAdapter

# Retrofit response/callback types
-keepattributes Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation