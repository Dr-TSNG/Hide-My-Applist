package icu.nullptr.hidemyapplist.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val encoder = Json { encodeDefaults = true }

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
        val isWhitelist: Boolean = false,
        val appList: MutableSet<String> = mutableSetOf(),
        val mapsRules: MutableSet<String> = mutableSetOf(),
        val queryParamRules: MutableSet<String> = mutableSetOf()
    ) {
        override fun toString() = encoder.encodeToString(this)
    }

    @Serializable
    data class AppConfig(
        var useWhitelist: Boolean = false,
        var enableAllHooks: Boolean = false,
        var excludeSystemApps: Boolean = false,
        val applyHooks: MutableSet<String> = mutableSetOf(),
        val applyTemplates: MutableSet<String> = mutableSetOf(),
        val extraAppList: MutableSet<String> = mutableSetOf(),
        val extraMapsRules: MutableSet<String> = mutableSetOf(),
        val extraQueryParamRules: MutableSet<String> = mutableSetOf()
    ) {
        override fun toString() = encoder.encodeToString(this)
    }

    companion object {
        fun parse(json: String) = encoder.decodeFromString<JsonConfig>(json)
    }

    override fun toString() = encoder.encodeToString(this)
}
