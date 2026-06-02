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
public final class AddContactViewModel_Factory implements Factory<AddContactViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  public AddContactViewModel_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public AddContactViewModel get() {
    return newInstance(userRepositoryProvider.get(), contactRepositoryProvider.get());
  }

  public static AddContactViewModel_Factory create(Provider<UserRepository> userRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    return new AddContactViewModel_Factory(userRepositoryProvider, contactRepositoryProvider);
  }

  public static AddContactViewModel newInstance(UserRepository userRepository,
      ContactRepository contactRepository) {
    return new AddContactViewModel(userRepository, contactRepository);
  }
}
