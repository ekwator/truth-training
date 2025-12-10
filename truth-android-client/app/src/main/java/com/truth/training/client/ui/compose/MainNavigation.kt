package com.truth.training.client.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.truth.training.client.ui.compose.events.*
import com.truth.training.client.ui.compose.contexts.*
import com.truth.training.client.ui.compose.judgments.*
import com.truth.training.client.ui.compose.nodes.NodeDetailScreen
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
                onNavigateToEvents = { 
                    // Clear viewJudgments flag when explicitly navigating to events via "View Events"
                    try {
                        navController.getBackStackEntry("events").savedStateHandle["viewJudgments"] = false
                    } catch (e: Exception) {
                        // Entry doesn't exist yet, will be set when navigating
                    }
                    onNavigateToEvents()
                },
                onNavigateToContexts = {
                    // Clear selectTemplateForEvent flag when navigating from Dashboard
                    try {
                        val contextsEntry = navController.getBackStackEntry("contexts")
                        contextsEntry.savedStateHandle["selectTemplateForEvent"] = false
                    } catch (e: Exception) {
                        // Entry doesn't exist yet, will be cleared after navigation
                    }
                    onNavigateToContexts()
                    // Ensure flag is cleared after navigation
                    scope.launch {
                        kotlinx.coroutines.delay(100)
                        try {
                            val contextsEntry = navController.getBackStackEntry("contexts")
                            contextsEntry.savedStateHandle["selectTemplateForEvent"] = false
                        } catch (e: Exception) {
                            // Entry might not be ready yet
                        }
                    }
                },
                onNavigateToJudgments = { 
                    // Navigate to events list with viewJudgments flag
                    // Set flag in savedStateHandle before navigation
                    try {
                        // Try to get existing entry first
                        val existingEntry = navController.getBackStackEntry("events")
                        existingEntry.savedStateHandle["viewJudgments"] = true
                        navController.navigate("events") {
                            launchSingleTop = true
                        }
                    } catch (e: Exception) {
                        // Entry doesn't exist yet, navigate first then set flag
                        navController.navigate("events") {
                            launchSingleTop = true
                        }
                        // Set flag after navigation
                        scope.launch {
                            kotlinx.coroutines.delay(100)
                            try {
                                navController.getBackStackEntry("events").savedStateHandle["viewJudgments"] = true
                            } catch (ex: Exception) {
                                // Entry still not ready, will be handled in composable
                            }
                        }
                    }
                },
                onSyncNow = { viewModel.refresh() },
                onNavigateToSummary = onNavigateToSummary,
                onNavigateToTraining = onNavigateToTraining,
                onNavigateToSettings = onNavigateToSettings
            )
        }
        
        // Route for events list - supports optional viewJudgments parameter
        composable("events") { backStackEntry ->
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
            
            // Track viewJudgments flag from savedStateHandle
            var viewJudgments by remember { mutableStateOf(false) }
            
            // Check savedStateHandle for viewJudgments flag
            LaunchedEffect(backStackEntry) {
                // Check immediately
                viewJudgments = backStackEntry.savedStateHandle.get<Boolean>("viewJudgments") ?: false
                // Also check after a delay in case flag is set after composable is created
                kotlinx.coroutines.delay(150)
                viewJudgments = backStackEntry.savedStateHandle.get<Boolean>("viewJudgments") ?: false
            }
            
            // Also listen to savedStateHandle changes
            LaunchedEffect(backStackEntry.savedStateHandle) {
                viewJudgments = backStackEntry.savedStateHandle.get<Boolean>("viewJudgments") ?: false
            }
            
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
                onEventClick = { eventId -> 
                    // Check flag value at click time (may have been set after composable creation)
                    // Don't clear the flag - it should persist until user explicitly navigates via "View Events"
                    val shouldViewJudgments = backStackEntry.savedStateHandle.get<Boolean>("viewJudgments") ?: false
                    
                    if (shouldViewJudgments) {
                        // Navigate to judgments screen for this event
                        // Flag remains set so subsequent event selections also go to judgments
                        onNavigateToJudgments(eventId.toString())
                    } else {
                        // Navigate to event details
                        onNavigateToEventDetails(eventId.toString())
                    }
                },
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
            
            // Check for template context from savedStateHandle (set when template is selected from contexts screen)
            val templateCategoryId = backStackEntry.savedStateHandle.get<Int?>("selectedTemplateCategoryId")
            val templateFormaId = backStackEntry.savedStateHandle.get<Int?>("selectedTemplateFormaId")
            val templateCauseId = backStackEntry.savedStateHandle.get<Int?>("selectedTemplateCauseId")
            val templateDevelopId = backStackEntry.savedStateHandle.get<Int?>("selectedTemplateDevelopId")
            val templateEffectId = backStackEntry.savedStateHandle.get<Int?>("selectedTemplateEffectId")
            
            // Update ViewModel when template context is received from contexts screen
            LaunchedEffect(templateCategoryId, templateFormaId, templateCauseId, templateDevelopId, templateEffectId) {
                if (templateCategoryId != null || templateFormaId != null || templateCauseId != null || 
                    templateDevelopId != null || templateEffectId != null) {
                    viewModel.setSelectedTemplateContext(
                        com.truth.training.client.ui.compose.events.ContextFields(
                            categoryId = templateCategoryId,
                            formaId = templateFormaId,
                            causeId = templateCauseId,
                            developId = templateDevelopId,
                            effectId = templateEffectId
                        )
                    )
                    // Clear savedStateHandle values after using them
                    backStackEntry.savedStateHandle["selectedTemplateCategoryId"] = null
                    backStackEntry.savedStateHandle["selectedTemplateFormaId"] = null
                    backStackEntry.savedStateHandle["selectedTemplateCauseId"] = null
                    backStackEntry.savedStateHandle["selectedTemplateDevelopId"] = null
                    backStackEntry.savedStateHandle["selectedTemplateEffectId"] = null
                }
            }
            
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
                    // Navigate to contexts screen with flag to select template for event
                    // Set flag in contexts entry's savedStateHandle before navigation
                    try {
                        val contextsEntry = navController.getBackStackEntry("contexts")
                        contextsEntry.savedStateHandle["selectTemplateForEvent"] = true
                    } catch (e: Exception) {
                        // Entry doesn't exist yet, will be set after navigation
                    }
                    navController.navigate("contexts") {
                        // Don't add to back stack if already there, just pop to it
                        popUpTo("event/create") { inclusive = false }
                    }
                    // Ensure flag is set after navigation
                    scope.launch {
                        kotlinx.coroutines.delay(100)
                        try {
                            val contextsEntry = navController.getBackStackEntry("contexts")
                            contextsEntry.savedStateHandle["selectTemplateForEvent"] = true
                        } catch (e: Exception) {
                            // Entry might not be ready yet
                        }
                    }
                },
                categoriesFlow = viewModel.categories,
                formasFlow = viewModel.formas,
                causesFlow = viewModel.causes,
                developsFlow = viewModel.develops,
                effectsFlow = viewModel.effects,
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
            
            // Get current locale to force ViewModel recreation when language changes
            // Use LaunchedEffect to track locale changes and recreate ViewModel
            val appConfig = remember { com.truth.training.client.data.config.AppConfig(context) }
            var currentLocale by remember { mutableStateOf(appConfig.locale) }
            
            // Update locale when backStackEntry changes (e.g., after language change and activity restart)
            LaunchedEffect(backStackEntry) {
                currentLocale = appConfig.locale
            }
            
            // Recreate ViewModel when locale changes to ensure flows are refreshed
            val viewModel = remember(eventId, currentLocale) { factory.createEventDetailViewModel(eventId) }
            
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
                viewModel = viewModel,
                onEdit = { 
                    navController.navigate("event/$eventId/edit")
                },
                onDelete = {
                    viewModel.deleteEvent {
                        onNavigateBack()
                    }
                },
                onNavigateToJudgments = { onNavigateToJudgments(eventIdStr) },
                categoriesFlow = viewModel.categories,
                formasFlow = viewModel.formas,
                causesFlow = viewModel.causes,
                developsFlow = viewModel.develops,
                effectsFlow = viewModel.effects,
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
        
        composable("contexts") { backStackEntry ->
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
            
            // Check if we're selecting template for event creation
            // Use LaunchedEffect to track changes in savedStateHandle
            var selectTemplateForEvent by remember { mutableStateOf(false) }
            LaunchedEffect(backStackEntry) {
                selectTemplateForEvent = backStackEntry.savedStateHandle.get<Boolean>("selectTemplateForEvent") ?: false
                // Also check after a delay in case flag is set after composable is created
                kotlinx.coroutines.delay(150)
                selectTemplateForEvent = backStackEntry.savedStateHandle.get<Boolean>("selectTemplateForEvent") ?: false
            }
            // Also listen to savedStateHandle changes
            LaunchedEffect(backStackEntry.savedStateHandle) {
                selectTemplateForEvent = backStackEntry.savedStateHandle.get<Boolean>("selectTemplateForEvent") ?: false
            }
            
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
                onTemplateClick = { templateId -> 
                    val template = templates.find { it.id == templateId }
                    if (template != null) {
                        if (selectTemplateForEvent) {
                            // Selecting template for event creation
                            // Store template context in event/create entry's savedStateHandle
                            try {
                                val eventCreateEntry = navController.getBackStackEntry("event/create")
                                // Store template context data in event/create entry
                                eventCreateEntry.savedStateHandle["selectedTemplateCategoryId"] = template.categoryId
                                eventCreateEntry.savedStateHandle["selectedTemplateFormaId"] = template.formaId
                                eventCreateEntry.savedStateHandle["selectedTemplateCauseId"] = template.causeId
                                eventCreateEntry.savedStateHandle["selectedTemplateDevelopId"] = template.developId
                                eventCreateEntry.savedStateHandle["selectedTemplateEffectId"] = template.effectId
                                // Clear the flag
                                backStackEntry.savedStateHandle["selectTemplateForEvent"] = false
                                // Navigate back to event create screen
                                navController.popBackStack()
                            } catch (e: Exception) {
                                // Event create entry not found, just navigate back
                                backStackEntry.savedStateHandle["selectTemplateForEvent"] = false
                                navController.popBackStack()
                            }
                        } else {
                            // Normal flow: navigate to create new template with selected template's fields
                            // Editing is not allowed, only creating new templates
                            // Store template data in the "context/create" entry's savedStateHandle
                            try {
                                val createEntry = navController.getBackStackEntry("context/create")
                                // Entry exists, set data directly
                                createEntry.savedStateHandle["selectedTemplateId"] = templateId
                                createEntry.savedStateHandle["selectedTemplateName"] = template.name
                                createEntry.savedStateHandle["selectedTemplateCategoryId"] = template.categoryId
                                createEntry.savedStateHandle["selectedTemplateFormaId"] = template.formaId
                                createEntry.savedStateHandle["selectedTemplateCauseId"] = template.causeId
                                createEntry.savedStateHandle["selectedTemplateDevelopId"] = template.developId
                                createEntry.savedStateHandle["selectedTemplateEffectId"] = template.effectId
                                createEntry.savedStateHandle["selectedTemplateDescription"] = template.description ?: ""
                            } catch (e: Exception) {
                                // Entry doesn't exist yet, store data in current entry to be copied on navigation
                                backStackEntry.savedStateHandle["selectedTemplateId"] = templateId
                                backStackEntry.savedStateHandle["selectedTemplateName"] = template.name
                                backStackEntry.savedStateHandle["selectedTemplateCategoryId"] = template.categoryId
                                backStackEntry.savedStateHandle["selectedTemplateFormaId"] = template.formaId
                                backStackEntry.savedStateHandle["selectedTemplateCauseId"] = template.causeId
                                backStackEntry.savedStateHandle["selectedTemplateDevelopId"] = template.developId
                                backStackEntry.savedStateHandle["selectedTemplateEffectId"] = template.effectId
                                backStackEntry.savedStateHandle["selectedTemplateDescription"] = template.description ?: ""
                            }
                            
                            // Navigate to create screen
                            navController.navigate("context/create") {
                                launchSingleTop = true
                            }
                            
                            // After navigation, ensure data is in the create entry's savedStateHandle
                            scope.launch {
                                kotlinx.coroutines.delay(100)
                                try {
                                    val createEntry = navController.getBackStackEntry("context/create")
                                    createEntry.savedStateHandle["selectedTemplateId"] = templateId
                                    createEntry.savedStateHandle["selectedTemplateName"] = template.name
                                    createEntry.savedStateHandle["selectedTemplateCategoryId"] = template.categoryId
                                    createEntry.savedStateHandle["selectedTemplateFormaId"] = template.formaId
                                    createEntry.savedStateHandle["selectedTemplateCauseId"] = template.causeId
                                    createEntry.savedStateHandle["selectedTemplateDevelopId"] = template.developId
                                    createEntry.savedStateHandle["selectedTemplateEffectId"] = template.effectId
                                    createEntry.savedStateHandle["selectedTemplateDescription"] = template.description ?: ""
                                } catch (e: Exception) {
                                    // Entry might not be ready yet
                                }
                            }
                        }
                    }
                },
                onNewTemplateClick = { navController.navigate("context/create") },
                categoriesFlow = viewModel.categories,
                formasFlow = viewModel.formas,
                causesFlow = viewModel.causes,
                developsFlow = viewModel.develops,
                effectsFlow = viewModel.effects,
                modifier = Modifier
            )
        }
        
        composable("context/create") { backStackEntry ->
            val context = LocalContext.current
            val application = remember(context) { 
                context.applicationContext as android.app.Application 
            }
            val factory = remember(application) { ViewModelFactory(application) }
            val viewModel = remember { factory.createContextTemplateEditorViewModel(null) }
            
            // Get selected template data from savedStateHandle (if template was selected from list)
            val selectedTemplateId = backStackEntry.savedStateHandle.get<Int>("selectedTemplateId")
            val selectedTemplateName = backStackEntry.savedStateHandle.get<String>("selectedTemplateName") ?: ""
            val selectedTemplateCategoryId = backStackEntry.savedStateHandle.get<Int?>("selectedTemplateCategoryId")
            val selectedTemplateFormaId = backStackEntry.savedStateHandle.get<Int?>("selectedTemplateFormaId")
            val selectedTemplateCauseId = backStackEntry.savedStateHandle.get<Int?>("selectedTemplateCauseId")
            val selectedTemplateDevelopId = backStackEntry.savedStateHandle.get<Int?>("selectedTemplateDevelopId")
            val selectedTemplateEffectId = backStackEntry.savedStateHandle.get<Int?>("selectedTemplateEffectId")
            val selectedTemplateDescription = backStackEntry.savedStateHandle.get<String>("selectedTemplateDescription") ?: ""
            
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
                initialName = selectedTemplateName,
                initialCategoryId = selectedTemplateCategoryId,
                initialFormaId = selectedTemplateFormaId,
                initialCauseId = selectedTemplateCauseId,
                initialDevelopId = selectedTemplateDevelopId,
                initialEffectId = selectedTemplateEffectId,
                initialDescription = selectedTemplateDescription,
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
        
        // Removed "context/{templateId}" route - editing is not allowed, only creating new templates
        // When clicking on a template in the list, navigate to "context/create" with template fields pre-filled
        
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
            NodesScreen(
                viewModel = viewModel,
                onNodeClick = { nodeId ->
                    navController.navigate("node/$nodeId")
                }
            )
        }
        
        composable(
            route = "node/{nodeId}",
            arguments = listOf(
                navArgument("nodeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val nodeIdStr = backStackEntry.arguments?.getLong("nodeId") ?: 0L
            val nodeId = nodeIdStr
            
            val context = LocalContext.current
            val application = remember(context) { 
                context.applicationContext as android.app.Application 
            }
            val factory = remember(application) { ViewModelFactory(application) }
            val viewModel = remember(nodeId) { factory.createNodeDetailViewModel(nodeId) }
            
            NodeDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
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
                onNavigateToNodes = {
                    navController.navigate("nodes")
                },
                modifier = Modifier
            )
        }
    }
}

