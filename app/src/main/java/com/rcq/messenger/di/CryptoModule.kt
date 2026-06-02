package com.rcq.messenger.di

import com.rcq.messenger.crypto.PersistentSignalProtocolStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.signal.libsignal.protocol.state.SignalProtocolStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CryptoModule {

    @Binds
    @Singleton
    abstract fun bindSignalProtocolStore(
        impl: PersistentSignalProtocolStore
    ): SignalProtocolStore
}
