package com.rcq.messenger.di;

import com.rcq.messenger.data.db.MessageDao;
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
public final class AppModule_ProvideMessageDaoFactory implements Factory<MessageDao> {
  private final Provider<RCQDatabase> databaseProvider;

  public AppModule_ProvideMessageDaoFactory(Provider<RCQDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public MessageDao get() {
    return provideMessageDao(databaseProvider.get());
  }

  public static AppModule_ProvideMessageDaoFactory create(Provider<RCQDatabase> databaseProvider) {
    return new AppModule_ProvideMessageDaoFactory(databaseProvider);
  }

  public static MessageDao provideMessageDao(RCQDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMessageDao(database));
  }
}
