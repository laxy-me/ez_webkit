package com.eztd.arm.tools

import android.content.Context
import androidx.datastore.DataStore
import androidx.datastore.preferences.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 *
 * @author yangguangda
 * @date 2020/11/5
 */
class PDataStore {
    companion object {
        var instance: PDataStore? = null
        lateinit var dataStore: DataStore<Preferences>
        private val DATASTORE_PREFERENCE_NAME = "DataStorePreference"//定义 DataStore 的名字

        fun getInstance(context: Context): PDataStore {
            if (instance == null) {
                instance = PDataStore().apply {
                    create(context)
                }
            }
            return instance!!
        }
    }

    private fun create(context: Context) {
        dataStore = context.createDataStore(name = DATASTORE_PREFERENCE_NAME)
    }

    suspend inline fun <reified T : Any> getData(key: String, default: T): T {
        return when (T::class) {
            Int::class, String::class, Boolean::class, Float::class, Long::class -> {
                val data = dataStore.data.map { preferences ->
                    // No type safety.
                    preferences[preferencesKey<T>(key)] ?: default
                }
                data.first()
            }
            else -> {
                throw IllegalArgumentException("Type not supported: ${T::class.java}")
            }
        }
    }

    suspend inline fun getSetData(key: String, default: Set<String>): Set<String> {
        val data = dataStore.data.map { preferences ->
            // No type safety.
            preferences[preferencesSetKey<String>(key)] ?: default
        }
        return data.first()
    }

    inline fun <reified T : Any> getKey(name: String): Preferences.Key<T> {
        return when (T::class) {
            Int::class, String::class, Boolean::class, Float::class, Long::class -> {
                preferencesKey(name)
            }
            else -> {
                throw IllegalArgumentException("Type not supported: ${T::class.java}")
            }
        }
    }

    suspend inline fun <reified T : Any> setData(key: String, any: T) {
        dataStore.edit { settings ->
            when (T::class) {
                Int::class, String::class, Boolean::class, Float::class, Long::class, Set::class -> {
                    settings[getKey<T>(key)] = any
                }
                else -> {
                    throw IllegalArgumentException("Type not supported: ${T::class.java}")
                }
            }
        }
    }

    suspend inline fun setSetData(key: String, any: Set<String>) {
        dataStore.edit { settings ->
            settings[preferencesSetKey(key)] = any
        }
    }
}