package com.truth.training.client

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.truth.training.client.data.TruthRepository
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.ui.compose.MainNavigation
import com.truth.training.client.ui.compose.theme.TruthTrainingTheme

/**
 * MainActivity with Jetpack Compose UI for Truth Training Android v1.0.0.
 * Integrates Room database, repositories, and Compose navigation.
 */
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val activityStartTime = System.currentTimeMillis()
        android.util.Log.d("MainActivity", "MainActivity.onCreate() started")
        
        try {
            // Verify Application instance (database will be accessed later via ViewModels)
            if (application !is TruthTrainingApplication) {
                throw IllegalStateException("Application must be TruthTrainingApplication")
            }
            
            // Database is pre-warmed in Application.onCreate() in background
            // No need to access it here - ViewModels will handle repository creation
            // This avoids blocking UI thread during launch
            
            // TruthCore is initialized in Application.onCreate() in background
            // No need to initialize again here
            
            android.util.Log.d("MainActivity", "Setting content...")
            setContent {
                // Optimize theme setup - use remember to avoid recreation
                TruthTrainingTheme {
                    Surface(
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // Optimize NavController creation - rememberNavController already uses remember internally
                        val navController = rememberNavController()
                        
                        // Main navigation graph
                        // Note: Navigation callbacks are created inline to avoid unnecessary complexity
                        // NavController is stable, so callbacks won't cause unnecessary recomposition
                               MainNavigation(
                                   navController = navController,
                                   onNavigateToEvents = { navController.navigate("events") },
                                   onNavigateToEventDetails = { eventId -> navController.navigate("event/$eventId") },
                                   onNavigateToNewEvent = { navController.navigate("event/create") },
                                   onNavigateToContexts = { navController.navigate("contexts") },
                                   onNavigateToContextEditor = { templateId ->
                                       if (templateId != null) {
                                           navController.navigate("context/$templateId")
                                       } else {
                                           navController.navigate("context/create")
                                       }
                                   },
                                   onNavigateToJudgments = { eventId -> navController.navigate("judgments/$eventId") },
                                   onNavigateToJudgmentSubmission = { eventId -> navController.navigate("judgment/submit/$eventId") },
                                   onNavigateToSummary = { navController.navigate("summary") },
                                   onNavigateToTraining = { navController.navigate("training") },
                                   onNavigateToSettings = { navController.navigate("settings") },
                                   onNavigateBack = { navController.popBackStack() }
                               )
                    }
                }
            }
            
            android.util.Log.d("MainActivity", "MainActivity.onCreate() completed in ${System.currentTimeMillis() - activityStartTime}ms")
        } catch (e: Exception) {
            // Critical error during startup - show error state
            android.util.Log.e("MainActivity", "Critical startup error", e)
            showErrorState("App initialization failed: ${e.message}")
        }
    }
    
    private fun showErrorState(message: String) {
        setContent {
            TruthTrainingTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        androidx.compose.material3.Text(
                            text = "Error",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                        androidx.compose.material3.Button(
                            onClick = { recreate() }
                        ) {
                            androidx.compose.material3.Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

