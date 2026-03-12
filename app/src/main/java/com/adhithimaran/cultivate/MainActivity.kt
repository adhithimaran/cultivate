package com.adhithimaran.cultivate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.adhithimaran.cultivate.auth.LoginScreen
import com.adhithimaran.cultivate.home.HomeScreen
import com.adhithimaran.cultivate.navigation.CultivateNavBar
import com.adhithimaran.cultivate.ui.theme.CultivateTheme
import com.google.firebase.auth.FirebaseAuth
import com.adhithimaran.cultivate.add.AddHabitScreen
import com.adhithimaran.cultivate.detail.HabitDetailScreen
import com.adhithimaran.cultivate.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CultivateTheme {
                val navController = rememberNavController()
                val currentUser = FirebaseAuth.getInstance().currentUser
                val startDestination = if (currentUser != null) "home" else "login"

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute != "login") {
                            CultivateNavBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                onSignOut  = {
                                    FirebaseAuth.getInstance().signOut()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onAddHabit = { navController.navigate("add_habit") }
                            )
                        }
                        composable("add_habit") {
                            AddHabitScreen(
                                onNavigateBack = {
                                    navController.navigate("home") {
                                        popUpTo("add_habit") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(
                            route = "habit/{habitId}",
                            arguments = listOf(navArgument("habitId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val habitId = backStackEntry.arguments?.getString("habitId")
                            HabitDetailScreen(habitId = habitId)
                        }
                        composable("settings") {
                             SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}
