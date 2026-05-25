package com.rcq.messenger.di;

import com.rcq.messenger.data.db.ContactDao;
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
public final class AppModule_ProvideContactDaoFactory implements Factory<ContactDao> {
  private final Provider<RCQDatabase> databaseProvider;

  public AppModule_ProvideContactDaoFactory(Provider<RCQDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ContactDao get() {
    return provideContactDao(databaseProvider.get());
  }

  public static AppModule_ProvideContactDaoFactory create(Provider<RCQDatabase> databaseProvider) {
    return new AppModule_ProvideContactDaoFactory(databaseProvider);
  }

  public static ContactDao provideContactDao(RCQDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideContactDao(database));
  }
}
