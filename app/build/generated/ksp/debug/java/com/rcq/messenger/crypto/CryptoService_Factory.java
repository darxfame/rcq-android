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

  private final Provider<EciesCrypto> eciesProvider;

  public CryptoService_Factory(Provider<SessionManager> sessionManagerProvider,
      Provider<SignalKeyStore> keyStoreProvider, Provider<EciesCrypto> eciesProvider) {
    this.sessionManagerProvider = sessionManagerProvider;
    this.keyStoreProvider = keyStoreProvider;
    this.eciesProvider = eciesProvider;
  }

  @Override
  public CryptoService get() {
    return newInstance(sessionManagerProvider.get(), keyStoreProvider.get(), eciesProvider.get());
  }

  public static CryptoService_Factory create(Provider<SessionManager> sessionManagerProvider,
      Provider<SignalKeyStore> keyStoreProvider, Provider<EciesCrypto> eciesProvider) {
    return new CryptoService_Factory(sessionManagerProvider, keyStoreProvider, eciesProvider);
  }

  public static CryptoService newInstance(SessionManager sessionManager, SignalKeyStore keyStore,
      EciesCrypto ecies) {
    return new CryptoService(sessionManager, keyStore, ecies);
  }
}
