package com.rcq.messenger.ui.auth;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AccountRecoveryViewModel_Factory implements Factory<AccountRecoveryViewModel> {
  @Override
  public AccountRecoveryViewModel get() {
    return newInstance();
  }

  public static AccountRecoveryViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AccountRecoveryViewModel newInstance() {
    return new AccountRecoveryViewModel();
  }

  private static final class InstanceHolder {
    private static final AccountRecoveryViewModel_Factory INSTANCE = new AccountRecoveryViewModel_Factory();
  }
}
