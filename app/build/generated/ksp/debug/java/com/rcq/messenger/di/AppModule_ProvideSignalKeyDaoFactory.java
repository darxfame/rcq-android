package com.rcq.messenger.di;

import com.rcq.messenger.data.db.RCQDatabase;
import com.rcq.messenger.data.db.SignalKeyDao;
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
public final class AppModule_ProvideSignalKeyDaoFactory implements Factory<SignalKeyDao> {
  private final Provider<RCQDatabase> databaseProvider;

  public AppModule_ProvideSignalKeyDaoFactory(Provider<RCQDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public SignalKeyDao get() {
    return provideSignalKeyDao(databaseProvider.get());
  }

  public static AppModule_ProvideSignalKeyDaoFactory create(
      Provider<RCQDatabase> databaseProvider) {
    return new AppModule_ProvideSignalKeyDaoFactory(databaseProvider);
  }

  public static SignalKeyDao provideSignalKeyDao(RCQDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSignalKeyDao(database));
  }
}
