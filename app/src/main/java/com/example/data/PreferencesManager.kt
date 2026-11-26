package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.LanguageMode
import com.example.model.MeasurementUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tailorDataStore: DataStore<Preferences> by preferencesDataStore(name = "tailorbook_prefs")

class PreferencesManager(context: Context) {
    private val dataStore = context.applicationContext.tailorDataStore

    private object Keys {
        val LANGUAGE_MODE = stringPreferencesKey("language_mode")
        val DEFAULT_UNIT = stringPreferencesKey("default_unit")
    }

    val languageMode: Flow<LanguageMode> = dataStore.data.map { prefs ->
        LanguageMode.fromCode(prefs[Keys.LANGUAGE_MODE] ?: LanguageMode.BOTH.code)
    }

    val defaultUnit: Flow<MeasurementUnit> = dataStore.data.map { prefs ->
        MeasurementUnit.fromCode(prefs[Keys.DEFAULT_UNIT] ?: MeasurementUnit.INCHES.code)
    }

    suspend fun setLanguageMode(mode: LanguageMode) {
        dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_MODE] = mode.code
        }
    }

    suspend fun setDefaultUnit(unit: MeasurementUnit) {
        dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_UNIT] = unit.code
        }
    }
}
