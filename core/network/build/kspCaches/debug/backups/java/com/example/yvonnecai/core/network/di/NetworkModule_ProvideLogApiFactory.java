package com.example.yvonnecai.core.network.di;

import com.example.yvonnecai.core.network.LogApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import retrofit2.Retrofit;

@ScopeMetadata("javax.inject.Singleton")
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
public final class NetworkModule_ProvideLogApiFactory implements Factory<LogApi> {
  private final Provider<Retrofit> retrofitProvider;

  private NetworkModule_ProvideLogApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public LogApi get() {
    return provideLogApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideLogApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideLogApiFactory(retrofitProvider);
  }

  public static LogApi provideLogApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideLogApi(retrofit));
  }
}
