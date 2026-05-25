# HFLocal - ProGuard Rules for Release Builds

# Kotlin coroutines
-dontwarn kotlinx.coroutines.internal.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.hflocal.shared.**$$serializer { *; }
-keepclassmembers class com.hflocal.shared.** {
    *** Companion;
}
-keepclasseswithmembers class com.hflocal.shared.** { *** Companion; }

# Koin
-keep class io.insert-koin.** { *; }
-dontwarn io.insert-koin.**

# Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# SQLDelight
-keep class app.cash.sqldelight.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep model classes
-keep class com.hflocal.shared.domain.model.** { *; }
