package com.rcq.messenger.call;

import android.content.Context;
import com.rcq.messenger.data.websocket.WebSocketService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;

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
public final class CallManager_Factory implements Factory<CallManager> {
  private final Provider<Context> contextProvider;

  private final Provider<WebSocketService> webSocketServiceProvider;

  private final Provider<Json> jsonProvider;

  public CallManager_Factory(Provider<Context> contextProvider,
      Provider<WebSocketService> webSocketServiceProvider, Provider<Json> jsonProvider) {
    this.contextProvider = contextProvider;
    this.webSocketServiceProvider = webSocketServiceProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public CallManager get() {
    return newInstance(contextProvider.get(), webSocketServiceProvider.get(), jsonProvider.get());
  }

  public static CallManager_Factory create(Provider<Context> contextProvider,
      Provider<WebSocketService> webSocketServiceProvider, Provider<Json> jsonProvider) {
    return new CallManager_Factory(contextProvider, webSocketServiceProvider, jsonProvider);
  }

  public static CallManager newInstance(Context context, WebSocketService webSocketService,
      Json json) {
    return new CallManager(context, webSocketService, json);
  }
}
