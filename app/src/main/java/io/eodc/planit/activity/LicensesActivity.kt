package io.eodc.planit.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import io.eodc.planit.R
import io.eodc.planit.adapter.License
import io.eodc.planit.adapter.LicenseAdapter
import java.util.*

/**
 * Activity for displaying licenses
 *
 * @author 2n
 */
class LicensesActivity : AppCompatActivity() {
    @BindView(R.id.tb)
    internal var mToolbar: Toolbar? = null
    @BindView(R.id.recycle_licence)
    internal var mRvLicenses: RecyclerView? = null

    // If there's a better way to grab licenses from dependencies, pls push
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licenses)
        ButterKnife.bind(this)

        setSupportActionBar(mToolbar)
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val licenses = ArrayList<License>()
        licenses.add(License("AHBottomNavigation",
                2017,
                "Aurelien Hubert",
                "https://www.apache.org/licenses/LICENSE-2.0.html",
                "https://github.com/aurelhubert/ahbottomnavigation"))
        licenses.add(License("ButterKnife",
                2013,
                "Jake Wharton",
                "https://www.apache.org/licenses/LICENSE-2.0.html",
                "https://github.com/JakeWharton/butterknife"))
        licenses.add(License("CircleImageView",
                2018,
                "Henning Dodenhof",
                "https://www.apache.org/licenses/LICENSE-2.0.html",
                "https://github.com/hdodenhof/CircleImageView"))
        licenses.add(License("Material Calendar View",
                2018,
                "Prolific Interactive",
                "https://opensource.org/licenses/MIT",
                "https://github.com/prolificinteractive/material-calendarview"))
        licenses.add(License("MPAndroidChart",
                2018,
                "Phillip Jahoda",
                "https://www.apache.org/licenses/LICENSE-2.0.html",
                "https://github.com/PhilJay/MPAndroidChart"))
        licenses.add(License("Spectrum",
                2016,
                "The Blue Alliance",
                "https://opensource.org/licenses/MIT",
                "https://github.com/the-blue-alliance/spectrum"))
        licenses.add(License("Timber",
                2013,
                "Jake Wharton",
                "http://www.apache.org/licenses/LICENSE-2.0",
                "https://github.com/JakeWharton/timber"))
        licenses.add(License("Joda-Time",
                2011,
                "Joda Stephen",
                "http://www.apache.org/licenses/LICENSE-2.0",
                "https://github.com/JodaOrg/joda-time"))
        mRvLicenses!!.adapter = LicenseAdapter(this, licenses)
        mRvLicenses!!.layoutManager = LinearLayoutManager(this)
    }
}
