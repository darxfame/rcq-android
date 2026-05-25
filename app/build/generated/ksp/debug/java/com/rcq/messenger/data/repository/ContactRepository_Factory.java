package com.rcq.messenger.data.repository;

import com.rcq.messenger.data.api.RCQApiService;
import com.rcq.messenger.data.db.ContactDao;
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
public final class ContactRepository_Factory implements Factory<ContactRepository> {
  private final Provider<RCQApiService> apiProvider;

  private final Provider<ContactDao> contactDaoProvider;

  public ContactRepository_Factory(Provider<RCQApiService> apiProvider,
      Provider<ContactDao> contactDaoProvider) {
    this.apiProvider = apiProvider;
    this.contactDaoProvider = contactDaoProvider;
  }

  @Override
  public ContactRepository get() {
    return newInstance(apiProvider.get(), contactDaoProvider.get());
  }

  public static ContactRepository_Factory create(Provider<RCQApiService> apiProvider,
      Provider<ContactDao> contactDaoProvider) {
    return new ContactRepository_Factory(apiProvider, contactDaoProvider);
  }

  public static ContactRepository newInstance(RCQApiService api, ContactDao contactDao) {
    return new ContactRepository(api, contactDao);
  }
}
