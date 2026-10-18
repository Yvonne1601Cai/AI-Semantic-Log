package com.example.yvonnecai.core.data.di

import com.example.yvonnecai.core.data.LogRepository
import com.example.yvonnecai.core.data.LogRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/** Marks the dispatcher used for CPU-bound work (mapping, indexing, filtering), so tests can swap it. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** Binds the repository contract and provides the CPU dispatcher. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindLogRepository(impl: LogRepositoryImpl): LogRepository

    companion object {
        @Provides
        @DefaultDispatcher
        fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    }
}
