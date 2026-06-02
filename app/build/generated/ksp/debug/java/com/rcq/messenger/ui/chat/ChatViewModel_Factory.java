package com.rcq.messenger.ui.chat;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.rcq.messenger.data.repository.ChatRepository;
import com.rcq.messenger.data.repository.UserRepository;
import com.rcq.messenger.media.MediaService;
import com.rcq.messenger.media.VoiceRecorder;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<MediaService> mediaServiceProvider;

  private final Provider<VoiceRecorder> voiceRecorderProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public ChatViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider, Provider<MediaService> mediaServiceProvider,
      Provider<VoiceRecorder> voiceRecorderProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.mediaServiceProvider = mediaServiceProvider;
    this.voiceRecorderProvider = voiceRecorderProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(chatRepositoryProvider.get(), userRepositoryProvider.get(), mediaServiceProvider.get(), voiceRecorderProvider.get(), dataStoreProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider, Provider<MediaService> mediaServiceProvider,
      Provider<VoiceRecorder> voiceRecorderProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new ChatViewModel_Factory(chatRepositoryProvider, userRepositoryProvider, mediaServiceProvider, voiceRecorderProvider, dataStoreProvider);
  }

  public static ChatViewModel newInstance(ChatRepository chatRepository,
      UserRepository userRepository, MediaService mediaService, VoiceRecorder voiceRecorder,
      DataStore<Preferences> dataStore) {
    return new ChatViewModel(chatRepository, userRepository, mediaService, voiceRecorder, dataStore);
  }
}
