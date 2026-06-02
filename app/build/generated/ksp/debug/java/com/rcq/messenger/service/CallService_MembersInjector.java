package com.rcq.messenger.service;

import com.rcq.messenger.call.CallManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class CallService_MembersInjector implements MembersInjector<CallService> {
  private final Provider<CallManager> callManagerProvider;

  public CallService_MembersInjector(Provider<CallManager> callManagerProvider) {
    this.callManagerProvider = callManagerProvider;
  }

  public static MembersInjector<CallService> create(Provider<CallManager> callManagerProvider) {
    return new CallService_MembersInjector(callManagerProvider);
  }

  @Override
  public void injectMembers(CallService instance) {
    injectCallManager(instance, callManagerProvider.get());
  }

  @InjectedFieldSignature("com.rcq.messenger.service.CallService.callManager")
  public static void injectCallManager(CallService instance, CallManager callManager) {
    instance.callManager = callManager;
  }
}
