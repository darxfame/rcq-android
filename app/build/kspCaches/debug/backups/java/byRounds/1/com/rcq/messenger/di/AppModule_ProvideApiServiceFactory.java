package com.rcq.messenger.di;

import com.rcq.messenger.data.api.RCQApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
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
    "KotlinInternalInJava"
})
public final class AppModule_ProvideApiServiceFactory implements Factory<RCQApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public AppModule_ProvideApiServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public RCQApiService get() {
    return provideApiService(retrofitProvider.get());
  }

  public static AppModule_ProvideApiServiceFactory create(Provider<Retrofit> retrofitProvider) {
    return new AppModule_ProvideApiServiceFactory(retrofitProvider);
  }

  public static RCQApiService provideApiService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideApiService(retrofit));
  }
}
