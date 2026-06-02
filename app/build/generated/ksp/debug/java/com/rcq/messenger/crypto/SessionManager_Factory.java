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
public final class SessionManager_Factory implements Factory<SessionManager> {
  private final Provider<SignalKeyStore> signalKeyStoreProvider;

  public SessionManager_Factory(Provider<SignalKeyStore> signalKeyStoreProvider) {
    this.signalKeyStoreProvider = signalKeyStoreProvider;
  }

  @Override
  public SessionManager get() {
    return newInstance(signalKeyStoreProvider.get());
  }

  public static SessionManager_Factory create(Provider<SignalKeyStore> signalKeyStoreProvider) {
    return new SessionManager_Factory(signalKeyStoreProvider);
  }

  public static SessionManager newInstance(SignalKeyStore signalKeyStore) {
    return new SessionManager(signalKeyStore);
  }
}
