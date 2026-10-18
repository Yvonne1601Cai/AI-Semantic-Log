package com.example.yvonnecai.core.network;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.serialization.json.Json;

@ScopeMetadata
@QualifierMetadata("com.example.yvonnecai.core.network.di.IoDispatcher")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class RetrofitLogRemoteDataSource_Factory implements Factory<RetrofitLogRemoteDataSource> {
  private final Provider<LogApi> apiProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  private RetrofitLogRemoteDataSource_Factory(Provider<LogApi> apiProvider,
      Provider<Json> jsonProvider, Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.apiProvider = apiProvider;
    this.jsonProvider = jsonProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public RetrofitLogRemoteDataSource get() {
    return newInstance(apiProvider.get(), jsonProvider.get(), ioDispatcherProvider.get());
  }

  public static RetrofitLogRemoteDataSource_Factory create(Provider<LogApi> apiProvider,
      Provider<Json> jsonProvider, Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new RetrofitLogRemoteDataSource_Factory(apiProvider, jsonProvider, ioDispatcherProvider);
  }

  public static RetrofitLogRemoteDataSource newInstance(LogApi api, Json json,
      CoroutineDispatcher ioDispatcher) {
    return new RetrofitLogRemoteDataSource(api, json, ioDispatcher);
  }
}
