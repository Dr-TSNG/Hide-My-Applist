package com.tsng.hidemyapplist

import android.annotation.SuppressLint
import com.google.gson.*
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger


object JsonConfigManager {
    object Key {
        const val TEMPLATES = "templates"                               //Array
        const val TEMPLATES_NAME = "name"                               //String
        const val TEMPLATES_IS_WHITE_LIST_MODE = "isWhiteListMode"      //Boolean
        const val TEMPLATES_APP_LIST = "appList"                        //Array
        const val TEMPLATES_MAP_RULES = "mapRules"                      //Array

        const val SCOPE = "scope"                                       //Array
        const val SCOPE_IS_WHITE_LIST_MODE = "isWhiteListMode"          //Boolean
        const val SCOPE_IS_EXCLUDE_SYSTEM_APP = "isExcludeSystemApp"    //Boolean
        const val SCOPE_APPLY_HOOKS = "applyHooks"                      //Array
        const val SCOPE_APPLY_TEMPLATES = "applyTemplates"              //Array
        const val SCOPE_EXTRA_APP_LIST = "extraAppList"                 //Array
        const val SCOPE_EXTRA_MAP_RULES = "extraMapRules"               //Array
    }

    /**
     * DO NOT CREATE NEW INSTANCE OF THIS CLASS!!!
     */
    class Config {
        @SuppressLint("SdCardPath")
        private val defConfigPath = "/data/data/${BuildConfig.APPLICATION_ID}/files/config.json"

        private var cfgFile: File = File(defConfigPath)

        private var cfg: JsonObject

        private fun init() {
            if (!cfgFile.exists()) {
                cfgFile.createNewFile()
            }
            cfg = try {
                JsonParser.parseString(cfgFile.readText()).asJsonObject
            } catch (thr: Throwable) {
                JsonObject()
            }
        }

        init {
            if (!cfgFile.exists()) {
                cfgFile.createNewFile()
            }
            cfg = try {
                JsonParser.parseString(cfgFile.readText()).asJsonObject
            } catch (thr: Throwable) {
                JsonObject()
            }
        }

        constructor()

        constructor(path: String) {
            File(path).run {
                try {
                    if (exists()) cfg = JsonParser.parseString(readText()).asJsonObject
                } catch (thr: Throwable) {
                    cfg = JsonObject()
                    save()
                }
            }
        }

        constructor(file: File) {
            file.run {
                try {
                    if (exists()) cfg = JsonParser.parseString(readText()).asJsonObject
                } catch (thr: Throwable) {
                    cfg = JsonObject()
                    save()
                }
            }
        }

        var autoSave: Boolean = false

        fun getConfig(): JsonObject {
            return cfg
        }

        fun putElement(key: String, elem: JsonElement) {
            cfg.add(key, elem)
            if (autoSave) save()
        }

        fun putBoolean(key: String, value: Boolean) {
            cfg.addProperty(key, value)
            if (autoSave) save()
        }

        fun putString(key: String, value: String) {
            cfg.addProperty(key, value)
            if (autoSave) save()
        }

        fun putNumber(key: String, value: Number) {
            cfg.addProperty(key, value)
        }

        @Synchronized
        fun save() {
            cfgFile.writeText(JsonConfigManager.cfg.toString())
        }

        @Synchronized
        fun save(cfg: String) {
            cfgFile.writeText(cfg)
        }

        @Synchronized
        fun save(cfg: JsonObject) {
            cfgFile.writeText(cfg.toString())
        }

        @Synchronized
        fun reload() {
            cfgFile = File(defConfigPath)
            init()
        }

        override fun toString(): String {
            return cfg.toString()
        }
    }

    private var cfg = Config()

    fun enableAutoSave() {
        cfg.autoSave = true
    }

    fun disableAutoSave() {
        cfg.autoSave = false
    }

    fun putElement(key: String, elem: JsonElement) {
        cfg.putElement(key, elem)
    }

    fun putBoolean(key: String, value: Boolean) {
        cfg.putBoolean(key, value)
    }

    fun putString(key: String, value: String) {
        cfg.putString(key, value)
    }

    fun putNumber(key: String, value: Number) {
        cfg.putNumber(key, value)
    }

    fun getElement(key: String): JsonElement {
        return cfg.getConfig().get(key)
    }

    fun getJsonObject(key: String): JsonObject {
        return cfg.getConfig().getAsJsonObject(key)
    }

    fun getJsonArray(key: String): JsonArray {
        return cfg.getConfig().getAsJsonArray(key)
    }

    fun getJsonPrimitive(key: String): JsonPrimitive {
        return cfg.getConfig().getAsJsonPrimitive(key)
    }

    fun getBoolean(key: String): Boolean {
        return cfg.getConfig().get(key).asBoolean
    }

    fun getString(key: String): String {
        return cfg.getConfig().get(key).asString
    }

    fun getNumber(key: String): Number {
        return cfg.getConfig().get(key).asNumber
    }

    fun getInt(key: String): Int {
        return cfg.getConfig().get(key).asInt
    }

    fun getFloat(key: String): Float {
        return cfg.getConfig().get(key).asFloat
    }

    fun getDouble(key: String): Double {
        return cfg.getConfig().get(key).asDouble
    }

    fun getLong(key: String): Long {
        return cfg.getConfig().get(key).asLong
    }

    fun getBigInteger(key: String): BigInteger {
        return cfg.getConfig().get(key).asBigInteger
    }

    fun getBigDecimal(key: String): BigDecimal {
        return cfg.getConfig().get(key).asBigDecimal
    }

    @Synchronized
    fun export(path: String) {
        File(path).run {
            if (!exists()) createNewFile()
            writeText(cfg.getConfig().toString())
        }
    }

    @Synchronized
    fun export(file: File) {
        file.run {
            if (!exists()) createNewFile()
            writeText(cfg.getConfig().toString())
        }
    }

    @Synchronized
    fun import(path: String) {
        import(File(path))
    }

    @Synchronized
    fun import(file: File) {
        if (!file.exists()) throw RuntimeException("File doesn't exists!")
        cfg = Config(file)
    }

    fun startEditConfig(block: Config.() -> Unit) {
        val flag = cfg.autoSave
        cfg.autoSave = false
        cfg.block()
        cfg.save()
        cfg.autoSave = flag
    }

    override fun toString(): String {
        return cfg.toString()
    }
}