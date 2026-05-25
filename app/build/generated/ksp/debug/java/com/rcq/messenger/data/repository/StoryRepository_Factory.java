package com.rcq.messenger.data.repository;

import com.rcq.messenger.data.api.RCQApiService;
import com.rcq.messenger.data.db.StoryDao;
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
public final class StoryRepository_Factory implements Factory<StoryRepository> {
  private final Provider<RCQApiService> apiProvider;

  private final Provider<StoryDao> storyDaoProvider;

  public StoryRepository_Factory(Provider<RCQApiService> apiProvider,
      Provider<StoryDao> storyDaoProvider) {
    this.apiProvider = apiProvider;
    this.storyDaoProvider = storyDaoProvider;
  }

  @Override
  public StoryRepository get() {
    return newInstance(apiProvider.get(), storyDaoProvider.get());
  }

  public static StoryRepository_Factory create(Provider<RCQApiService> apiProvider,
      Provider<StoryDao> storyDaoProvider) {
    return new StoryRepository_Factory(apiProvider, storyDaoProvider);
  }

  public static StoryRepository newInstance(RCQApiService api, StoryDao storyDao) {
    return new StoryRepository(api, storyDao);
  }
}
