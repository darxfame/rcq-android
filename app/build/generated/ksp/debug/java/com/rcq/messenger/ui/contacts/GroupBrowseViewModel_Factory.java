package com.rcq.messenger.ui.contacts;

import com.rcq.messenger.data.repository.GroupRepository;
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
public final class GroupBrowseViewModel_Factory implements Factory<GroupBrowseViewModel> {
  private final Provider<GroupRepository> groupRepositoryProvider;

  public GroupBrowseViewModel_Factory(Provider<GroupRepository> groupRepositoryProvider) {
    this.groupRepositoryProvider = groupRepositoryProvider;
  }

  @Override
  public GroupBrowseViewModel get() {
    return newInstance(groupRepositoryProvider.get());
  }

  public static GroupBrowseViewModel_Factory create(
      Provider<GroupRepository> groupRepositoryProvider) {
    return new GroupBrowseViewModel_Factory(groupRepositoryProvider);
  }

  public static GroupBrowseViewModel newInstance(GroupRepository groupRepository) {
    return new GroupBrowseViewModel(groupRepository);
  }
}
