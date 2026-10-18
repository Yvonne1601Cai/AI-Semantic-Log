package com.example.yvonnecai.core.data;

import com.example.yvonnecai.core.network.LogRemoteDataSource;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.example.yvonnecai.core.data.di.DefaultDispatcher")
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
public final class LogRepositoryImpl_Factory implements Factory<LogRepositoryImpl> {
  private final Provider<LogRemoteDataSource> remoteProvider;

  private final Provider<CoroutineDispatcher> dispatcherProvider;

  private LogRepositoryImpl_Factory(Provider<LogRemoteDataSource> remoteProvider,
      Provider<CoroutineDispatcher> dispatcherProvider) {
    this.remoteProvider = remoteProvider;
    this.dispatcherProvider = dispatcherProvider;
  }

  @Override
  public LogRepositoryImpl get() {
    return newInstance(remoteProvider.get(), dispatcherProvider.get());
  }

  public static LogRepositoryImpl_Factory create(Provider<LogRemoteDataSource> remoteProvider,
      Provider<CoroutineDispatcher> dispatcherProvider) {
    return new LogRepositoryImpl_Factory(remoteProvider, dispatcherProvider);
  }

  public static LogRepositoryImpl newInstance(LogRemoteDataSource remote,
      CoroutineDispatcher dispatcher) {
    return new LogRepositoryImpl(remote, dispatcher);
  }
}
