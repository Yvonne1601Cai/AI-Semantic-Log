package com.example.yvonnecai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.yvonnecai.core.designsystem.theme.YvonneCaiTheme
import com.example.yvonnecai.feature.logs.ui.LogsRoute
import dagger.hilt.android.AndroidEntryPoint

/** Single activity hosting the logs screen. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YvonneCaiTheme {
                LogsRoute()
            }
        }
    }
}
