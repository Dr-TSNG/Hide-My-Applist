package com.tsng.hidemyapplist.ui

import android.widget.ImageView
import android.widget.TextView
import com.drakeet.about.*
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.R


class AboutActivity : AbsAboutActivity() {
    override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
        icon.setImageResource(R.mipmap.ic_launcher)
        slogan.text = applicationInfo.loadLabel(packageManager)
        version.text = "V" + BuildConfig.VERSION_NAME
    }

    override fun onItemsCreated(items: MutableList<Any>) {
        items.add(Category(getString(R.string.about_title)))
        items.add(Card(getString(R.string.about_description)))

        items.add(Category(getString(R.string.about_how_to_use_title)))
        items.add(Card(getString(R.string.about_how_to_use_description_1)))
        items.add(Line())
        items.add(Card(getString(R.string.about_how_to_use_description_2)))
        items.add(Line())
        items.add(Card(getString(R.string.about_how_to_use_description_3)))
        items.add(Line())
        items.add(Card(getString(R.string.about_how_to_use_description_4)))

        items.add(Category(getString(R.string.about_hook_differences_title)))
        items.add(Card(getString(R.string.about_hook_differences_description)))

        items.add(Category(getString(R.string.about_developer)))
        items.add(Contributor(R.drawable.about_author, "Dr.TSNG", "Developer", "https://github.com/Dr-TSNG"))
        items.add(Contributor(R.drawable.about_icon_designer, "辉少菌", "Icon designer", "http://www.coolapk.com/u/1560270"))

        items.add(Category(getString(R.string.about_support)))
        items.add(Card("Github Page\nhttps://github.com/Dr-TSNG/Hide-My-Applist"))
        items.add(Line())
        items.add(Card("Telegram Group\nhttps://t.me/HideMyApplist"))
        items.add(Line())
        items.add(Card("CoolMarket\nhttps://www.coolapk.com/u/1911298"))

        items.add(Category(getString(R.string.about_open_source)))
        items.add(License("jsonxx", "Nomango", License.MIT, "https://github.com/Nomango/jsonxx"))
        items.add(License("Gson", "Google", License.APACHE_2, "https://github.com/google/gson"))
        items.add(License("okhttp", "square", License.APACHE_2, "https://github.com/square/okhttp"))
        items.add(License("MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"))
        items.add(License("about-page", "drakeet", License.APACHE_2, "https://github.com/drakeet/about-page"))
        items.add(License("SmartRefreshLayout", "scwang90", License.APACHE_2, "https://github.com/scwang90/SmartRefreshLayout"))
        items.add(License("Dobby", "jmpews", License.APACHE_2, "https://github.com/jmpews/Dobby"))
        items.add(License("libsu", "topjohnwu", License.APACHE_2, "https://github.com/topjohnwu/libsu"))
    }
}