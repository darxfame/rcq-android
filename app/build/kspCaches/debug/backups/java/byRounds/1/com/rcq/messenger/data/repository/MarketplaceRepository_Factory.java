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
public final class MarketplaceRepository_Factory implements Factory<MarketplaceRepository> {
  private final Provider<RCQApiService> apiProvider;

  public MarketplaceRepository_Factory(Provider<RCQApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public MarketplaceRepository get() {
    return newInstance(apiProvider.get());
  }

  public static MarketplaceRepository_Factory create(Provider<RCQApiService> apiProvider) {
    return new MarketplaceRepository_Factory(apiProvider);
  }

  public static MarketplaceRepository newInstance(RCQApiService api) {
    return new MarketplaceRepository(api);
  }
}
