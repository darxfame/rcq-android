package com.rcq.messenger.di;

import com.rcq.messenger.data.db.RCQDatabase;
import com.rcq.messenger.data.db.UserDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
    "KotlinInternalInJava"
})
public final class AppModule_ProvideUserDaoFactory implements Factory<UserDao> {
  private final Provider<RCQDatabase> databaseProvider;

  public AppModule_ProvideUserDaoFactory(Provider<RCQDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public UserDao get() {
    return provideUserDao(databaseProvider.get());
  }

  public static AppModule_ProvideUserDaoFactory create(Provider<RCQDatabase> databaseProvider) {
    return new AppModule_ProvideUserDaoFactory(databaseProvider);
  }

  public static UserDao provideUserDao(RCQDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideUserDao(database));
  }
}
