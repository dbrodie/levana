package com.levana.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.levana.app.ui.calendar.CalendarScreen
import com.levana.app.ui.daydetail.DayDetailScreen
import com.levana.app.ui.navigation.CalendarRoute
import com.levana.app.ui.navigation.DayDetailRoute
import com.levana.app.ui.theme.LevanaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LevanaTheme {
                LevanaApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevanaApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val canGoBack = backStackEntry?.destination?.route != null &&
        navController.previousBackStackEntry != null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CalendarRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<CalendarRoute> {
                CalendarScreen(
                    onDayClick = { date ->
                        navController.navigate(DayDetailRoute(date.toEpochDay()))
                    }
                )
            }
            composable<DayDetailRoute> { backStack ->
                val route = backStack.toRoute<DayDetailRoute>()
                DayDetailScreen(dateEpochDay = route.dateEpochDay)
            }
        }
    }
}
