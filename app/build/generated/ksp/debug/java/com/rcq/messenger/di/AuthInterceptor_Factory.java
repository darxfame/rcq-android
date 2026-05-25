package com.rcq.messenger.di;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
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
public final class AuthInterceptor_Factory implements Factory<AuthInterceptor> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public AuthInterceptor_Factory(Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public AuthInterceptor get() {
    return newInstance(dataStoreProvider.get());
  }

  public static AuthInterceptor_Factory create(Provider<DataStore<Preferences>> dataStoreProvider) {
    return new AuthInterceptor_Factory(dataStoreProvider);
  }

  public static AuthInterceptor newInstance(DataStore<Preferences> dataStore) {
    return new AuthInterceptor(dataStore);
  }
}
