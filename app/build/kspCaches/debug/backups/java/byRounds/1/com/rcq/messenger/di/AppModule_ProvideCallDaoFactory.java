package com.rcq.messenger.di;

import com.rcq.messenger.data.db.CallDao;
import com.rcq.messenger.data.db.RCQDatabase;
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
public final class AppModule_ProvideCallDaoFactory implements Factory<CallDao> {
  private final Provider<RCQDatabase> databaseProvider;

  public AppModule_ProvideCallDaoFactory(Provider<RCQDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public CallDao get() {
    return provideCallDao(databaseProvider.get());
  }

  public static AppModule_ProvideCallDaoFactory create(Provider<RCQDatabase> databaseProvider) {
    return new AppModule_ProvideCallDaoFactory(databaseProvider);
  }

  public static CallDao provideCallDao(RCQDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCallDao(database));
  }
}
