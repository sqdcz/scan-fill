package com.scanfill.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.LinearLayout
import com.scanfill.app.R

class AboutActivity : BaseSettingsActivity() {

    override val titleRes = R.string.cat_about

    override fun build(ui: Ui, content: LinearLayout) {
        ui.group(content).let { g ->
            ui.row(g, getString(R.string.about_version), null, ui.valueLabel("1.1.0"))
            ui.divider(g)
            ui.sectionText(g, getString(R.string.about_desc))
        }

        // 官网 & GitHub
        ui.group(content).let { g ->
            ui.row(g, getString(R.string.link_website), getString(R.string.link_website_url)) {
                open("https://github.com/sqdcz")
            }
            ui.divider(g)
            ui.row(g, getString(R.string.link_github), getString(R.string.link_github_url)) {
                open("https://github.com/sqdcz")
            }
        }

        ui.group(content, getString(R.string.about_credits)).let { g ->
            ui.sectionText(g, getString(R.string.about_credits_content))
        }
    }

    private fun open(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
