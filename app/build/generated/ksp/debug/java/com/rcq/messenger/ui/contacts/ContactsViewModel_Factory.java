package com.rcq.messenger.ui.contacts;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.rcq.messenger.data.db.ContactDao;
import com.rcq.messenger.data.repository.ChatRepository;
import com.rcq.messenger.data.repository.ContactRepository;
import com.rcq.messenger.data.repository.GroupRepository;
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

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<ContactDao> contactDaoProvider;

  private final Provider<GroupRepository> groupRepositoryProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public ContactsViewModel_Factory(Provider<ContactRepository> contactRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider, Provider<ContactDao> contactDaoProvider,
      Provider<GroupRepository> groupRepositoryProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.contactDaoProvider = contactDaoProvider;
    this.groupRepositoryProvider = groupRepositoryProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public ContactsViewModel get() {
    return newInstance(contactRepositoryProvider.get(), chatRepositoryProvider.get(), userRepositoryProvider.get(), contactDaoProvider.get(), groupRepositoryProvider.get(), dataStoreProvider.get());
  }

  public static ContactsViewModel_Factory create(
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider, Provider<ContactDao> contactDaoProvider,
      Provider<GroupRepository> groupRepositoryProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new ContactsViewModel_Factory(contactRepositoryProvider, chatRepositoryProvider, userRepositoryProvider, contactDaoProvider, groupRepositoryProvider, dataStoreProvider);
  }

  public static ContactsViewModel newInstance(ContactRepository contactRepository,
      ChatRepository chatRepository, UserRepository userRepository, ContactDao contactDao,
      GroupRepository groupRepository, DataStore<Preferences> dataStore) {
    return new ContactsViewModel(contactRepository, chatRepository, userRepository, contactDao, groupRepository, dataStore);
  }
}
