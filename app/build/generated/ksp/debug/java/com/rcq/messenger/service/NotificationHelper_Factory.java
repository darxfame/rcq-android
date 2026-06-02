package com.rcq.messenger.service;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class NotificationHelper_Factory implements Factory<NotificationHelper> {
  private final Provider<Context> contextProvider;

  private final Provider<SoundManager> soundManagerProvider;

  public NotificationHelper_Factory(Provider<Context> contextProvider,
      Provider<SoundManager> soundManagerProvider) {
    this.contextProvider = contextProvider;
    this.soundManagerProvider = soundManagerProvider;
  }

  @Override
  public NotificationHelper get() {
    return newInstance(contextProvider.get(), soundManagerProvider.get());
  }

  public static NotificationHelper_Factory create(Provider<Context> contextProvider,
      Provider<SoundManager> soundManagerProvider) {
    return new NotificationHelper_Factory(contextProvider, soundManagerProvider);
  }

  public static NotificationHelper newInstance(Context context, SoundManager soundManager) {
    return new NotificationHelper(context, soundManager);
  }
}
