package com.lyihub.archiveassistant.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lyihub.archiveassistant.domain.AiEngineSettings
import com.lyihub.archiveassistant.domain.AiEngineType

object AiEngineSettingsPreferences {
    val EngineTypeKey = stringPreferencesKey("ai_engine_type")
    val BaseUrlKey = stringPreferencesKey("ai_base_url")
    val ModelNameKey = stringPreferencesKey("ai_model_name")
    val ApiKeyAliasKey = stringPreferencesKey("ai_api_key_alias")
    val ApiKeyKey = stringPreferencesKey("ai_api_key")
    val LocalEndpointKey = stringPreferencesKey("ai_local_endpoint")

    fun decode(preferences: Preferences): AiEngineSettings {
        val defaults = AiEngineSettings()
        return AiEngineSettings(
            engineType = preferences[EngineTypeKey]?.let(::decodeEngineType) ?: defaults.engineType,
            baseUrl = preferences[BaseUrlKey] ?: defaults.baseUrl,
            modelName = preferences[ModelNameKey] ?: defaults.modelName,
            apiKeyAlias = preferences[ApiKeyAliasKey] ?: defaults.apiKeyAlias,
            apiKey = preferences[ApiKeyKey] ?: defaults.apiKey,
            localEndpoint = preferences[LocalEndpointKey] ?: defaults.localEndpoint,
        )
    }

    fun encode(settings: AiEngineSettings, preferences: MutablePreferences) {
        preferences[EngineTypeKey] = settings.engineType.name
        preferences[BaseUrlKey] = settings.baseUrl
        preferences[ModelNameKey] = settings.modelName
        preferences[ApiKeyAliasKey] = settings.apiKeyAlias
        preferences[ApiKeyKey] = settings.apiKey
        preferences[LocalEndpointKey] = settings.localEndpoint
    }

    private fun decodeEngineType(value: String): AiEngineType =
        AiEngineType.entries.firstOrNull { it.name == value } ?: AiEngineSettings().engineType
}
