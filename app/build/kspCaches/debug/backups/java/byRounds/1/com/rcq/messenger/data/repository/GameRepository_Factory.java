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
public final class GameRepository_Factory implements Factory<GameRepository> {
  private final Provider<RCQApiService> apiProvider;

  public GameRepository_Factory(Provider<RCQApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public GameRepository get() {
    return newInstance(apiProvider.get());
  }

  public static GameRepository_Factory create(Provider<RCQApiService> apiProvider) {
    return new GameRepository_Factory(apiProvider);
  }

  public static GameRepository newInstance(RCQApiService api) {
    return new GameRepository(api);
  }
}
