package com.rcq.messenger.data.repository;

import com.rcq.messenger.crypto.CryptoService;
import com.rcq.messenger.data.api.RCQApiService;
import com.rcq.messenger.data.db.ChatDao;
import com.rcq.messenger.data.db.MessageDao;
import com.rcq.messenger.data.websocket.WebSocketService;
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
public final class ChatRepository_Factory implements Factory<ChatRepository> {
  private final Provider<RCQApiService> apiProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<WebSocketService> webSocketServiceProvider;

  private final Provider<CryptoService> cryptoServiceProvider;

  public ChatRepository_Factory(Provider<RCQApiService> apiProvider,
      Provider<ChatDao> chatDaoProvider, Provider<MessageDao> messageDaoProvider,
      Provider<WebSocketService> webSocketServiceProvider,
      Provider<CryptoService> cryptoServiceProvider) {
    this.apiProvider = apiProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.webSocketServiceProvider = webSocketServiceProvider;
    this.cryptoServiceProvider = cryptoServiceProvider;
  }

  @Override
  public ChatRepository get() {
    return newInstance(apiProvider.get(), chatDaoProvider.get(), messageDaoProvider.get(), webSocketServiceProvider.get(), cryptoServiceProvider.get());
  }

  public static ChatRepository_Factory create(Provider<RCQApiService> apiProvider,
      Provider<ChatDao> chatDaoProvider, Provider<MessageDao> messageDaoProvider,
      Provider<WebSocketService> webSocketServiceProvider,
      Provider<CryptoService> cryptoServiceProvider) {
    return new ChatRepository_Factory(apiProvider, chatDaoProvider, messageDaoProvider, webSocketServiceProvider, cryptoServiceProvider);
  }

  public static ChatRepository newInstance(RCQApiService api, ChatDao chatDao,
      MessageDao messageDao, WebSocketService webSocketService, CryptoService cryptoService) {
    return new ChatRepository(api, chatDao, messageDao, webSocketService, cryptoService);
  }
}
