package com.rcq.messenger.data.websocket;

import android.content.Context;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class WebSocketService_Factory implements Factory<WebSocketService> {
  private final Provider<Context> contextProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public WebSocketService_Factory(Provider<Context> contextProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.contextProvider = contextProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public WebSocketService get() {
    return newInstance(contextProvider.get(), dataStoreProvider.get());
  }

  public static WebSocketService_Factory create(Provider<Context> contextProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new WebSocketService_Factory(contextProvider, dataStoreProvider);
  }

  public static WebSocketService newInstance(Context context, DataStore<Preferences> dataStore) {
    return new WebSocketService(context, dataStore);
  }
}
