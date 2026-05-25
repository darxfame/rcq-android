package com.rcq.messenger.di;

import com.rcq.messenger.data.db.GroupDao;
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
public final class AppModule_ProvideGroupDaoFactory implements Factory<GroupDao> {
  private final Provider<RCQDatabase> databaseProvider;

  public AppModule_ProvideGroupDaoFactory(Provider<RCQDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public GroupDao get() {
    return provideGroupDao(databaseProvider.get());
  }

  public static AppModule_ProvideGroupDaoFactory create(Provider<RCQDatabase> databaseProvider) {
    return new AppModule_ProvideGroupDaoFactory(databaseProvider);
  }

  public static GroupDao provideGroupDao(RCQDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGroupDao(database));
  }
}
