package icu.nullptr.hidemyapplist.data

import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.service.PrefManager
import kotlinx.serialization.Serializable
import rxhttp.toClass
import rxhttp.toStr
import rxhttp.tryAwait
import rxhttp.wrapper.param.RxHttp
import java.util.*

class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val content: String,
    val downloadUrl: String
)

@Serializable
private data class UpdateData(
    val release: Item?,
    val beta: Item?
) {
    @Serializable
    data class Item(
        val versionName: String,
        val versionCode: Int,
        val downloadUrl: String
    )
}

suspend fun fetchLatestUpdate(): UpdateInfo? {
    val updateData = RxHttp.get(Constants.UPDATE_URL_BASE + "updates.json")
        .toClass<UpdateData>()
        .tryAwait() ?: return null
    val (isBeta, item) = if (PrefManager.receiveBetaUpdate) {
        true to (updateData.beta ?: updateData.release)
    } else {
        false to updateData.release
    }
    if (item == null) return null

    val variantPrefix = if (isBeta) "beta" else "release"
    val languagePrefix = if (Locale.getDefault().language.contains("zh")) "zh" else "en"
    val content = RxHttp.get(Constants.UPDATE_URL_BASE + variantPrefix + "-" + languagePrefix + ".html")
        .toStr()
        .tryAwait() ?: return null
    return UpdateInfo(item.versionName, item.versionCode, content, item.downloadUrl)
}
