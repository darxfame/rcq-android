package com.rcq.messenger.di;

import com.rcq.messenger.data.db.RCQDatabase;
import com.rcq.messenger.data.db.StoryDao;
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
public final class AppModule_ProvideStoryDaoFactory implements Factory<StoryDao> {
  private final Provider<RCQDatabase> databaseProvider;

  public AppModule_ProvideStoryDaoFactory(Provider<RCQDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public StoryDao get() {
    return provideStoryDao(databaseProvider.get());
  }

  public static AppModule_ProvideStoryDaoFactory create(Provider<RCQDatabase> databaseProvider) {
    return new AppModule_ProvideStoryDaoFactory(databaseProvider);
  }

  public static StoryDao provideStoryDao(RCQDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideStoryDao(database));
  }
}
