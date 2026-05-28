package com.rcq.messenger.ui.auth;

import android.content.Context;
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
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AccountRecoveryViewModel_Factory implements Factory<AccountRecoveryViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  private final Provider<Context> contextProvider;

  public AccountRecoveryViewModel_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<DataStore<Preferences>> dataStoreProvider, Provider<Context> contextProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.dataStoreProvider = dataStoreProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public AccountRecoveryViewModel get() {
    return newInstance(userRepositoryProvider.get(), dataStoreProvider.get(), contextProvider.get());
  }

  public static AccountRecoveryViewModel_Factory create(
      Provider<UserRepository> userRepositoryProvider,
      Provider<DataStore<Preferences>> dataStoreProvider, Provider<Context> contextProvider) {
    return new AccountRecoveryViewModel_Factory(userRepositoryProvider, dataStoreProvider, contextProvider);
  }

  public static AccountRecoveryViewModel newInstance(UserRepository userRepository,
      DataStore<Preferences> dataStore, Context context) {
    return new AccountRecoveryViewModel(userRepository, dataStore, context);
  }
}
