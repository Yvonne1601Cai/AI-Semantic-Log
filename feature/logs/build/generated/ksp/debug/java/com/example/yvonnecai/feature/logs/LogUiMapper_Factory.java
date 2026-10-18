package com.example.yvonnecai.feature.logs;

import com.example.yvonnecai.feature.logs.format.LogFormatters;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
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
public final class LogUiMapper_Factory implements Factory<LogUiMapper> {
  private final Provider<LogFormatters> formattersProvider;

  private LogUiMapper_Factory(Provider<LogFormatters> formattersProvider) {
    this.formattersProvider = formattersProvider;
  }

  @Override
  public LogUiMapper get() {
    return newInstance(formattersProvider.get());
  }

  public static LogUiMapper_Factory create(Provider<LogFormatters> formattersProvider) {
    return new LogUiMapper_Factory(formattersProvider);
  }

  public static LogUiMapper newInstance(LogFormatters formatters) {
    return new LogUiMapper(formatters);
  }
}
