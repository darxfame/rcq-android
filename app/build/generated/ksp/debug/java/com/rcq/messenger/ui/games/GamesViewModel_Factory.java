package com.rcq.messenger.ui.games;

import com.rcq.messenger.data.repository.GameRepository;
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
public final class GamesViewModel_Factory implements Factory<GamesViewModel> {
  private final Provider<GameRepository> gameRepositoryProvider;

  public GamesViewModel_Factory(Provider<GameRepository> gameRepositoryProvider) {
    this.gameRepositoryProvider = gameRepositoryProvider;
  }

  @Override
  public GamesViewModel get() {
    return newInstance(gameRepositoryProvider.get());
  }

  public static GamesViewModel_Factory create(Provider<GameRepository> gameRepositoryProvider) {
    return new GamesViewModel_Factory(gameRepositoryProvider);
  }

  public static GamesViewModel newInstance(GameRepository gameRepository) {
    return new GamesViewModel(gameRepository);
  }
}
