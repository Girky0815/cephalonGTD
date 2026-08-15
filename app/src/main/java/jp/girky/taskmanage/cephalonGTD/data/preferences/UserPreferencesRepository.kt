package jp.girky.taskmanage.cephalonGTD.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AuthType {
    NONE,
    BIOMETRIC,
    PIN
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val AUTH_TYPE = stringPreferencesKey("auth_type")
        val SCREEN_CAPTURE_PROTECTION = booleanPreferencesKey("screen_capture_protection")
        val SELECTED_GROUP = stringPreferencesKey("selected_group")
        val USER_PIN = stringPreferencesKey("user_pin")
    }

    val authType: Flow<AuthType> = context.dataStore.data.map { preferences ->
        val name = preferences[PreferencesKeys.AUTH_TYPE] ?: AuthType.NONE.name
        try {
            AuthType.valueOf(name)
        } catch (e: Exception) {
            AuthType.NONE
        }
    }

    val screenCaptureProtection: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SCREEN_CAPTURE_PROTECTION] ?: false
    }

    val selectedGroup: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SELECTED_GROUP] ?: "すべて"
    }

    val userPin: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_PIN]
    }

    suspend fun setAuthType(authType: AuthType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_TYPE] = authType.name
        }
    }

    suspend fun setScreenCaptureProtection(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCREEN_CAPTURE_PROTECTION] = enabled
        }
    }

    suspend fun setSelectedGroup(group: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_GROUP] = group
        }
    }

    suspend fun setUserPin(pin: String?) {
        context.dataStore.edit { preferences ->
            if (pin != null) {
                preferences[PreferencesKeys.USER_PIN] = pin
            } else {
                preferences.remove(PreferencesKeys.USER_PIN)
            }
        }
    }
}
