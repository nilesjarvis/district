package com.district.jellyfinmono

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.remember
import com.district.jellyfinmono.app.DistrictApp
import com.district.jellyfinmono.app.AppGraph
import com.district.jellyfinmono.app.AppViewModel
import com.district.jellyfinmono.core.design.MonoTokens

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(MonoTokens.BgInt),
            navigationBarStyle = SystemBarStyle.dark(MonoTokens.BgInt),
        )
        setContent {
            val graph = remember { AppGraph(applicationContext) }
            val viewModel: AppViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = graph.appViewModel() as T
                },
            )
            DistrictApp(viewModel)
        }
    }
}
