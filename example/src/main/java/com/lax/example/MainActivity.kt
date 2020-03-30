package com.lax.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lax.ezweb.Launcher
import com.lax.ezweb.WebActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Launcher.with(this, WebActivity::class.java)
            .putExtra(WebActivity.EX_TITLE, "")
            .putExtra(WebActivity.EX_HAS_TITLE_BAR, false)
            .putExtra(WebActivity.EX_URL, "https://app.mallucky.com")
            .execute()
    }
}
