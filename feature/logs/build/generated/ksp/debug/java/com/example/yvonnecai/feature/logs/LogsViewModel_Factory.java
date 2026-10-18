package com.example.yvonnecai.feature.logs;

import com.example.yvonnecai.core.data.LogRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata
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
public final class LogsViewModel_Factory implements Factory<LogsViewModel> {
  private final Provider<LogRepository> repositoryProvider;

  private final Provider<LogUiMapper> mapperProvider;

  private final Provider<CoroutineDispatcher> defaultDispatcherProvider;

  private LogsViewModel_Factory(Provider<LogRepository> repositoryProvider,
      Provider<LogUiMapper> mapperProvider,
      Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    this.repositoryProvider = repositoryProvider;
    this.mapperProvider = mapperProvider;
    this.defaultDispatcherProvider = defaultDispatcherProvider;
  }

  @Override
  public LogsViewModel get() {
    return newInstance(repositoryProvider.get(), mapperProvider.get(), defaultDispatcherProvider.get());
  }

  public static LogsViewModel_Factory create(Provider<LogRepository> repositoryProvider,
      Provider<LogUiMapper> mapperProvider,
      Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    return new LogsViewModel_Factory(repositoryProvider, mapperProvider, defaultDispatcherProvider);
  }

  public static LogsViewModel newInstance(LogRepository repository, LogUiMapper mapper,
      CoroutineDispatcher defaultDispatcher) {
    return new LogsViewModel(repository, mapper, defaultDispatcher);
  }
}
