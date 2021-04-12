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

        items.add(Category(getString(R.string.about_mode_differences_title)))
        items.add(Card(getString(R.string.about_mode_differences_description_1)))
        items.add(Line())
        items.add(Card(getString(R.string.about_mode_differences_description_2)))

        items.add(Category(getString(R.string.about_hook_differences_title)))
        items.add(Card(getString(R.string.about_hook_differences_description)))

        items.add(Category(getString(R.string.about_developer)))
        items.add(Contributor(0, "Dr.TSNG", "Developer", "https://github.com/Dr-TSNG"))

        items.add(Category(getString(R.string.about_support)))
        items.add(Card("Github Page\nhttps://github.com/Dr-TSNG/Hide-My-Applist"))
        items.add(Line())
        items.add(Card("CoolMarket\nhttps://www.coolapk.com/u/1911298"))

        items.add(Category(getString(R.string.about_open_source)))
        items.add(License("MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"))
        items.add(License("about-page", "drakeet", License.APACHE_2, "https://github.com/drakeet/about-page"))
    }
}