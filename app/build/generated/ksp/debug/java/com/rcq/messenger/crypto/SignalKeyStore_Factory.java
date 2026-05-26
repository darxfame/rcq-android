package com.rcq.messenger.crypto;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.signal.libsignal.protocol.IdentityKeyPair;

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
public final class SignalKeyStore_Factory implements Factory<SignalKeyStore> {
  private final Provider<IdentityKeyPair> identityKeyPairProvider;

  public SignalKeyStore_Factory(Provider<IdentityKeyPair> identityKeyPairProvider) {
    this.identityKeyPairProvider = identityKeyPairProvider;
  }

  @Override
  public SignalKeyStore get() {
    return newInstance(identityKeyPairProvider.get());
  }

  public static SignalKeyStore_Factory create(Provider<IdentityKeyPair> identityKeyPairProvider) {
    return new SignalKeyStore_Factory(identityKeyPairProvider);
  }

  public static SignalKeyStore newInstance(IdentityKeyPair identityKeyPair) {
    return new SignalKeyStore(identityKeyPair);
  }
}
