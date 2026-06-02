package com.rcq.messenger.ui.calls;

import com.rcq.messenger.call.CallManager;
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

  private final Provider<CallManager> callManagerProvider;

  public CallViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<CallManager> callManagerProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.callManagerProvider = callManagerProvider;
  }

  @Override
  public CallViewModel get() {
    return newInstance(chatRepositoryProvider.get(), callManagerProvider.get());
  }

  public static CallViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<CallManager> callManagerProvider) {
    return new CallViewModel_Factory(chatRepositoryProvider, callManagerProvider);
  }

  public static CallViewModel newInstance(ChatRepository chatRepository, CallManager callManager) {
    return new CallViewModel(chatRepository, callManager);
  }
}
