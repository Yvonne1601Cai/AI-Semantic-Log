package com.example.yvonnecai.core.network.di

import com.example.yvonnecai.core.network.LogApi
import com.example.yvonnecai.core.network.LogEndpoint
import com.example.yvonnecai.core.network.LogRemoteDataSource
import com.example.yvonnecai.core.network.RetrofitLogRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Marks the dispatcher used for blocking network reads and decoding. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Provides the networking stack: JSON, OkHttp, Retrofit and [LogApi]. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // The backend may add fields; ignore what we do not model.
        ignoreUnknownKeys = true
        // Tolerate `null` where a DTO expects a non-null default.
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(LogEndpoint.PLACEHOLDER_BASE_URL)
            .client(client)
            .build()

    @Provides
    @Singleton
    fun provideLogApi(retrofit: Retrofit): LogApi = retrofit.create(LogApi::class.java)
}

/** Binds the network contract to its Retrofit implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingsModule {

    @Binds
    abstract fun bindRemoteDataSource(impl: RetrofitLogRemoteDataSource): LogRemoteDataSource
}
