package com.rcq.messenger.data.repository;

import com.rcq.messenger.data.api.RCQApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class NearbyRepository_Factory implements Factory<NearbyRepository> {
  private final Provider<RCQApiService> apiProvider;

  public NearbyRepository_Factory(Provider<RCQApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public NearbyRepository get() {
    return newInstance(apiProvider.get());
  }

  public static NearbyRepository_Factory create(Provider<RCQApiService> apiProvider) {
    return new NearbyRepository_Factory(apiProvider);
  }

  public static NearbyRepository newInstance(RCQApiService api) {
    return new NearbyRepository(api);
  }
}
