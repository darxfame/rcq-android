package com.rcq.messenger.data.repository;

import com.rcq.messenger.data.api.RCQApiService;
import com.rcq.messenger.data.db.PetDao;
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
public final class PetRepository_Factory implements Factory<PetRepository> {
  private final Provider<RCQApiService> apiProvider;

  private final Provider<PetDao> petDaoProvider;

  public PetRepository_Factory(Provider<RCQApiService> apiProvider,
      Provider<PetDao> petDaoProvider) {
    this.apiProvider = apiProvider;
    this.petDaoProvider = petDaoProvider;
  }

  @Override
  public PetRepository get() {
    return newInstance(apiProvider.get(), petDaoProvider.get());
  }

  public static PetRepository_Factory create(Provider<RCQApiService> apiProvider,
      Provider<PetDao> petDaoProvider) {
    return new PetRepository_Factory(apiProvider, petDaoProvider);
  }

  public static PetRepository newInstance(RCQApiService api, PetDao petDao) {
    return new PetRepository(api, petDao);
  }
}
