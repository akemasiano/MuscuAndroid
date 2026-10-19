package fr.muscu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.muscu.app.ui.exercises.ExerciseEditScreen
import fr.muscu.app.ui.exercises.ExercisesScreen
import fr.muscu.app.ui.theme.MuscuColors
import fr.muscu.app.ui.theme.MuscuTheme
import fr.muscu.app.ui.week.WeekScreen

private object Routes {
    const val WEEK = "week"
    const val EXERCISES = "exercises"
    const val EXERCISE_EDIT = "exercise/{id}"
    fun exercise(id: Long = -1) = "exercise/$id"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuscuTheme {
                val nav = rememberNavController()
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route

                Scaffold(
                    containerColor = MuscuColors.Background,
                    bottomBar = {
                        if (route == Routes.WEEK || route == Routes.EXERCISES) {
                            NavigationBar(containerColor = MuscuColors.Surface) {
                                listOf(
                                    Triple(Routes.WEEK, "Semaine", Icons.Outlined.CalendarMonth),
                                    Triple(Routes.EXERCISES, "Exercices", Icons.AutoMirrored.Outlined.FormatListBulleted),
                                ).forEach { (dest, label, icon) ->
                                    NavigationBarItem(
                                        selected = route == dest,
                                        onClick = {
                                            nav.navigate(dest) {
                                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(icon, null) },
                                        label = { Text(label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MuscuColors.Primary,
                                            selectedTextColor = MuscuColors.Primary,
                                            indicatorColor = MuscuColors.PrimaryLight,
                                            unselectedIconColor = MuscuColors.TextSecondary,
                                            unselectedTextColor = MuscuColors.TextSecondary,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                ) { padding ->
                    NavHost(nav, startDestination = Routes.WEEK, modifier = Modifier.padding(padding)) {
                        composable(Routes.WEEK) {
                            WeekScreen(onCreateExercise = { nav.navigate(Routes.exercise()) })
                        }
                        composable(Routes.EXERCISES) {
                            ExercisesScreen(
                                onOpenExercise = { nav.navigate(Routes.exercise(it)) },
                                onNewExercise = { nav.navigate(Routes.exercise()) },
                            )
                        }
                        composable(
                            Routes.EXERCISE_EDIT,
                            arguments = listOf(navArgument("id") { type = NavType.LongType }),
                        ) { entry ->
                            ExerciseEditScreen(
                                exerciseId = entry.arguments?.getLong("id") ?: -1,
                                onBack = {
                                    // Évite un double retour (ex. sauvegarde asynchrone + appui retour).
                                    if (nav.currentBackStackEntry?.id == entry.id) nav.popBackStack()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
