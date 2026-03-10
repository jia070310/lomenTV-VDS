package com.lomen.tv.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.lomen.tv.ui.navigation.LomenTVNavigation
import com.lomen.tv.ui.theme.LomenTVTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge experience
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            LomenTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        LomenTVNavigation()
                    }
                )
            }
        }
    }
}
