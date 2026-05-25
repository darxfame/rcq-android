package com.rcq.messenger.di;

import com.rcq.messenger.data.db.PetDao;
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
public final class AppModule_ProvidePetDaoFactory implements Factory<PetDao> {
  private final Provider<RCQDatabase> databaseProvider;

  public AppModule_ProvidePetDaoFactory(Provider<RCQDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public PetDao get() {
    return providePetDao(databaseProvider.get());
  }

  public static AppModule_ProvidePetDaoFactory create(Provider<RCQDatabase> databaseProvider) {
    return new AppModule_ProvidePetDaoFactory(databaseProvider);
  }

  public static PetDao providePetDao(RCQDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.providePetDao(database));
  }
}
