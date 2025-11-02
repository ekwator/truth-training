package com.truth.training.client

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
        
        // Get Application instance and database
        val application = application as TruthTrainingApplication
        val database = application.database
        
        // Initialize Truth Core
        TruthCore.initNode()
        
        // Create repository (Note: API will be provided by NetworkModule)
        // For now, creating with null API - will be updated in NetworkModule integration
        val repository = TruthRepository(this, database)
        
        setContent {
            TruthTrainingTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Main navigation graph
                    // Note: Full ViewModel integration would provide state and navigation callbacks
                    // This is a basic setup - can be extended with ViewModels
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
    }
}

