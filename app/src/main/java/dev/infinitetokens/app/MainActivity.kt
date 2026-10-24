package dev.infinitetokens.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color(0xFF101010).toArgb()
        window.navigationBarColor = Color(0xFF101010).toArgb()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 0

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Color(0xFF101010),
                surface = Color(0xFF171717),
                primary = Color(0xFFE68568),
                onBackground = Color(0xFFF5F2EF),
                onSurface = Color(0xFFF5F2EF),
            )) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF101010)) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF101010)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Infinite Tokens",
                            color = Color(0xFFF5F2EF),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Serif,
                        )
                    }
                }
            }
        }
    }
}
