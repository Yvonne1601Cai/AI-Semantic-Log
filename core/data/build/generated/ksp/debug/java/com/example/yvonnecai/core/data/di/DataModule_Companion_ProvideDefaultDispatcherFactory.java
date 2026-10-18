package com.example.yvonnecai.core.data.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DataModule_Companion_ProvideDefaultDispatcherFactory implements Factory<CoroutineDispatcher> {
  @Override
  public CoroutineDispatcher get() {
    return provideDefaultDispatcher();
  }

  public static DataModule_Companion_ProvideDefaultDispatcherFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CoroutineDispatcher provideDefaultDispatcher() {
    return Preconditions.checkNotNullFromProvides(DataModule.Companion.provideDefaultDispatcher());
  }

  private static final class InstanceHolder {
    static final DataModule_Companion_ProvideDefaultDispatcherFactory INSTANCE = new DataModule_Companion_ProvideDefaultDispatcherFactory();
  }
}
