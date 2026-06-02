package com.rcq.messenger.ui.settings;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.rcq.messenger.data.repository.UserRepository;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public SettingsViewModel_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(userRepositoryProvider.get(), dataStoreProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<UserRepository> userRepositoryProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new SettingsViewModel_Factory(userRepositoryProvider, dataStoreProvider);
  }

  public static SettingsViewModel newInstance(UserRepository userRepository,
      DataStore<Preferences> dataStore) {
    return new SettingsViewModel(userRepository, dataStore);
  }
}
