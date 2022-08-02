package icu.nullptr.hidemyapplist.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val encoder = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@Serializable
data class JsonConfig(
    var configVersion: Int = BuildConfig.SERVICE_VERSION,
    var detailLog: Boolean = false,
    var maxLogSize: Int = 512,
    val templates: MutableMap<String, Template> = mutableMapOf(),
    val scope: MutableMap<String, AppConfig> = mutableMapOf()
) {
    @Serializable
    data class Template(
        val isWhitelist: Boolean,
        val appList: Set<String>
    ) {
        override fun toString() = encoder.encodeToString(this)
    }

    @Serializable
    data class AppConfig(
        val useWhitelist: Boolean,
        val excludeSystemApps: Boolean,
        val applyTemplates: MutableSet<String>,
        val extraAppList: MutableSet<String>
    ) {
        override fun toString() = encoder.encodeToString(this)
    }

    companion object {
        fun parse(json: String) = encoder.decodeFromString<JsonConfig>(json)
    }

    override fun toString() = encoder.encodeToString(this)
}
