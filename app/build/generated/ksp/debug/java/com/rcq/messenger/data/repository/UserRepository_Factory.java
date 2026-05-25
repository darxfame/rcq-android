package com.rcq.messenger.data.repository;

import com.rcq.messenger.data.api.RCQApiService;
import com.rcq.messenger.data.db.UserDao;
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
public final class UserRepository_Factory implements Factory<UserRepository> {
  private final Provider<RCQApiService> apiProvider;

  private final Provider<UserDao> userDaoProvider;

  public UserRepository_Factory(Provider<RCQApiService> apiProvider,
      Provider<UserDao> userDaoProvider) {
    this.apiProvider = apiProvider;
    this.userDaoProvider = userDaoProvider;
  }

  @Override
  public UserRepository get() {
    return newInstance(apiProvider.get(), userDaoProvider.get());
  }

  public static UserRepository_Factory create(Provider<RCQApiService> apiProvider,
      Provider<UserDao> userDaoProvider) {
    return new UserRepository_Factory(apiProvider, userDaoProvider);
  }

  public static UserRepository newInstance(RCQApiService api, UserDao userDao) {
    return new UserRepository(api, userDao);
  }
}
