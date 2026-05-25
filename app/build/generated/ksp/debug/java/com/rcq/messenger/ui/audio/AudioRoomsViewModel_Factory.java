package com.rcq.messenger.ui.audio;

import com.rcq.messenger.data.repository.AudioRoomRepository;
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
public final class AudioRoomsViewModel_Factory implements Factory<AudioRoomsViewModel> {
  private final Provider<AudioRoomRepository> audioRoomRepositoryProvider;

  public AudioRoomsViewModel_Factory(Provider<AudioRoomRepository> audioRoomRepositoryProvider) {
    this.audioRoomRepositoryProvider = audioRoomRepositoryProvider;
  }

  @Override
  public AudioRoomsViewModel get() {
    return newInstance(audioRoomRepositoryProvider.get());
  }

  public static AudioRoomsViewModel_Factory create(
      Provider<AudioRoomRepository> audioRoomRepositoryProvider) {
    return new AudioRoomsViewModel_Factory(audioRoomRepositoryProvider);
  }

  public static AudioRoomsViewModel newInstance(AudioRoomRepository audioRoomRepository) {
    return new AudioRoomsViewModel(audioRoomRepository);
  }
}
