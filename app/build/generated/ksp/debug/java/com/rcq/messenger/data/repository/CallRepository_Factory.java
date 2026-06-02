package com.rcq.messenger.data.repository;

import com.rcq.messenger.data.api.RCQApiService;
import com.rcq.messenger.data.db.CallDao;
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
public final class CallRepository_Factory implements Factory<CallRepository> {
  private final Provider<RCQApiService> apiProvider;

  private final Provider<CallDao> callDaoProvider;

  public CallRepository_Factory(Provider<RCQApiService> apiProvider,
      Provider<CallDao> callDaoProvider) {
    this.apiProvider = apiProvider;
    this.callDaoProvider = callDaoProvider;
  }

  @Override
  public CallRepository get() {
    return newInstance(apiProvider.get(), callDaoProvider.get());
  }

  public static CallRepository_Factory create(Provider<RCQApiService> apiProvider,
      Provider<CallDao> callDaoProvider) {
    return new CallRepository_Factory(apiProvider, callDaoProvider);
  }

  public static CallRepository newInstance(RCQApiService api, CallDao callDao) {
    return new CallRepository(api, callDao);
  }
}
