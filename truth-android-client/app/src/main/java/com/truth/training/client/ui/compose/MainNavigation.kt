package com.truth.training.client.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.truth.training.client.ui.compose.events.*
import com.truth.training.client.ui.compose.contexts.*
import com.truth.training.client.ui.compose.judgments.*
import com.truth.training.client.ui.compose.nodes.NodesScreen
import com.truth.training.client.ui.compose.nodes.NodesViewModel
import com.truth.training.client.ui.compose.summary.OverallSummaryScreen
import com.truth.training.client.ui.compose.training.TrainingResultsScreen
import com.truth.training.client.ui.compose.settings.SettingsScreen
import com.truth.training.client.ui.summary.OverallSummaryViewModel
import com.truth.training.client.ui.training.TrainingResultsViewModel
import com.truth.training.client.ui.settings.SettingsViewModel
import com.truth.training.client.ui.compose.summary.OverallSummaryScreen
import com.truth.training.client.ui.compose.training.TrainingResultsScreen
import com.truth.training.client.ui.compose.settings.SettingsScreen
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.truth.training.client.ui.DashboardViewModel
import com.truth.training.client.ui.compose.ViewModelFactory
import com.truth.training.client.ui.events.EventListViewModel
import com.truth.training.client.ui.events.EventDetailViewModel
import com.truth.training.client.ui.events.EventCreateViewModel
import com.truth.training.client.ui.contexts.ContextTemplateListViewModel
import com.truth.training.client.ui.contexts.ContextTemplateEditorViewModel
import com.truth.training.client.ui.judgments.JudgmentListViewModel
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.data.TruthRepository
import kotlinx.coroutines.launch

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
    onNavigateToSummary: () -> Unit = {},
    onNavigateToTraining: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
    ) {
    val context = LocalContext.current
    val application = remember(context) { 
        context.applicationContext as android.app.Application 
    }
    val factory = remember(application) { ViewModelFactory(application) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = modifier
    ) {
        composable("dashboard") {
            val viewModel: DashboardViewModel = viewModel(factory = factory)
            
            // Collect ViewModel state (StateFlow.collectAsState() is already optimized)
            val syncStatus by viewModel.syncStatus.collectAsState()
            val eventCount by viewModel.eventCount.collectAsState()
            
            // Display DashboardScreen
            DashboardScreen(
                syncStatus = syncStatus,
                eventCount = eventCount,
                onNavigateToEvents = onNavigateToEvents,
                onNavigateToContexts = onNavigateToContexts,
                onNavigateToJudgments = { 
                    // Navigate to events list first, user can select event to view judgments
                    // Alternatively, we could create a route for all judgments, but this matches UX better
                    onNavigateToEvents()
                },
                onSyncNow = { viewModel.refresh() },
                onNavigateToSummary = onNavigateToSummary,
                onNavigateToTraining = onNavigateToTraining,
                onNavigateToSettings = onNavigateToSettings
            )
        }
        
        composable("events") {
            val context = LocalContext.current
            val application = remember(context) { 
                context.applicationContext as android.app.Application 
            }
            val factory = remember(application) { ViewModelFactory(application) }
            val viewModel: EventListViewModel = viewModel(factory = factory)
            
            val events by viewModel.events.collectAsState()
            val error by viewModel.error.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            
            // Show error snackbar if error occurs
            error?.let { errorMessage ->
                LaunchedEffect(errorMessage) {
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage)
                    }
                }
            }
            
            EventListScreen(
                events = events,
                onEventClick = { eventId -> onNavigateToEventDetails(eventId.toString()) },
                onNewEventClick = onNavigateToNewEvent,
                modifier = Modifier
            )
        }
        
        composable("event/create") { backStackEntry ->
            val context = LocalContext.current
            val application = remember(context) { 
                context.applicationContext as android.app.Application 
            }
            val factory = remember(application) { ViewModelFactory(application) }
            // Use backStackEntry as viewModelStoreOwner to ensure state is preserved
            val viewModel: EventCreateViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = factory
            )
            
            val error by viewModel.error.collectAsState()
            val selectedTemplateContext by viewModel.selectedTemplateContext.collectAsState()
            val templates by viewModel.templates.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            
            // Show error snackbar if error occurs
            error?.let { errorMessage ->
                LaunchedEffect(errorMessage) {
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage)
                    }
                }
            }
            
            EventCreateScreen(
                onSave = { request ->
                    viewModel.createEvent(request) {
                        onNavigateBack()
                    }
                },
                onCancel = onNavigateBack,
                selectedTemplateContext = selectedTemplateContext,
                onSelectTemplate = {
                    navController.navigate("event/create/select-template")
                },
                categoriesFlow = viewModel.categories,
                formasFlow = viewModel.formas,
                causesFlow = viewModel.causes,
                developsFlow = viewModel.develops,
                effectsFlow = viewModel.effects,
                modifier = Modifier
            )
        }
        
        composable("event/create/select-template") { backStackEntry ->
            val context = LocalContext.current
            val application = remember(context) { 
                context.applicationContext as android.app.Application 
            }
            val factory = remember(application) { ViewModelFactory(application) }
            
            // Get parent entry to share ViewModel instance
            // Use try-catch in case parent entry is not available
            val parentEntry = remember(backStackEntry) {
                try {
                    navController.getBackStackEntry("event/create")
                } catch (e: Exception) {
                    // Fallback to current entry if parent is not available
                    backStackEntry
                }
            }
            
            // Use ViewModel from parent route to share state
            val viewModel: EventCreateViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                factory = factory
            )
            
            val templates by viewModel.templates.collectAsState()
            
            ContextTemplateSelectionScreen(
                templates = templates,
                onTemplateSelected = { contextFields ->
                    // Update ViewModel state before navigating back
                    viewModel.setSelectedTemplateContext(contextFields)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() },
                modifier = Modifier
            )
        }
        
        composable("event/{eventId}") { backStackEntry ->
            val eventIdStr = backStackEntry.arguments?.getString("eventId") ?: ""
            val eventId = eventIdStr.toLongOrNull() ?: 0L
            
            val context = LocalContext.current
            val application = remember(context) { 
                context.applicationContext as android.app.Application 
            }
            val factory = remember(application) { ViewModelFactory(application) }
            val viewModel = remember(eventId) { factory.createEventDetailViewModel(eventId) }
            
            val event by viewModel.event.collectAsState()
            val error by viewModel.error.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            
            // Show error snackbar if error occurs
            error?.let { errorMessage ->
                LaunchedEffect(errorMessage) {
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage)
                    }
                }
            }
            
            EventDetailScreen(
                event = event,
                onEdit = { 
                    navController.navigate("event/$eventId/edit")
                },
                onDelete = {
                    viewModel.deleteEvent {
                        onNavigateBack()
                    }
                },
                onNavigateToJudgments = { onNavigateToJudgments(eventIdStr) },
                modifier = Modifier
            )
        }
        
        composable("event/{eventId}/edit") { backStackEntry ->
            val eventIdStr = backStackEntry.arguments?.getString("eventId") ?: ""
            val eventId = eventIdStr.toLongOrNull() ?: 0L
            
            val context = LocalContext.current
            val application = remember(context) { 
                context.applicationContext as android.app.Application 
            }
            val factory = remember(application) { ViewModelFactory(application) }
            val viewModel = remember(eventId) { factory.createEventDetailViewModel(eventId) }
            
            val event by viewModel.event.collectAsState()
            val error by viewModel.error.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            
            // Show error snackbar if error occurs
            error?.let { errorMessage ->
                LaunchedEffect(errorMessage) {
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage)
                    }
                }
            }
            
            val currentEvent = event
            if (currentEvent != null) {
                EventEditScreen(
                    event = currentEvent,
                    onSave = { id, request ->
                        viewModel.updateEvent(id, request) {
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() },
                    categoriesFlow = viewModel.categories,
                    formasFlow = viewModel.formas,
                    causesFlow = viewModel.causes,
                    developsFlow = viewModel.develops,
                    effectsFlow = viewModel.effects,
                    modifier = Modifier
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        
        composable("contexts") {
            val context = LocalContext.current
            val application = remember(context) { 
                context.applicationContext as android.app.Application 
            }
            val factory = remember(application) { ViewModelFactory(application) }
            val viewModel: ContextTemplateListViewModel = viewModel(factory = factory)
            
            val templates by viewModel.templates.collectAsState()
            val error by viewModel.error.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            
            // Show error snackbar if error occurs
            error?.let { errorMessage ->
                LaunchedEffect(errorMessage) {
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage)
                    }
                }
            }
            
            ContextTemplateListScreen(
                templates = templates,
                onTemplateClick = { templateId -> onNavigateToContextEditor(templateId) },
                onNewTemplateClick = { onNavigateToContextEditor(null) },
                modifier = Modifier
            )
        }
        
        composable("context/create") {
            val context = LocalContext.current
            val application = remember(context) { 
                context.applicationContext as android.app.Application 
            }
            val factory = remember(application) { ViewModelFactory(application) }
            val viewModel = remember { factory.createContextTemplateEditorViewModel(null) }
            
            val template by viewModel.template.collectAsState()
            val categories by viewModel.categories.collectAsState()
            val formas by viewModel.formas.collectAsState()
            val causes by viewModel.causes.collectAsState()
            val develops by viewModel.develops.collectAsState()
            val effects by viewModel.effects.collectAsState()
            val error by viewModel.error.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            
            // Show error snackbar if error occurs
            error?.let { errorMessage ->
                LaunchedEffect(errorMessage) {
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage)
                    }
                }
            }
            
            ContextTemplateEditorScreen(
                templateId = null,
                initialName = "",
                initialCategoryId = null,
                initialFormaId = null,
                initialCauseId = null,
                initialDevelopId = null,
                initialEffectId = null,
                initialDescription = "",
                categoriesFlow = viewModel.categories,
                formasFlow = viewModel.formas,
                causesFlow = viewModel.causes,
                developsFlow = viewModel.develops,
                effectsFlow = viewModel.effects,
                onSave = { request ->
                    viewModel.saveTemplate(request) {
                        onNavigateBack()
                    }
                },
                onCancel = onNavigateBack,
                modifier = Modifier
            )
        }
        
        composable("context/{templateId}") { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId")?.toIntOrNull()
            
            val context = LocalContext.current
            val application = remember(context) { 
                context.applicationContext as android.app.Application 
            }
            val factory = remember(application) { ViewModelFactory(application) }
            val viewModel = remember(templateId) { factory.createContextTemplateEditorViewModel(templateId) }
            
            val template by viewModel.template.collectAsState()
            val categories by viewModel.categories.collectAsState()
            val formas by viewModel.formas.collectAsState()
            val causes by viewModel.causes.collectAsState()
            val develops by viewModel.develops.collectAsState()
            val effects by viewModel.effects.collectAsState()
            val error by viewModel.error.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            
            // Show error snackbar if error occurs
            error?.let { errorMessage ->
                LaunchedEffect(errorMessage) {
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage)
                    }
                }
            }
            
            ContextTemplateEditorScreen(
                templateId = templateId,
                initialName = template?.name ?: "",
                initialCategoryId = template?.categoryId,
                initialFormaId = template?.formaId,
                initialCauseId = template?.causeId,
                initialDevelopId = template?.developId,
                initialEffectId = template?.effectId,
                initialDescription = template?.description ?: "",
                categoriesFlow = viewModel.categories,
                formasFlow = viewModel.formas,
                causesFlow = viewModel.causes,
                developsFlow = viewModel.develops,
                effectsFlow = viewModel.effects,
                onSave = { request ->
                    viewModel.saveTemplate(request) {
                        onNavigateBack()
                    }
                },
                onCancel = onNavigateBack,
                modifier = Modifier
            )
        }
        
        composable("judgments/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")?.toLongOrNull() ?: 0L
            
            val context = LocalContext.current
            val application = remember(context) { 
                context.applicationContext as android.app.Application 
            }
            val factory = remember(application) { ViewModelFactory(application) }
            val viewModel = remember(eventId) { factory.createJudgmentListViewModel(eventId) }
            
            val event by viewModel.event.collectAsState()
            val judgments by viewModel.judgments.collectAsState()
            val stats by viewModel.stats.collectAsState()
            val error by viewModel.error.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            
            // Show error snackbar if error occurs
            error?.let { errorMessage ->
                LaunchedEffect(errorMessage) {
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage)
                    }
                }
            }
            
            JudgmentListScreen(
                eventTitle = event?.description ?: "Event $eventId",
                judgments = judgments,
                stats = stats,
                onNewJudgmentClick = { onNavigateToJudgmentSubmission(eventId.toString()) },
                modifier = Modifier
            )
        }
        
        composable("judgment/submit/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")?.toLongOrNull() ?: 0L
            
            val context = LocalContext.current
            val application = remember(context) { 
                context.applicationContext as android.app.Application 
            }
            val factory = remember(application) { ViewModelFactory(application) }
            val eventViewModel = remember(eventId) { factory.createEventDetailViewModel(eventId) }
            
            val event by eventViewModel.event.collectAsState()
            
            // Create repository for judgment submission
            val repository = remember(application) {
                val app = application as TruthTrainingApplication
                TruthRepository(application, app.database)
            }
            val scope = rememberCoroutineScope()
            
            JudgmentSubmissionScreen(
                eventId = eventId,
                eventDescription = event?.description ?: "Event $eventId",
                onSubmit = { request ->
                    // Submit judgment in coroutine scope
                    scope.launch {
                        repository.judgmentRepository.submitJudgment(request).fold(
                            onSuccess = { onNavigateBack() },
                            onFailure = { /* TODO: Show error */ }
                        )
                    }
                },
                onCancel = onNavigateBack,
                modifier = Modifier
            )
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

        composable("summary") {
            val viewModel: OverallSummaryViewModel = viewModel(factory = factory)
            val error by viewModel.error.collectAsState()

            error?.let { errorMessage ->
                LaunchedEffect(errorMessage) {
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage)
                        (viewModel.error as? kotlinx.coroutines.flow.MutableStateFlow<String?>)?.value = null
                    }
                }
            }
            OverallSummaryScreen(
                viewModel = viewModel,
                modifier = Modifier
            )
        }

        composable("training") {
            val viewModel: TrainingResultsViewModel = viewModel(factory = factory)
            val error by viewModel.error.collectAsState()

            error?.let { errorMessage ->
                LaunchedEffect(errorMessage) {
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage)
                        (viewModel.error as? kotlinx.coroutines.flow.MutableStateFlow<String?>)?.value = null
                    }
                }
            }
            TrainingResultsScreen(
                viewModel = viewModel,
                modifier = Modifier
            )
        }

        composable("settings") {
            val viewModel: SettingsViewModel = viewModel(factory = factory)
            val error by viewModel.error.collectAsState()

            error?.let { errorMessage ->
                LaunchedEffect(errorMessage) {
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage)
                        (viewModel.error as? kotlinx.coroutines.flow.MutableStateFlow<String?>)?.value = null
                    }
                }
            }
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                modifier = Modifier
            )
        }
    }
}

