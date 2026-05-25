package com.rcq.messenger.di;

import com.rcq.messenger.data.db.ChatDao;
import com.rcq.messenger.data.db.RCQDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideChatDaoFactory implements Factory<ChatDao> {
  private final Provider<RCQDatabase> databaseProvider;

  public AppModule_ProvideChatDaoFactory(Provider<RCQDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ChatDao get() {
    return provideChatDao(databaseProvider.get());
  }

  public static AppModule_ProvideChatDaoFactory create(Provider<RCQDatabase> databaseProvider) {
    return new AppModule_ProvideChatDaoFactory(databaseProvider);
  }

  public static ChatDao provideChatDao(RCQDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideChatDao(database));
  }
}
