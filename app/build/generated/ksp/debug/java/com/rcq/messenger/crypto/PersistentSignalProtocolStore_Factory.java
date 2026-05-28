package com.rcq.messenger.crypto;

import com.rcq.messenger.data.db.SignalKeyDao;
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
public final class PersistentSignalProtocolStore_Factory implements Factory<PersistentSignalProtocolStore> {
  private final Provider<SignalKeyDao> signalKeyDaoProvider;

  public PersistentSignalProtocolStore_Factory(Provider<SignalKeyDao> signalKeyDaoProvider) {
    this.signalKeyDaoProvider = signalKeyDaoProvider;
  }

  @Override
  public PersistentSignalProtocolStore get() {
    return newInstance(signalKeyDaoProvider.get());
  }

  public static PersistentSignalProtocolStore_Factory create(
      Provider<SignalKeyDao> signalKeyDaoProvider) {
    return new PersistentSignalProtocolStore_Factory(signalKeyDaoProvider);
  }

  public static PersistentSignalProtocolStore newInstance(SignalKeyDao signalKeyDao) {
    return new PersistentSignalProtocolStore(signalKeyDao);
  }
}
