
package com.example.unisphere.ui.composables

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.unisphere.ui.screen.uniHome.HomeScreen
import com.example.unisphere.ui.screen.profile.ProfileScreen
import com.example.unisphere.ui.screen.profile.ProfileViewModel
import com.example.unisphere.ui.screen.accessScreen.LoginScreen
import com.example.unisphere.ui.screen.accessScreen.ResetPasswordScreen
import com.example.unisphere.ui.screen.accessScreen.SignInScreen
import com.example.unisphere.ui.screen.calendar.AddCalendarEvent
import com.example.unisphere.ui.screen.calendar.CalendarScreen
import com.example.unisphere.ui.screen.calendar.EditCalendarEventScreen
import com.example.unisphere.ui.screen.calendar.EventDetailScreen
import com.example.unisphere.ui.screen.wallet.WalletScreen
import com.example.unisphere.ui.screen.map.MapScreen
import com.example.unisphere.ui.screen.cook.CookScreen
import com.example.unisphere.ui.screen.cook.FavoriteRecipesScreen
import com.example.unisphere.ui.screen.cook.RecipeDetailScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    profileViewModel: ProfileViewModel
) {
    NavHost(
        navController = navController,
        startDestination = NavigationRoute.LoginScreen
    ) {
        composable<NavigationRoute.LoginScreen> {
            LoginScreen(navController)
        }

        composable<NavigationRoute.Homescreen> {
            HomeScreen(
                navController = navController,
                profileViewModel = profileViewModel
            )
        }

        composable<NavigationRoute.SignInScreen> {
            SignInScreen(navController)
        }

        composable<NavigationRoute.ProfileScreen> {
            ProfileScreen(
                navController = navController,
                viewModel = profileViewModel
            )
        }

        composable<NavigationRoute.CalendarScreen> {
            CalendarScreen(
                navController = navController,
                profileViewModel = profileViewModel
            )
        }

        composable<NavigationRoute.AddCalendarEvent> {
            AddCalendarEvent(navController)
        }

        composable<NavigationRoute.WalletScreen> {
            WalletScreen(
                navController = navController,
                profileViewModel = profileViewModel
            )
        }

        composable<NavigationRoute.MapScreen> {
            MapScreen(
                navController = navController,
                profileViewModel = profileViewModel
            )
        }

        composable<NavigationRoute.CookScreen> {
            CookScreen(
                navController = navController,
                profileViewModel = profileViewModel
            )
        }

        composable<NavigationRoute.RecipeDetailScreen> { backStackEntry ->
            val route: NavigationRoute.RecipeDetailScreen =
                backStackEntry.toRoute()

            RecipeDetailScreen(
                recipeId = route.recipeId,
                navController = navController
            )
        }

        composable<NavigationRoute.EventDetailScreen> {
            EventDetailScreen(navController)
        }

        composable<NavigationRoute.EditCalendarEventScreen> {
            EditCalendarEventScreen(navController)
        }

        composable<NavigationRoute.FavoriteRecipesScreen> {
            FavoriteRecipesScreen(navController)
        }

        composable("reset_password_screen") {
            ResetPasswordScreen(navController = navController)
        }
    }
}
