package com.rcq.messenger.crypto;

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
public final class CryptoService_Factory implements Factory<CryptoService> {
  private final Provider<SessionManager> sessionManagerProvider;

  private final Provider<SignalKeyStore> keyStoreProvider;

  public CryptoService_Factory(Provider<SessionManager> sessionManagerProvider,
      Provider<SignalKeyStore> keyStoreProvider) {
    this.sessionManagerProvider = sessionManagerProvider;
    this.keyStoreProvider = keyStoreProvider;
  }

  @Override
  public CryptoService get() {
    return newInstance(sessionManagerProvider.get(), keyStoreProvider.get());
  }

  public static CryptoService_Factory create(Provider<SessionManager> sessionManagerProvider,
      Provider<SignalKeyStore> keyStoreProvider) {
    return new CryptoService_Factory(sessionManagerProvider, keyStoreProvider);
  }

  public static CryptoService newInstance(SessionManager sessionManager, SignalKeyStore keyStore) {
    return new CryptoService(sessionManager, keyStore);
  }
}
