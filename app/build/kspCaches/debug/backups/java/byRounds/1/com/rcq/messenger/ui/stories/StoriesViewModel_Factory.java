package com.rcq.messenger.ui.stories;

import com.rcq.messenger.data.repository.StoryRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class StoriesViewModel_Factory implements Factory<StoriesViewModel> {
  private final Provider<StoryRepository> storyRepositoryProvider;

  public StoriesViewModel_Factory(Provider<StoryRepository> storyRepositoryProvider) {
    this.storyRepositoryProvider = storyRepositoryProvider;
  }

  @Override
  public StoriesViewModel get() {
    return newInstance(storyRepositoryProvider.get());
  }

  public static StoriesViewModel_Factory create(Provider<StoryRepository> storyRepositoryProvider) {
    return new StoriesViewModel_Factory(storyRepositoryProvider);
  }

  public static StoriesViewModel newInstance(StoryRepository storyRepository) {
    return new StoriesViewModel(storyRepository);
  }
}
