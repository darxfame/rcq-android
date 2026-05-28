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
public final class StoryViewerViewModel_Factory implements Factory<StoryViewerViewModel> {
  private final Provider<StoryRepository> storyRepositoryProvider;

  public StoryViewerViewModel_Factory(Provider<StoryRepository> storyRepositoryProvider) {
    this.storyRepositoryProvider = storyRepositoryProvider;
  }

  @Override
  public StoryViewerViewModel get() {
    return newInstance(storyRepositoryProvider.get());
  }

  public static StoryViewerViewModel_Factory create(
      Provider<StoryRepository> storyRepositoryProvider) {
    return new StoryViewerViewModel_Factory(storyRepositoryProvider);
  }

  public static StoryViewerViewModel newInstance(StoryRepository storyRepository) {
    return new StoryViewerViewModel(storyRepository);
  }
}
