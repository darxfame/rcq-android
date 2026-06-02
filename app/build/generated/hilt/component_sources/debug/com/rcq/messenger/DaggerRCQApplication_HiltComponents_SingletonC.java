package com.rcq.messenger;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.rcq.messenger.call.CallManager;
import com.rcq.messenger.crypto.CryptoService;
import com.rcq.messenger.crypto.EciesCrypto;
import com.rcq.messenger.crypto.EciesKeyStore;
import com.rcq.messenger.crypto.PersistentSignalProtocolStore;
import com.rcq.messenger.crypto.SessionManager;
import com.rcq.messenger.crypto.SignalKeyStore;
import com.rcq.messenger.data.api.RCQApiService;
import com.rcq.messenger.data.db.CallDao;
import com.rcq.messenger.data.db.ChatDao;
import com.rcq.messenger.data.db.ContactDao;
import com.rcq.messenger.data.db.GroupDao;
import com.rcq.messenger.data.db.MessageDao;
import com.rcq.messenger.data.db.PendingOutboxDao;
import com.rcq.messenger.data.db.RCQDatabase;
import com.rcq.messenger.data.db.SignalKeyDao;
import com.rcq.messenger.data.db.StoryDao;
import com.rcq.messenger.data.db.UserDao;
import com.rcq.messenger.data.repository.AudioRoomRepository;
import com.rcq.messenger.data.repository.CallRepository;
import com.rcq.messenger.data.repository.ChatRepository;
import com.rcq.messenger.data.repository.ContactRepository;
import com.rcq.messenger.data.repository.GroupRepository;
import com.rcq.messenger.data.repository.OutboxProcessor;
import com.rcq.messenger.data.repository.StoryRepository;
import com.rcq.messenger.data.repository.UserRepository;
import com.rcq.messenger.data.websocket.WebSocketService;
import com.rcq.messenger.di.AppModule_ProvideApiServiceFactory;
import com.rcq.messenger.di.AppModule_ProvideAuthInterceptorFactory;
import com.rcq.messenger.di.AppModule_ProvideCallDaoFactory;
import com.rcq.messenger.di.AppModule_ProvideChatDaoFactory;
import com.rcq.messenger.di.AppModule_ProvideContactDaoFactory;
import com.rcq.messenger.di.AppModule_ProvideDataStoreFactory;
import com.rcq.messenger.di.AppModule_ProvideDatabaseFactory;
import com.rcq.messenger.di.AppModule_ProvideGroupDaoFactory;
import com.rcq.messenger.di.AppModule_ProvideJsonFactory;
import com.rcq.messenger.di.AppModule_ProvideMessageDaoFactory;
import com.rcq.messenger.di.AppModule_ProvideOkHttpClientFactory;
import com.rcq.messenger.di.AppModule_ProvidePendingOutboxDaoFactory;
import com.rcq.messenger.di.AppModule_ProvideRetrofitFactory;
import com.rcq.messenger.di.AppModule_ProvideSignalKeyDaoFactory;
import com.rcq.messenger.di.AppModule_ProvideStoryDaoFactory;
import com.rcq.messenger.di.AppModule_ProvideUserDaoFactory;
import com.rcq.messenger.di.AuthInterceptor;
import com.rcq.messenger.media.MediaService;
import com.rcq.messenger.media.VoiceRecorder;
import com.rcq.messenger.service.AudioRoomService;
import com.rcq.messenger.service.CallService;
import com.rcq.messenger.service.CallService_MembersInjector;
import com.rcq.messenger.service.NotificationHelper;
import com.rcq.messenger.service.SoundManager;
import com.rcq.messenger.ui.MainActivity;
import com.rcq.messenger.ui.audio.AudioRoomsViewModel;
import com.rcq.messenger.ui.audio.AudioRoomsViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.auth.AccountRecoveryViewModel;
import com.rcq.messenger.ui.auth.AccountRecoveryViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.auth.AuthViewModel;
import com.rcq.messenger.ui.auth.AuthViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.calls.CallViewModel;
import com.rcq.messenger.ui.calls.CallViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.calls.CallsViewModel;
import com.rcq.messenger.ui.calls.CallsViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.chat.ChatViewModel;
import com.rcq.messenger.ui.chat.ChatViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.chat.ChatsViewModel;
import com.rcq.messenger.ui.chat.ChatsViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.contacts.AddContactViewModel;
import com.rcq.messenger.ui.contacts.AddContactViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.contacts.ContactsViewModel;
import com.rcq.messenger.ui.contacts.ContactsViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.contacts.CreateGroupViewModel;
import com.rcq.messenger.ui.contacts.CreateGroupViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.contacts.GroupBrowseViewModel;
import com.rcq.messenger.ui.contacts.GroupBrowseViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.contacts.PendingRequestsViewModel;
import com.rcq.messenger.ui.contacts.PendingRequestsViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.profile.ProfileViewModel;
import com.rcq.messenger.ui.profile.ProfileViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.settings.SettingsViewModel;
import com.rcq.messenger.ui.settings.SettingsViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.stories.StoriesViewModel;
import com.rcq.messenger.ui.stories.StoriesViewModel_HiltModules_KeyModule_ProvideFactory;
import com.rcq.messenger.ui.stories.StoryViewerViewModel;
import com.rcq.messenger.ui.stories.StoryViewerViewModel_HiltModules_KeyModule_ProvideFactory;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SetBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

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
public final class DaggerRCQApplication_HiltComponents_SingletonC {
  private DaggerRCQApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public RCQApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements RCQApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public RCQApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements RCQApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public RCQApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements RCQApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public RCQApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements RCQApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public RCQApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements RCQApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public RCQApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements RCQApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public RCQApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements RCQApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public RCQApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends RCQApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends RCQApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends RCQApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends RCQApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Set<String> getViewModelKeys() {
      return SetBuilder.<String>newSetBuilder(16).add(AccountRecoveryViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(AddContactViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(AudioRoomsViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(AuthViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(CallViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(CallsViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(ChatViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(ChatsViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(ContactsViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(CreateGroupViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(GroupBrowseViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(PendingRequestsViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(ProfileViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(SettingsViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(StoriesViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(StoryViewerViewModel_HiltModules_KeyModule_ProvideFactory.provide()).build();
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }
  }

  private static final class ViewModelCImpl extends RCQApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AccountRecoveryViewModel> accountRecoveryViewModelProvider;

    private Provider<AddContactViewModel> addContactViewModelProvider;

    private Provider<AudioRoomsViewModel> audioRoomsViewModelProvider;

    private Provider<AuthViewModel> authViewModelProvider;

    private Provider<CallViewModel> callViewModelProvider;

    private Provider<CallsViewModel> callsViewModelProvider;

    private Provider<ChatViewModel> chatViewModelProvider;

    private Provider<ChatsViewModel> chatsViewModelProvider;

    private Provider<ContactsViewModel> contactsViewModelProvider;

    private Provider<CreateGroupViewModel> createGroupViewModelProvider;

    private Provider<GroupBrowseViewModel> groupBrowseViewModelProvider;

    private Provider<PendingRequestsViewModel> pendingRequestsViewModelProvider;

    private Provider<ProfileViewModel> profileViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<StoriesViewModel> storiesViewModelProvider;

    private Provider<StoryViewerViewModel> storyViewerViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.accountRecoveryViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.addContactViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.audioRoomsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.callViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.callsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.chatViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.chatsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.contactsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.createGroupViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.groupBrowseViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
      this.pendingRequestsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 11);
      this.profileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 12);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 13);
      this.storiesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 14);
      this.storyViewerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 15);
    }

    @Override
    public Map<String, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(16).put("com.rcq.messenger.ui.auth.AccountRecoveryViewModel", ((Provider) accountRecoveryViewModelProvider)).put("com.rcq.messenger.ui.contacts.AddContactViewModel", ((Provider) addContactViewModelProvider)).put("com.rcq.messenger.ui.audio.AudioRoomsViewModel", ((Provider) audioRoomsViewModelProvider)).put("com.rcq.messenger.ui.auth.AuthViewModel", ((Provider) authViewModelProvider)).put("com.rcq.messenger.ui.calls.CallViewModel", ((Provider) callViewModelProvider)).put("com.rcq.messenger.ui.calls.CallsViewModel", ((Provider) callsViewModelProvider)).put("com.rcq.messenger.ui.chat.ChatViewModel", ((Provider) chatViewModelProvider)).put("com.rcq.messenger.ui.chat.ChatsViewModel", ((Provider) chatsViewModelProvider)).put("com.rcq.messenger.ui.contacts.ContactsViewModel", ((Provider) contactsViewModelProvider)).put("com.rcq.messenger.ui.contacts.CreateGroupViewModel", ((Provider) createGroupViewModelProvider)).put("com.rcq.messenger.ui.contacts.GroupBrowseViewModel", ((Provider) groupBrowseViewModelProvider)).put("com.rcq.messenger.ui.contacts.PendingRequestsViewModel", ((Provider) pendingRequestsViewModelProvider)).put("com.rcq.messenger.ui.profile.ProfileViewModel", ((Provider) profileViewModelProvider)).put("com.rcq.messenger.ui.settings.SettingsViewModel", ((Provider) settingsViewModelProvider)).put("com.rcq.messenger.ui.stories.StoriesViewModel", ((Provider) storiesViewModelProvider)).put("com.rcq.messenger.ui.stories.StoryViewerViewModel", ((Provider) storyViewerViewModelProvider)).build();
    }

    @Override
    public Map<String, Object> getHiltViewModelAssistedMap() {
      return Collections.<String, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.rcq.messenger.ui.auth.AccountRecoveryViewModel 
          return (T) new AccountRecoveryViewModel(singletonCImpl.userRepositoryProvider.get(), singletonCImpl.provideDataStoreProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // com.rcq.messenger.ui.contacts.AddContactViewModel 
          return (T) new AddContactViewModel(singletonCImpl.userRepositoryProvider.get(), singletonCImpl.contactRepositoryProvider.get());

          case 2: // com.rcq.messenger.ui.audio.AudioRoomsViewModel 
          return (T) new AudioRoomsViewModel(singletonCImpl.audioRoomRepositoryProvider.get());

          case 3: // com.rcq.messenger.ui.auth.AuthViewModel 
          return (T) new AuthViewModel(singletonCImpl.provideApiServiceProvider.get(), singletonCImpl.provideDataStoreProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.webSocketServiceProvider.get(), singletonCImpl.cryptoServiceProvider.get(), singletonCImpl.eciesKeyStoreProvider.get(), singletonCImpl.chatRepositoryProvider.get());

          case 4: // com.rcq.messenger.ui.calls.CallViewModel 
          return (T) new CallViewModel(singletonCImpl.chatRepositoryProvider.get(), singletonCImpl.callManagerProvider.get());

          case 5: // com.rcq.messenger.ui.calls.CallsViewModel 
          return (T) new CallsViewModel(singletonCImpl.callRepositoryProvider.get());

          case 6: // com.rcq.messenger.ui.chat.ChatViewModel 
          return (T) new ChatViewModel(singletonCImpl.chatRepositoryProvider.get(), singletonCImpl.userRepositoryProvider.get(), singletonCImpl.mediaServiceProvider.get(), singletonCImpl.voiceRecorderProvider.get(), singletonCImpl.provideDataStoreProvider.get());

          case 7: // com.rcq.messenger.ui.chat.ChatsViewModel 
          return (T) new ChatsViewModel(singletonCImpl.chatRepositoryProvider.get());

          case 8: // com.rcq.messenger.ui.contacts.ContactsViewModel 
          return (T) new ContactsViewModel(singletonCImpl.contactRepositoryProvider.get(), singletonCImpl.chatRepositoryProvider.get(), singletonCImpl.userRepositoryProvider.get(), singletonCImpl.contactDao(), singletonCImpl.groupRepositoryProvider.get(), singletonCImpl.provideDataStoreProvider.get());

          case 9: // com.rcq.messenger.ui.contacts.CreateGroupViewModel 
          return (T) new CreateGroupViewModel(singletonCImpl.groupRepositoryProvider.get());

          case 10: // com.rcq.messenger.ui.contacts.GroupBrowseViewModel 
          return (T) new GroupBrowseViewModel(singletonCImpl.groupRepositoryProvider.get());

          case 11: // com.rcq.messenger.ui.contacts.PendingRequestsViewModel 
          return (T) new PendingRequestsViewModel(singletonCImpl.contactRepositoryProvider.get(), singletonCImpl.webSocketServiceProvider.get());

          case 12: // com.rcq.messenger.ui.profile.ProfileViewModel 
          return (T) new ProfileViewModel(singletonCImpl.userRepositoryProvider.get(), singletonCImpl.chatRepositoryProvider.get());

          case 13: // com.rcq.messenger.ui.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.userRepositoryProvider.get(), singletonCImpl.provideDataStoreProvider.get());

          case 14: // com.rcq.messenger.ui.stories.StoriesViewModel 
          return (T) new StoriesViewModel(singletonCImpl.storyRepositoryProvider.get());

          case 15: // com.rcq.messenger.ui.stories.StoryViewerViewModel 
          return (T) new StoryViewerViewModel(singletonCImpl.storyRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends RCQApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends RCQApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectAudioRoomService(AudioRoomService audioRoomService) {
    }

    @Override
    public void injectCallService(CallService callService) {
      injectCallService2(callService);
    }

    private CallService injectCallService2(CallService instance) {
      CallService_MembersInjector.injectCallManager(instance, singletonCImpl.callManagerProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends RCQApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<SoundManager> soundManagerProvider;

    private Provider<NotificationHelper> notificationHelperProvider;

    private Provider<DataStore<Preferences>> provideDataStoreProvider;

    private Provider<AuthInterceptor> provideAuthInterceptorProvider;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<Retrofit> provideRetrofitProvider;

    private Provider<RCQApiService> provideApiServiceProvider;

    private Provider<RCQDatabase> provideDatabaseProvider;

    private Provider<WebSocketService> webSocketServiceProvider;

    private Provider<PersistentSignalProtocolStore> persistentSignalProtocolStoreProvider;

    private Provider<SignalKeyStore> signalKeyStoreProvider;

    private Provider<SessionManager> sessionManagerProvider;

    private Provider<EciesCrypto> eciesCryptoProvider;

    private Provider<CryptoService> cryptoServiceProvider;

    private Provider<ChatRepository> chatRepositoryProvider;

    private Provider<OutboxProcessor> outboxProcessorProvider;

    private Provider<UserRepository> userRepositoryProvider;

    private Provider<ContactRepository> contactRepositoryProvider;

    private Provider<AudioRoomRepository> audioRoomRepositoryProvider;

    private Provider<EciesKeyStore> eciesKeyStoreProvider;

    private Provider<CallManager> callManagerProvider;

    private Provider<CallRepository> callRepositoryProvider;

    private Provider<MediaService> mediaServiceProvider;

    private Provider<VoiceRecorder> voiceRecorderProvider;

    private Provider<GroupRepository> groupRepositoryProvider;

    private Provider<StoryRepository> storyRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private ChatDao chatDao() {
      return AppModule_ProvideChatDaoFactory.provideChatDao(provideDatabaseProvider.get());
    }

    private ContactDao contactDao() {
      return AppModule_ProvideContactDaoFactory.provideContactDao(provideDatabaseProvider.get());
    }

    private MessageDao messageDao() {
      return AppModule_ProvideMessageDaoFactory.provideMessageDao(provideDatabaseProvider.get());
    }

    private GroupDao groupDao() {
      return AppModule_ProvideGroupDaoFactory.provideGroupDao(provideDatabaseProvider.get());
    }

    private SignalKeyDao signalKeyDao() {
      return AppModule_ProvideSignalKeyDaoFactory.provideSignalKeyDao(provideDatabaseProvider.get());
    }

    private PendingOutboxDao pendingOutboxDao() {
      return AppModule_ProvidePendingOutboxDaoFactory.providePendingOutboxDao(provideDatabaseProvider.get());
    }

    private UserDao userDao() {
      return AppModule_ProvideUserDaoFactory.provideUserDao(provideDatabaseProvider.get());
    }

    private CallDao callDao() {
      return AppModule_ProvideCallDaoFactory.provideCallDao(provideDatabaseProvider.get());
    }

    private StoryDao storyDao() {
      return AppModule_ProvideStoryDaoFactory.provideStoryDao(provideDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.soundManagerProvider = DoubleCheck.provider(new SwitchingProvider<SoundManager>(singletonCImpl, 1));
      this.notificationHelperProvider = DoubleCheck.provider(new SwitchingProvider<NotificationHelper>(singletonCImpl, 0));
      this.provideDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 7));
      this.provideAuthInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<AuthInterceptor>(singletonCImpl, 6));
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 5));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 8));
      this.provideRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 4));
      this.provideApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<RCQApiService>(singletonCImpl, 3));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<RCQDatabase>(singletonCImpl, 9));
      this.webSocketServiceProvider = DoubleCheck.provider(new SwitchingProvider<WebSocketService>(singletonCImpl, 10));
      this.persistentSignalProtocolStoreProvider = DoubleCheck.provider(new SwitchingProvider<PersistentSignalProtocolStore>(singletonCImpl, 14));
      this.signalKeyStoreProvider = DoubleCheck.provider(new SwitchingProvider<SignalKeyStore>(singletonCImpl, 13));
      this.sessionManagerProvider = DoubleCheck.provider(new SwitchingProvider<SessionManager>(singletonCImpl, 12));
      this.eciesCryptoProvider = DoubleCheck.provider(new SwitchingProvider<EciesCrypto>(singletonCImpl, 15));
      this.cryptoServiceProvider = DoubleCheck.provider(new SwitchingProvider<CryptoService>(singletonCImpl, 11));
      this.chatRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ChatRepository>(singletonCImpl, 2));
      this.outboxProcessorProvider = DoubleCheck.provider(new SwitchingProvider<OutboxProcessor>(singletonCImpl, 16));
      this.userRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<UserRepository>(singletonCImpl, 17));
      this.contactRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ContactRepository>(singletonCImpl, 18));
      this.audioRoomRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AudioRoomRepository>(singletonCImpl, 19));
      this.eciesKeyStoreProvider = DoubleCheck.provider(new SwitchingProvider<EciesKeyStore>(singletonCImpl, 20));
      this.callManagerProvider = DoubleCheck.provider(new SwitchingProvider<CallManager>(singletonCImpl, 21));
      this.callRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<CallRepository>(singletonCImpl, 22));
      this.mediaServiceProvider = DoubleCheck.provider(new SwitchingProvider<MediaService>(singletonCImpl, 23));
      this.voiceRecorderProvider = DoubleCheck.provider(new SwitchingProvider<VoiceRecorder>(singletonCImpl, 24));
      this.groupRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<GroupRepository>(singletonCImpl, 25));
      this.storyRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<StoryRepository>(singletonCImpl, 26));
    }

    @Override
    public void injectRCQApplication(RCQApplication rCQApplication) {
      injectRCQApplication2(rCQApplication);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private RCQApplication injectRCQApplication2(RCQApplication instance) {
      RCQApplication_MembersInjector.injectNotificationHelper(instance, notificationHelperProvider.get());
      RCQApplication_MembersInjector.injectChatRepository(instance, chatRepositoryProvider.get());
      RCQApplication_MembersInjector.injectOutboxProcessor(instance, outboxProcessorProvider.get());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.rcq.messenger.service.NotificationHelper 
          return (T) new NotificationHelper(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.soundManagerProvider.get());

          case 1: // com.rcq.messenger.service.SoundManager 
          return (T) new SoundManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.rcq.messenger.data.repository.ChatRepository 
          return (T) new ChatRepository(singletonCImpl.provideApiServiceProvider.get(), singletonCImpl.chatDao(), singletonCImpl.contactDao(), singletonCImpl.messageDao(), singletonCImpl.groupDao(), singletonCImpl.webSocketServiceProvider.get(), singletonCImpl.cryptoServiceProvider.get(), singletonCImpl.notificationHelperProvider.get(), singletonCImpl.pendingOutboxDao());

          case 3: // com.rcq.messenger.data.api.RCQApiService 
          return (T) AppModule_ProvideApiServiceFactory.provideApiService(singletonCImpl.provideRetrofitProvider.get());

          case 4: // retrofit2.Retrofit 
          return (T) AppModule_ProvideRetrofitFactory.provideRetrofit(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 5: // okhttp3.OkHttpClient 
          return (T) AppModule_ProvideOkHttpClientFactory.provideOkHttpClient(singletonCImpl.provideAuthInterceptorProvider.get());

          case 6: // com.rcq.messenger.di.AuthInterceptor 
          return (T) AppModule_ProvideAuthInterceptorFactory.provideAuthInterceptor(singletonCImpl.provideDataStoreProvider.get());

          case 7: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) AppModule_ProvideDataStoreFactory.provideDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 8: // kotlinx.serialization.json.Json 
          return (T) AppModule_ProvideJsonFactory.provideJson();

          case 9: // com.rcq.messenger.data.db.RCQDatabase 
          return (T) AppModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 10: // com.rcq.messenger.data.websocket.WebSocketService 
          return (T) new WebSocketService(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideDataStoreProvider.get());

          case 11: // com.rcq.messenger.crypto.CryptoService 
          return (T) new CryptoService(singletonCImpl.sessionManagerProvider.get(), singletonCImpl.signalKeyStoreProvider.get(), singletonCImpl.eciesCryptoProvider.get());

          case 12: // com.rcq.messenger.crypto.SessionManager 
          return (T) new SessionManager(singletonCImpl.signalKeyStoreProvider.get());

          case 13: // com.rcq.messenger.crypto.SignalKeyStore 
          return (T) new SignalKeyStore(singletonCImpl.persistentSignalProtocolStoreProvider.get());

          case 14: // com.rcq.messenger.crypto.PersistentSignalProtocolStore 
          return (T) new PersistentSignalProtocolStore(singletonCImpl.signalKeyDao());

          case 15: // com.rcq.messenger.crypto.EciesCrypto 
          return (T) new EciesCrypto();

          case 16: // com.rcq.messenger.data.repository.OutboxProcessor 
          return (T) new OutboxProcessor(singletonCImpl.pendingOutboxDao(), singletonCImpl.messageDao(), singletonCImpl.provideApiServiceProvider.get(), singletonCImpl.cryptoServiceProvider.get(), singletonCImpl.webSocketServiceProvider.get());

          case 17: // com.rcq.messenger.data.repository.UserRepository 
          return (T) new UserRepository(singletonCImpl.provideApiServiceProvider.get(), singletonCImpl.userDao());

          case 18: // com.rcq.messenger.data.repository.ContactRepository 
          return (T) new ContactRepository(singletonCImpl.provideApiServiceProvider.get(), singletonCImpl.contactDao());

          case 19: // com.rcq.messenger.data.repository.AudioRoomRepository 
          return (T) new AudioRoomRepository(singletonCImpl.provideApiServiceProvider.get());

          case 20: // com.rcq.messenger.crypto.EciesKeyStore 
          return (T) new EciesKeyStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 21: // com.rcq.messenger.call.CallManager 
          return (T) new CallManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.webSocketServiceProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 22: // com.rcq.messenger.data.repository.CallRepository 
          return (T) new CallRepository(singletonCImpl.provideApiServiceProvider.get(), singletonCImpl.callDao());

          case 23: // com.rcq.messenger.media.MediaService 
          return (T) new MediaService(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideApiServiceProvider.get(), singletonCImpl.cryptoServiceProvider.get());

          case 24: // com.rcq.messenger.media.VoiceRecorder 
          return (T) new VoiceRecorder(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 25: // com.rcq.messenger.data.repository.GroupRepository 
          return (T) new GroupRepository(singletonCImpl.provideApiServiceProvider.get(), singletonCImpl.groupDao(), singletonCImpl.provideDataStoreProvider.get());

          case 26: // com.rcq.messenger.data.repository.StoryRepository 
          return (T) new StoryRepository(singletonCImpl.provideApiServiceProvider.get(), singletonCImpl.storyDao());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
