package com.truth.training.client.ui.compose

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.ui.DashboardViewModel
import com.truth.training.client.ui.compose.nodes.NodesViewModel
import com.truth.training.client.ui.events.EventListViewModel
import com.truth.training.client.ui.events.EventDetailViewModel
import com.truth.training.client.ui.events.EventCreateViewModel
import com.truth.training.client.ui.contexts.ContextTemplateListViewModel
import com.truth.training.client.ui.contexts.ContextTemplateEditorViewModel
import com.truth.training.client.ui.judgments.JudgmentListViewModel
import com.truth.training.client.ui.summary.OverallSummaryViewModel
import com.truth.training.client.ui.training.TrainingResultsViewModel
import com.truth.training.client.ui.settings.SettingsViewModel

/**
 * ViewModelFactory for creating ViewModel instances with Application dependency.
 * Supports all ViewModels used in the app.
 */
class ViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    
    private val app = application as? TruthTrainingApplication
        ?: throw IllegalStateException("Application must be TruthTrainingApplication")
    
    private val database: TruthDatabase = app.database
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(application, database) as T
            }
            modelClass.isAssignableFrom(NodesViewModel::class.java) -> {
                NodesViewModel(application) as T
            }
            modelClass.isAssignableFrom(EventListViewModel::class.java) -> {
                EventListViewModel(application) as T
            }
            modelClass.isAssignableFrom(EventCreateViewModel::class.java) -> {
                EventCreateViewModel(application) as T
            }
            modelClass.isAssignableFrom(ContextTemplateListViewModel::class.java) -> {
                ContextTemplateListViewModel(application) as T
            }
            modelClass.isAssignableFrom(ContextTemplateEditorViewModel::class.java) -> {
                // For editor, we need templateId - this will be handled via SavedStateHandle
                // For now, create without templateId (new template)
                ContextTemplateEditorViewModel(application) as T
            }
            modelClass.isAssignableFrom(JudgmentListViewModel::class.java) -> {
                // For judgment list, we need eventId - this will be handled via SavedStateHandle
                // For now, throw - should be created with eventId parameter
                throw IllegalArgumentException("JudgmentListViewModel requires eventId parameter. Use createWithEventId() instead.")
            }
            modelClass.isAssignableFrom(EventDetailViewModel::class.java) -> {
                // For event detail, we need eventId - this will be handled via SavedStateHandle
                // For now, throw - should be created with eventId parameter
                throw IllegalArgumentException("EventDetailViewModel requires eventId parameter. Use createWithEventId() instead.")
            }
            modelClass.isAssignableFrom(OverallSummaryViewModel::class.java) -> {
                OverallSummaryViewModel(application) as T
            }
            modelClass.isAssignableFrom(TrainingResultsViewModel::class.java) -> {
                TrainingResultsViewModel(application) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(application) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
    
    /**
     * Create EventDetailViewModel with eventId.
     */
    fun createEventDetailViewModel(eventId: Long): EventDetailViewModel {
        return EventDetailViewModel(application, eventId)
    }
    
    /**
     * Create JudgmentListViewModel with eventId.
     */
    fun createJudgmentListViewModel(eventId: Long): JudgmentListViewModel {
        return JudgmentListViewModel(application, eventId)
    }
    
    /**
     * Create ContextTemplateEditorViewModel with templateId (null for new template).
     */
    fun createContextTemplateEditorViewModel(templateId: Int? = null): ContextTemplateEditorViewModel {
        return ContextTemplateEditorViewModel(application, templateId)
    }
}

