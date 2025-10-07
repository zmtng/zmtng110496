package com.example.prototyp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.prototyp.prefs.ThemePrefs
import kotlinx.coroutines.launch
import com.example.prototyp.AppDatabase
import kotlinx.coroutines.flow.distinctUntilChanged

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themePrefs = ThemePrefs(this)
        AppCompatDelegate.setDefaultNightMode(themePrefs.theme)
        setContentView(R.layout.activity_main)

        AppDatabase.getInstance(applicationContext)



        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment())
                .commit()
        }
    }
}
