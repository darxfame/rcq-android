package com.rcq.messenger.di;

import com.rcq.messenger.data.ws.WebSocketManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideWebSocketManagerFactory implements Factory<WebSocketManager> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Json> jsonProvider;

  public AppModule_ProvideWebSocketManagerFactory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Json> jsonProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public WebSocketManager get() {
    return provideWebSocketManager(okHttpClientProvider.get(), jsonProvider.get());
  }

  public static AppModule_ProvideWebSocketManagerFactory create(
      Provider<OkHttpClient> okHttpClientProvider, Provider<Json> jsonProvider) {
    return new AppModule_ProvideWebSocketManagerFactory(okHttpClientProvider, jsonProvider);
  }

  public static WebSocketManager provideWebSocketManager(OkHttpClient okHttpClient, Json json) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideWebSocketManager(okHttpClient, json));
  }
}
