package com.rcq.messenger.data.repository;

import com.rcq.messenger.crypto.CryptoService;
import com.rcq.messenger.data.api.RCQApiService;
import com.rcq.messenger.data.db.ChatDao;
import com.rcq.messenger.data.db.ContactDao;
import com.rcq.messenger.data.db.GroupDao;
import com.rcq.messenger.data.db.MessageDao;
import com.rcq.messenger.data.db.PendingOutboxDao;
import com.rcq.messenger.data.websocket.WebSocketService;
import com.rcq.messenger.service.NotificationHelper;
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

  private final Provider<ContactDao> contactDaoProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<GroupDao> groupDaoProvider;

  private final Provider<WebSocketService> webSocketServiceProvider;

  private final Provider<CryptoService> cryptoServiceProvider;

  private final Provider<NotificationHelper> notificationHelperProvider;

  private final Provider<PendingOutboxDao> outboxDaoProvider;

  public ChatRepository_Factory(Provider<RCQApiService> apiProvider,
      Provider<ChatDao> chatDaoProvider, Provider<ContactDao> contactDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<GroupDao> groupDaoProvider,
      Provider<WebSocketService> webSocketServiceProvider,
      Provider<CryptoService> cryptoServiceProvider,
      Provider<NotificationHelper> notificationHelperProvider,
      Provider<PendingOutboxDao> outboxDaoProvider) {
    this.apiProvider = apiProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.contactDaoProvider = contactDaoProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.groupDaoProvider = groupDaoProvider;
    this.webSocketServiceProvider = webSocketServiceProvider;
    this.cryptoServiceProvider = cryptoServiceProvider;
    this.notificationHelperProvider = notificationHelperProvider;
    this.outboxDaoProvider = outboxDaoProvider;
  }

  @Override
  public ChatRepository get() {
    return newInstance(apiProvider.get(), chatDaoProvider.get(), contactDaoProvider.get(), messageDaoProvider.get(), groupDaoProvider.get(), webSocketServiceProvider.get(), cryptoServiceProvider.get(), notificationHelperProvider.get(), outboxDaoProvider.get());
  }

  public static ChatRepository_Factory create(Provider<RCQApiService> apiProvider,
      Provider<ChatDao> chatDaoProvider, Provider<ContactDao> contactDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<GroupDao> groupDaoProvider,
      Provider<WebSocketService> webSocketServiceProvider,
      Provider<CryptoService> cryptoServiceProvider,
      Provider<NotificationHelper> notificationHelperProvider,
      Provider<PendingOutboxDao> outboxDaoProvider) {
    return new ChatRepository_Factory(apiProvider, chatDaoProvider, contactDaoProvider, messageDaoProvider, groupDaoProvider, webSocketServiceProvider, cryptoServiceProvider, notificationHelperProvider, outboxDaoProvider);
  }

  public static ChatRepository newInstance(RCQApiService api, ChatDao chatDao,
      ContactDao contactDao, MessageDao messageDao, GroupDao groupDao,
      WebSocketService webSocketService, CryptoService cryptoService,
      NotificationHelper notificationHelper, PendingOutboxDao outboxDao) {
    return new ChatRepository(api, chatDao, contactDao, messageDao, groupDao, webSocketService, cryptoService, notificationHelper, outboxDao);
  }
}
