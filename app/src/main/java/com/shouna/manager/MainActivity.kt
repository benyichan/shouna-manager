package com.shouna.manager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shouna.manager.navigation.AppNav
import com.shouna.manager.ui.theme.ShounaManagerTheme

class MainActivity : ComponentActivity() {

    private var openExpiring by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openExpiring = intent.getBooleanExtra(EXTRA_OPEN_EXPIRING, false)
        setContent {
            ShounaManagerTheme {
                AppNav(openExpiring = openExpiring)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_EXPIRING, false)) {
            openExpiring = true
        }
    }

    companion object {
        const val EXTRA_OPEN_EXPIRING = "open_expiring"
    }
}
