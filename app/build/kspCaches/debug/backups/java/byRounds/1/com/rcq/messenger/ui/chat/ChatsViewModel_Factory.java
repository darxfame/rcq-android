package com.rcq.messenger.ui.chat;

import com.rcq.messenger.data.repository.ChatRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ChatsViewModel_Factory implements Factory<ChatsViewModel> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public ChatsViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public ChatsViewModel get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static ChatsViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider) {
    return new ChatsViewModel_Factory(chatRepositoryProvider);
  }

  public static ChatsViewModel newInstance(ChatRepository chatRepository) {
    return new ChatsViewModel(chatRepository);
  }
}
