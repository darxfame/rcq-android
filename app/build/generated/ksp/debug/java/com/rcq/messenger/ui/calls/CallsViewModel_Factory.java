package com.rcq.messenger.ui.calls;

import com.rcq.messenger.data.repository.CallRepository;
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
public final class CallsViewModel_Factory implements Factory<CallsViewModel> {
  private final Provider<CallRepository> callRepositoryProvider;

  public CallsViewModel_Factory(Provider<CallRepository> callRepositoryProvider) {
    this.callRepositoryProvider = callRepositoryProvider;
  }

  @Override
  public CallsViewModel get() {
    return newInstance(callRepositoryProvider.get());
  }

  public static CallsViewModel_Factory create(Provider<CallRepository> callRepositoryProvider) {
    return new CallsViewModel_Factory(callRepositoryProvider);
  }

  public static CallsViewModel newInstance(CallRepository callRepository) {
    return new CallsViewModel(callRepository);
  }
}
