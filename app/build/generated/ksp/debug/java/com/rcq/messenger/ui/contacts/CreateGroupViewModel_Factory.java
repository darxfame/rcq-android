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
public final class CreateGroupViewModel_Factory implements Factory<CreateGroupViewModel> {
  private final Provider<GroupRepository> groupRepositoryProvider;

  public CreateGroupViewModel_Factory(Provider<GroupRepository> groupRepositoryProvider) {
    this.groupRepositoryProvider = groupRepositoryProvider;
  }

  @Override
  public CreateGroupViewModel get() {
    return newInstance(groupRepositoryProvider.get());
  }

  public static CreateGroupViewModel_Factory create(
      Provider<GroupRepository> groupRepositoryProvider) {
    return new CreateGroupViewModel_Factory(groupRepositoryProvider);
  }

  public static CreateGroupViewModel newInstance(GroupRepository groupRepository) {
    return new CreateGroupViewModel(groupRepository);
  }
}
