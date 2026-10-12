
package com.example.unisphere

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.unisphere.ui.composables.NavGraph
import com.example.unisphere.ui.screen.profile.ProfileViewModel
import com.example.unisphere.ui.theme.UniSphereTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val viewModel: ProfileViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            val systemDarkTheme = isSystemInDarkTheme()

            val useDarkTheme = when {
                !state.isLoggedIn -> systemDarkTheme
                state.currentTheme == "Scuro" -> true
                state.currentTheme == "Chiaro" -> false
                else -> systemDarkTheme
            }

            LaunchedEffect(
                state.isLoggedIn,
                state.currentTheme,
                useDarkTheme
            ) {
                Log.d(
                    "THEME_DEBUG",
                    "MainActivity | " +
                            "VM=${System.identityHashCode(viewModel)} | " +
                            "isLoggedIn=${state.isLoggedIn} | " +
                            "currentTheme=${state.currentTheme} | " +
                            "systemDarkTheme=$systemDarkTheme | " +
                            "useDarkTheme=$useDarkTheme"
                )
            }

            UniSphereTheme(darkTheme = useDarkTheme) {
                Surface {
                    val controller = rememberNavController()
                    navController = controller

                    NavGraph(
                        navController = controller,
                        profileViewModel = viewModel
                    )

                    LaunchedEffect(intent) {
                        intent?.let {
                            handleDeepLink(it)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data

        if (
            data != null &&
            data.scheme == "unisphere" &&
            data.host == "reset-password"
        ) {
            try {
                navController?.navigate("reset_password_screen") {
                    popUpTo(0) {
                        inclusive = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
