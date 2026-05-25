package com.rcq.messenger.ui.calls;

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
public final class CallViewModel_Factory implements Factory<CallViewModel> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public CallViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public CallViewModel get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static CallViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider) {
    return new CallViewModel_Factory(chatRepositoryProvider);
  }

  public static CallViewModel newInstance(ChatRepository chatRepository) {
    return new CallViewModel(chatRepository);
  }
}
