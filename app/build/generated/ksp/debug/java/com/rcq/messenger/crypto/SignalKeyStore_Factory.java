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
public final class SignalKeyStore_Factory implements Factory<SignalKeyStore> {
  private final Provider<PersistentSignalProtocolStore> persistentStoreProvider;

  public SignalKeyStore_Factory(Provider<PersistentSignalProtocolStore> persistentStoreProvider) {
    this.persistentStoreProvider = persistentStoreProvider;
  }

  @Override
  public SignalKeyStore get() {
    return newInstance(persistentStoreProvider.get());
  }

  public static SignalKeyStore_Factory create(
      Provider<PersistentSignalProtocolStore> persistentStoreProvider) {
    return new SignalKeyStore_Factory(persistentStoreProvider);
  }

  public static SignalKeyStore newInstance(PersistentSignalProtocolStore persistentStore) {
    return new SignalKeyStore(persistentStore);
  }
}
