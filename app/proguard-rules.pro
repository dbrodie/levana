# KosherJava
-keep class com.kosherjava.zmanim.** { *; }

# Room entities
-keep class com.levana.app.data.db.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.levana.app.**$$serializer { *; }
-keepclassmembers class com.levana.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.levana.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager workers
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
