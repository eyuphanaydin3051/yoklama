package com.workhourtracker.di

import android.content.Context
import com.workhourtracker.data.local.AppDatabase
import com.workhourtracker.data.local.AttendanceDao
import com.workhourtracker.data.remote.AuthRepository
import com.workhourtracker.data.remote.FirestoreAttendanceSource
import com.workhourtracker.data.repository.AttendanceRepository
import com.workhourtracker.data.repository.AttendanceRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideAttendanceDao(database: AppDatabase): AttendanceDao = database.attendanceDao()
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFirestoreSource(): FirestoreAttendanceSource = FirestoreAttendanceSource.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(@ApplicationContext context: Context): AuthRepository =
        AuthRepository.getInstance(context)

    @Provides
    @Singleton
    fun provideCompanyRepository(@ApplicationContext context: Context): com.workhourtracker.data.remote.CompanyRepository =
        com.workhourtracker.data.remote.CompanyRepository.getInstance(context)

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Provides
    @Singleton
    fun provideAttendanceRepository(
        dao: AttendanceDao,
        firestoreSource: FirestoreAttendanceSource,
        scope: CoroutineScope
    ): AttendanceRepository = AttendanceRepositoryImpl(dao, firestoreSource, scope)
}
