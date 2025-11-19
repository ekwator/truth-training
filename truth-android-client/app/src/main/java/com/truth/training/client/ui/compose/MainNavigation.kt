package com.truth.training.client.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.truth.training.client.ui.compose.events.*
import com.truth.training.client.ui.compose.contexts.*
import com.truth.training.client.ui.compose.judgments.*
import com.truth.training.client.ui.compose.nodes.NodesScreen
import com.truth.training.client.ui.compose.nodes.NodesViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Main Navigation component for Truth Training Android app.
 * Handles navigation between Events, Contexts, and Judgments screens.
 */
@Composable
fun MainNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    // Navigation callbacks and state will be provided by ViewModel
    onNavigateToEvents: () -> Unit = {},
    onNavigateToEventDetails: (String) -> Unit = {},
    onNavigateToNewEvent: () -> Unit = {},
    onNavigateToContexts: () -> Unit = {},
    onNavigateToContextEditor: (Int?) -> Unit = {},
    onNavigateToJudgments: (String) -> Unit = {},
    onNavigateToJudgmentSubmission: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = "events",
        modifier = modifier
    ) {
        composable("events") {
            // EventListScreen will be provided via ViewModel state
            // Placeholder for now
        }
        
        composable("event/create") {
            // EventCreateScreen will be provided via ViewModel
            // Placeholder for now
        }
        
        composable("event/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            // EventDetailScreen - to be implemented
            // Placeholder for now
        }
        
        composable("contexts") {
            // ContextTemplateListScreen will be provided via ViewModel state
            // Placeholder for now
        }
        
        composable("context/create") {
            // ContextTemplateEditorScreen will be provided via ViewModel
            // Placeholder for now
        }
        
        composable("context/{templateId}") { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId")?.toIntOrNull()
            // ContextTemplateEditorScreen for editing
            // Placeholder for now
        }
        
        composable("judgments/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")?.toLongOrNull()
            if (eventId != null) {
                // TODO: Provide JudgmentListScreen with ViewModel data for eventId
            }
        }
        
        composable("judgment/submit/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")?.toLongOrNull()
            if (eventId != null) {
                // TODO: Provide JudgmentSubmissionScreen with ViewModel data for eventId
            }
        }
        
        composable("nodes") {
            val context = LocalContext.current
            val viewModel: NodesViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return NodesViewModel(context.applicationContext as android.app.Application) as T
                    }
                }
            )
            NodesScreen(viewModel = viewModel)
        }
    }
}

