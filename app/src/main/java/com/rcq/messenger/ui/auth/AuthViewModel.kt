package com.rcq.messenger.ui.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.crypto.CryptoService
import com.rcq.messenger.crypto.CryptoService.RegistrationBundle
import com.rcq.messenger.data.api.RCQApiService
import com.rcq.messenger.data.api.RegisterRequest
import com.rcq.messenger.di.PreferencesKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: RCQApiService,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
    private val webSocketService: com.rcq.messenger.data.websocket.WebSocketService,
    private val cryptoService: CryptoService
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentUin = MutableStateFlow<Long?>(null)
    val currentUin: StateFlow<Long?> = _currentUin.asStateFlow()

    private val _recoveryPhrase = MutableStateFlow<List<String>>(emptyList())
    val recoveryPhrase: StateFlow<List<String>> = _recoveryPhrase.asStateFlow()

    private val _pendingBundle = MutableStateFlow<RegistrationBundle?>(null)
    val pendingBundle: StateFlow<RegistrationBundle?> = _pendingBundle.asStateFlow()

    private val _showRecoveryPhrase = MutableStateFlow(false)
    val showRecoveryPhrase: StateFlow<Boolean> = _showRecoveryPhrase.asStateFlow()

    companion object {
        private val KEY_UIN = longPreferencesKey("uin")
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_NICKNAME = stringPreferencesKey("nickname")
        private val KEY_IDENTITY_KEY = stringPreferencesKey("identity_key")
        private val KEY_SIGNING_KEY = stringPreferencesKey("signing_key")
        private val KEY_RECOVERY_PHRASE = stringPreferencesKey("recovery_phrase")
    }

    init {
        checkExistingAuth()
    }

    private fun checkExistingAuth() {
        viewModelScope.launch {
            context.dataStore.data.first().let { prefs ->
                val uin = prefs[KEY_UIN]
                val token = prefs[KEY_TOKEN]
                val savedNickname = prefs[KEY_NICKNAME] ?: ""
                val identityKey = prefs[KEY_IDENTITY_KEY]

                if (uin != null && token != null && identityKey != null) {
                    _nickname.value = savedNickname
                    _currentUin.value = uin
                    _isAuthenticated.value = true
                    _authState.value = AuthState.Authenticated
                    // Connect WebSocket after successful authentication
                    webSocketService.connect()
                } else {
                    _authState.value = AuthState.Onboarding
                }
            }
        }
    }

    fun startRegistration(nickname: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _nickname.value = nickname

            try {
                // Generate registration bundle (identity + signing keys)
                val bundle = cryptoService.generateRegistrationBundle()
                _pendingBundle.value = bundle

                // Register with server
                val response = api.register(
                    RegisterRequest(
                        nickname = nickname,
                        identity_key = bundle.identityKey,
                        signing_key = bundle.signedPreKey
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()!!
                    val uin = body.uin
                    val token = body.token

                    // Generate recovery phrase from keys
                    val recoveryPhrase = generateRecoveryPhrase(bundle.identityKey, bundle.signedPreKey)
                    _recoveryPhrase.value = recoveryPhrase
                    _showRecoveryPhrase.value = true

                    // Save to auth DataStore
                    context.dataStore.edit { prefs ->
                        prefs[KEY_UIN] = uin
                        prefs[KEY_TOKEN] = token
                        prefs[KEY_NICKNAME] = nickname
                        prefs[KEY_IDENTITY_KEY] = bundle.identityKey
                        prefs[KEY_SIGNING_KEY] = bundle.signedPreKey
                        prefs[KEY_RECOVERY_PHRASE] = recoveryPhrase.joinToString(" ")
                    }
                    // Also save to main DataStore for AuthInterceptor and WebSocketService
                    dataStore.edit { prefs ->
                        prefs[PreferencesKeys.AUTH_TOKEN] = token
                        prefs[PreferencesKeys.USER_UIN] = uin
                    }

                    _currentUin.value = uin
                    _authState.value = AuthState.ShowRecoveryPhrase
                } else {
                    _error.value = "Registration failed (${response.code()}): ${response.message()}"
                    _authState.value = AuthState.Error(_error.value!!)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Registration failed"
                _authState.value = AuthState.Error(_error.value!!)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmRecoveryPhrase() {
        viewModelScope.launch {
            _showRecoveryPhrase.value = false
            _currentUin.value?.let { uin ->
                _isAuthenticated.value = true
                _authState.value = AuthState.Authenticated
                // Connect WebSocket after confirming recovery phrase
                webSocketService.connect()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Clear both DataStores
            context.dataStore.edit { prefs ->
                prefs.clear()
            }
            dataStore.edit { prefs ->
                prefs.clear()
            }
            _isAuthenticated.value = false
            _authState.value = AuthState.Onboarding
            _currentUin.value = null
            _nickname.value = ""
            _recoveryPhrase.value = emptyList()
            _pendingBundle.value = null
            _showRecoveryPhrase.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun generateRecoveryPhrase(identityKey: String, signingKey: String): List<String> {
        // BIP39-like word list (simplified - 24 words)
        val wordList = listOf(
            "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract",
            "absurd", "abuse", "access", "accident", "account", "accuse", "achieve", "acid",
            "acoustic", "acquire", "across", "act", "action", "actor", "actress", "actual",
            "adapt", "add", "addict", "address", "adjust", "admit", "adult", "advance",
            "advice", "aerobic", "affair", "afford", "afraid", "again", "age", "agent",
            "agree", "ahead", "aim", "air", "airport", "aisle", "alarm", "album",
            "alert", "alien", "all", "alley", "allow", "almost", "alone", "alpha",
            "already", "also", "alter", "always", "amateur", "amazing", "among", "amount",
            "ancient", "anger", "angle", "angry", "animal", "ankle", "announce", "annual",
            "another", "answer", "antenna", "antique", "anxiety", "any", "apart", "apology",
            "appear", "apple", "approve", "april", "arch", "arctic", "area", "arena",
            "argue", "arm", "armed", "armor", "army", "around", "arrange", "arrest"
        )

        // Derive 24 words from keys using simple deterministic algorithm
        val combined = "$identityKey$signingKey"
        val words = mutableListOf<String>()

        for (i in 0 until 24) {
            val index = (combined.hashCode() + i * 17 + combined[i % combined.length].code) % wordList.size
            words.add(wordList[kotlin.math.abs(index)])
        }

        return words
    }
}

sealed class AuthState {
    data object Loading : AuthState()
    data object Onboarding : AuthState()
    data object ShowRecoveryPhrase : AuthState()
    data object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}