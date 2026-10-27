package dev.infinitetokens.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.infinitetokens.ui.InfiniteTokensApp
import dev.infinitetokens.ui.InfiniteTokensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color(0xFF101010).toArgb()
        window.navigationBarColor = Color(0xFF101010).toArgb()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 0

        setContent {
            InfiniteTokensTheme { InfiniteTokensApp(onExit = { finish() }) }
        }
    }
}
