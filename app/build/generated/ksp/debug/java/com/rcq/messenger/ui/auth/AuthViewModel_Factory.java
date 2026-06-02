package com.rcq.messenger.ui.auth;

import android.content.Context;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.rcq.messenger.crypto.CryptoService;
import com.rcq.messenger.data.api.RCQApiService;
import com.rcq.messenger.data.websocket.WebSocketService;
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
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<RCQApiService> apiProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  private final Provider<Context> contextProvider;

  private final Provider<WebSocketService> webSocketServiceProvider;

  private final Provider<CryptoService> cryptoServiceProvider;

  public AuthViewModel_Factory(Provider<RCQApiService> apiProvider,
      Provider<DataStore<Preferences>> dataStoreProvider, Provider<Context> contextProvider,
      Provider<WebSocketService> webSocketServiceProvider,
      Provider<CryptoService> cryptoServiceProvider) {
    this.apiProvider = apiProvider;
    this.dataStoreProvider = dataStoreProvider;
    this.contextProvider = contextProvider;
    this.webSocketServiceProvider = webSocketServiceProvider;
    this.cryptoServiceProvider = cryptoServiceProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(apiProvider.get(), dataStoreProvider.get(), contextProvider.get(), webSocketServiceProvider.get(), cryptoServiceProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<RCQApiService> apiProvider,
      Provider<DataStore<Preferences>> dataStoreProvider, Provider<Context> contextProvider,
      Provider<WebSocketService> webSocketServiceProvider,
      Provider<CryptoService> cryptoServiceProvider) {
    return new AuthViewModel_Factory(apiProvider, dataStoreProvider, contextProvider, webSocketServiceProvider, cryptoServiceProvider);
  }

  public static AuthViewModel newInstance(RCQApiService api, DataStore<Preferences> dataStore,
      Context context, WebSocketService webSocketService, CryptoService cryptoService) {
    return new AuthViewModel(api, dataStore, context, webSocketService, cryptoService);
  }
}
