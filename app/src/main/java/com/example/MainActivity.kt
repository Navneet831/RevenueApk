package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.DashboardScreen
import com.example.ui.GrewLoginScreen
import com.example.ui.GrewViewModel
import com.example.ui.theme.MyApplicationTheme
import io.github.jan.supabase.auth.status.SessionStatus

class MainActivity : ComponentActivity() {
    private val viewModel: GrewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val userSession by viewModel.userSession.collectAsState()
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    when (userSession) {
                        is SessionStatus.Authenticated -> {
                            DashboardScreen(viewModel = viewModel)
                        }
                        else -> {
                            GrewLoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = { 
                                    // SessionStatus will update automatically via collectAsState
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
