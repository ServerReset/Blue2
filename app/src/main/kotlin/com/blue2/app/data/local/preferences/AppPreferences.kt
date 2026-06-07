package com.blue2.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.blue2.app.domain.models.BluelinkRegion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "blue2_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.dataStore

    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val TOKEN_EXPIRY = longPreferencesKey("token_expiry")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val REGION = stringPreferencesKey("region")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val CA_PIN = stringPreferencesKey("ca_pin")
        val EU_PIN = stringPreferencesKey("eu_pin")
        val US_PIN = stringPreferencesKey("us_pin")
        val EU_CONTROL_TOKEN = stringPreferencesKey("eu_control_token")
        val EU_CONTROL_TOKEN_EXPIRY = longPreferencesKey("eu_control_token_expiry")

        // Theme
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val CUSTOM_SEED_COLOR = intPreferencesKey("custom_seed_color")
        val USE_ATKINSON_FONT = booleanPreferencesKey("use_atkinson_font")

        // App behavior
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val PRESSURE_UNIT = stringPreferencesKey("pressure_unit")
        val AUTO_REFRESH_INTERVAL_MIN = intPreferencesKey("auto_refresh_interval_min")
        val LAST_SELECTED_VIN = stringPreferencesKey("last_selected_vin")

        // Log
        val APP_LOG = stringPreferencesKey("app_log")

        // Home command order: comma-separated command keys
        val COMMAND_ORDER = stringPreferencesKey("command_order")
    }

    val accessToken: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[ACCESS_TOKEN] }

    val refreshToken: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[REFRESH_TOKEN] }

    val tokenExpiry: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[TOKEN_EXPIRY] ?: 0L }

    val userId: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[USER_ID] }

    val userEmail: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[USER_EMAIL] }

    val region: Flow<BluelinkRegion> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[REGION]?.let { runCatching { BluelinkRegion.valueOf(it) }.getOrNull() }
                ?: BluelinkRegion.US
        }

    val deviceId: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[DEVICE_ID] }

    val isLoggedIn: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[IS_LOGGED_IN] ?: false }

    val themeMode: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[THEME_MODE] ?: "system" }

    val themeStyle: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[THEME_STYLE] ?: "material_expressive" }

    val dynamicColor: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[DYNAMIC_COLOR] ?: true }

    val amoledMode: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[AMOLED_MODE] ?: false }

    val customSeedColor: Flow<Int?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[CUSTOM_SEED_COLOR] }

    val useAtkinsonFont: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[USE_ATKINSON_FONT] ?: false }

    val distanceUnit: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[DISTANCE_UNIT] ?: "km" }

    val tempUnit: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[TEMP_UNIT] ?: "C" }

    val autoRefreshIntervalMin: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[AUTO_REFRESH_INTERVAL_MIN] ?: 15 }

    val lastSelectedVin: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[LAST_SELECTED_VIN] }

    val commandOrder: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[COMMAND_ORDER] }

    val appLog: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[APP_LOG] ?: "" }

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        userId: String? = null,
        email: String? = null,
    ) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
            prefs[TOKEN_EXPIRY] = System.currentTimeMillis() + (expiresInSeconds * 1000)
            userId?.let { prefs[USER_ID] = it }
            email?.let { prefs[USER_EMAIL] = it }
            prefs[IS_LOGGED_IN] = true
        }
    }

    suspend fun saveRegion(region: BluelinkRegion) {
        dataStore.edit { it[REGION] = region.name }
    }

    suspend fun saveDeviceId(deviceId: String) {
        dataStore.edit { it[DEVICE_ID] = deviceId }
    }

    val caPin: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[CA_PIN] }

    val usPin: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[US_PIN] }

    val euControlToken: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[EU_CONTROL_TOKEN] }

    val euControlTokenExpiry: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[EU_CONTROL_TOKEN_EXPIRY] ?: 0L }

    suspend fun saveCaPin(pin: String) { dataStore.edit { it[CA_PIN] = pin } }
    suspend fun saveEuPin(pin: String) { dataStore.edit { it[EU_PIN] = pin } }
    suspend fun saveUsPin(pin: String) { dataStore.edit { it[US_PIN] = pin } }

    suspend fun saveEuControlToken(token: String, expiresInSeconds: Long) {
        dataStore.edit { prefs ->
            prefs[EU_CONTROL_TOKEN] = token
            prefs[EU_CONTROL_TOKEN_EXPIRY] = System.currentTimeMillis() + (expiresInSeconds * 1000)
        }
    }

    suspend fun getCaPinOnce(): String? =
        dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[CA_PIN] }.first()

    suspend fun getEuPinOnce(): String? =
        dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[EU_PIN] }.first()

    suspend fun getUsPinOnce(): String? =
        dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[US_PIN] }.first()

    suspend fun getEuControlTokenOnce(): Pair<String?, Long> =
        dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[EU_CONTROL_TOKEN] to (it[EU_CONTROL_TOKEN_EXPIRY] ?: 0L) }.first()

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
            prefs.remove(TOKEN_EXPIRY)
            prefs.remove(USER_ID)
            prefs.remove(EU_CONTROL_TOKEN)
            prefs.remove(EU_CONTROL_TOKEN_EXPIRY)
            prefs[IS_LOGGED_IN] = false
        }
    }

    suspend fun setThemeMode(mode: String) { dataStore.edit { it[THEME_MODE] = mode } }
    suspend fun setThemeStyle(style: String) { dataStore.edit { it[THEME_STYLE] = style } }
    suspend fun setDynamicColor(enabled: Boolean) { dataStore.edit { it[DYNAMIC_COLOR] = enabled } }
    suspend fun setAmoledMode(enabled: Boolean) { dataStore.edit { it[AMOLED_MODE] = enabled } }
    suspend fun setCustomSeedColor(color: Int?) {
        dataStore.edit { prefs ->
            if (color != null) prefs[CUSTOM_SEED_COLOR] = color
            else prefs.remove(CUSTOM_SEED_COLOR)
        }
    }
    suspend fun setUseAtkinsonFont(enabled: Boolean) { dataStore.edit { it[USE_ATKINSON_FONT] = enabled } }
    suspend fun setDistanceUnit(unit: String) { dataStore.edit { it[DISTANCE_UNIT] = unit } }
    suspend fun setTempUnit(unit: String) { dataStore.edit { it[TEMP_UNIT] = unit } }
    suspend fun setAutoRefreshInterval(minutes: Int) { dataStore.edit { it[AUTO_REFRESH_INTERVAL_MIN] = minutes } }
    suspend fun setLastSelectedVin(vin: String) { dataStore.edit { it[LAST_SELECTED_VIN] = vin } }

    suspend fun appendLog(entry: String) {
        dataStore.edit { prefs ->
            val current = prefs[APP_LOG] ?: ""
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date())
            val newEntry = "[$timestamp] $entry\n"
            val updated = (current + newEntry).takeLast(50_000)
            prefs[APP_LOG] = updated
        }
    }

    suspend fun clearLog() { dataStore.edit { it[APP_LOG] = "" } }

    suspend fun setCommandOrder(order: String) { dataStore.edit { it[COMMAND_ORDER] = order } }

    suspend fun getAccessTokenOnce(): String? =
        dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[ACCESS_TOKEN] }.first()

    suspend fun getRegionOnce(): BluelinkRegion =
        dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { prefs ->
                prefs[REGION]?.let { runCatching { BluelinkRegion.valueOf(it) }.getOrNull() }
                    ?: BluelinkRegion.US
            }.first()
}
