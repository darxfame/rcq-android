package com.rcq.messenger.data.ws;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;

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
public final class WebSocketManager_Factory implements Factory<WebSocketManager> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Json> jsonProvider;

  public WebSocketManager_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Json> jsonProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public WebSocketManager get() {
    return newInstance(okHttpClientProvider.get(), jsonProvider.get());
  }

  public static WebSocketManager_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Json> jsonProvider) {
    return new WebSocketManager_Factory(okHttpClientProvider, jsonProvider);
  }

  public static WebSocketManager newInstance(OkHttpClient okHttpClient, Json json) {
    return new WebSocketManager(okHttpClient, json);
  }
}
