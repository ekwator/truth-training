package com.truth.training.client

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
        
        try {
            // Get Application instance and database
            val application = application as? TruthTrainingApplication
                ?: throw IllegalStateException("Application must be TruthTrainingApplication")
            
            val database = try {
                application.database
            } catch (e: Exception) {
                // Database initialization failed - show error state
                showErrorState("Database initialization failed: ${e.message}")
                return
            }
            
            // Initialize Truth Core (non-critical, continue if fails)
            try {
                TruthCore.initNode()
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "TruthCore initialization failed: ${e.message}", e)
                // Continue without TruthCore - app can still function
            }
            
            // Repository will be created when needed via ViewModels or dependency injection
            // No need to create here - ViewModels will handle repository creation
            
            setContent {
                TruthTrainingTheme {
                    Surface(
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        
                        // Main navigation graph
                        // Entry screen is explicitly defined as "events" in MainNavigation
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
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
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

