# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Room
-keep class androidx.room.** { *; }
-keep class com.workhourtracker.data.local.** { *; }
-keep class com.workhourtracker.domain.model.** { *; }

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keep class com.workhourtracker.domain.model.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class com.workhourtracker.di.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keep class com.workhourtracker.data.repository.SyncWorker { *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }

# Kotlinx DateTime
-keep class kotlinx.datetime.** { *; }

# Vico Charts
-keep class io.github.patryk321.vico.** { *; }

# Coil
-keep class coil.** { *; }
-keep class io.coil-kt.** { *; }