package com.rcq.messenger.ui.contacts;

import com.rcq.messenger.data.repository.ContactRepository;
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
public final class ContactsViewModel_Factory implements Factory<ContactsViewModel> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public ContactsViewModel_Factory(Provider<ContactRepository> contactRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public ContactsViewModel get() {
    return newInstance(contactRepositoryProvider.get(), userRepositoryProvider.get());
  }

  public static ContactsViewModel_Factory create(
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new ContactsViewModel_Factory(contactRepositoryProvider, userRepositoryProvider);
  }

  public static ContactsViewModel newInstance(ContactRepository contactRepository,
      UserRepository userRepository) {
    return new ContactsViewModel(contactRepository, userRepository);
  }
}
