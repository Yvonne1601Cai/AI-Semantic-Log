package com.example.yvonnecai.feature.logs.format;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class LogFormatters_Factory implements Factory<LogFormatters> {
  @Override
  public LogFormatters get() {
    return newInstance();
  }

  public static LogFormatters_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static LogFormatters newInstance() {
    return new LogFormatters();
  }

  private static final class InstanceHolder {
    static final LogFormatters_Factory INSTANCE = new LogFormatters_Factory();
  }
}
