package com.rcq.messenger.data.repository;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.rcq.messenger.data.api.RCQApiService;
import com.rcq.messenger.data.db.GroupDao;
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
public final class GroupRepository_Factory implements Factory<GroupRepository> {
  private final Provider<RCQApiService> apiProvider;

  private final Provider<GroupDao> groupDaoProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public GroupRepository_Factory(Provider<RCQApiService> apiProvider,
      Provider<GroupDao> groupDaoProvider, Provider<DataStore<Preferences>> dataStoreProvider) {
    this.apiProvider = apiProvider;
    this.groupDaoProvider = groupDaoProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public GroupRepository get() {
    return newInstance(apiProvider.get(), groupDaoProvider.get(), dataStoreProvider.get());
  }

  public static GroupRepository_Factory create(Provider<RCQApiService> apiProvider,
      Provider<GroupDao> groupDaoProvider, Provider<DataStore<Preferences>> dataStoreProvider) {
    return new GroupRepository_Factory(apiProvider, groupDaoProvider, dataStoreProvider);
  }

  public static GroupRepository newInstance(RCQApiService api, GroupDao groupDao,
      DataStore<Preferences> dataStore) {
    return new GroupRepository(api, groupDao, dataStore);
  }
}
