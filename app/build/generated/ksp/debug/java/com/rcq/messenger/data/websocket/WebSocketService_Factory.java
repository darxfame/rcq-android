package com.rcq.messenger.data.websocket;

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
public final class WebSocketService_Factory implements Factory<WebSocketService> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public WebSocketService_Factory(Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public WebSocketService get() {
    return newInstance(dataStoreProvider.get());
  }

  public static WebSocketService_Factory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new WebSocketService_Factory(dataStoreProvider);
  }

  public static WebSocketService newInstance(DataStore<Preferences> dataStore) {
    return new WebSocketService(dataStore);
  }
}
