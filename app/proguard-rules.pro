# Ktor, the http client under Supabase, updates these fields atomically by name.
-keepclassmembers class io.ktor.** { volatile <fields>; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Optional logging backends that Ktor and OkHttp mention but we never ship.
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
