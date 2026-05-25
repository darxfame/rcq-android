package com.rcq.messenger.ui.contacts;

import com.rcq.messenger.data.repository.ContactRepository;
import com.rcq.messenger.data.websocket.WebSocketService;
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
public final class PendingRequestsViewModel_Factory implements Factory<PendingRequestsViewModel> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<WebSocketService> webSocketServiceProvider;

  public PendingRequestsViewModel_Factory(Provider<ContactRepository> contactRepositoryProvider,
      Provider<WebSocketService> webSocketServiceProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.webSocketServiceProvider = webSocketServiceProvider;
  }

  @Override
  public PendingRequestsViewModel get() {
    return newInstance(contactRepositoryProvider.get(), webSocketServiceProvider.get());
  }

  public static PendingRequestsViewModel_Factory create(
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<WebSocketService> webSocketServiceProvider) {
    return new PendingRequestsViewModel_Factory(contactRepositoryProvider, webSocketServiceProvider);
  }

  public static PendingRequestsViewModel newInstance(ContactRepository contactRepository,
      WebSocketService webSocketService) {
    return new PendingRequestsViewModel(contactRepository, webSocketService);
  }
}
