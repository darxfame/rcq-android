package com.rcq.messenger.ui.chat;

import com.rcq.messenger.data.repository.ChatRepository;
import com.rcq.messenger.data.repository.UserRepository;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public ChatViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(chatRepositoryProvider.get(), userRepositoryProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new ChatViewModel_Factory(chatRepositoryProvider, userRepositoryProvider);
  }

  public static ChatViewModel newInstance(ChatRepository chatRepository,
      UserRepository userRepository) {
    return new ChatViewModel(chatRepository, userRepository);
  }
}
