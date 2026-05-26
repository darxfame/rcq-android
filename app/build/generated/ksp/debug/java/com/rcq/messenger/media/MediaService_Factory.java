package com.rcq.messenger.media;

import android.content.Context;
import com.rcq.messenger.crypto.CryptoService;
import com.rcq.messenger.data.api.RCQApiService;
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
public final class MediaService_Factory implements Factory<MediaService> {
  private final Provider<Context> contextProvider;

  private final Provider<RCQApiService> apiServiceProvider;

  private final Provider<CryptoService> cryptoServiceProvider;

  public MediaService_Factory(Provider<Context> contextProvider,
      Provider<RCQApiService> apiServiceProvider, Provider<CryptoService> cryptoServiceProvider) {
    this.contextProvider = contextProvider;
    this.apiServiceProvider = apiServiceProvider;
    this.cryptoServiceProvider = cryptoServiceProvider;
  }

  @Override
  public MediaService get() {
    return newInstance(contextProvider.get(), apiServiceProvider.get(), cryptoServiceProvider.get());
  }

  public static MediaService_Factory create(Provider<Context> contextProvider,
      Provider<RCQApiService> apiServiceProvider, Provider<CryptoService> cryptoServiceProvider) {
    return new MediaService_Factory(contextProvider, apiServiceProvider, cryptoServiceProvider);
  }

  public static MediaService newInstance(Context context, RCQApiService apiService,
      CryptoService cryptoService) {
    return new MediaService(context, apiService, cryptoService);
  }
}
