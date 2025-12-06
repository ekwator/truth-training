package com.truth.training.client.integration

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.ui.DashboardViewModel
import com.truth.training.client.ui.compose.ViewModelFactory
import com.truth.training.client.ui.compose.nodes.NodesViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for ViewModelFactory.
 * Verifies factory works in Activity context and creates ViewModels correctly.
 * 
 * Task: T010
 */
@RunWith(AndroidJUnit4::class)
class ViewModelFactoryTest {
    
    private lateinit var application: Application
    private lateinit var factory: ViewModelFactory
    
    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext<TruthTrainingApplication>()
        factory = ViewModelFactory(application)
    }
    
    @Test
    fun testFactoryCreatesDashboardViewModel() {
        val viewModel = factory.create(DashboardViewModel::class.java)
        
        assertNotNull("DashboardViewModel should be created", viewModel)
        assertTrue("Should be instance of DashboardViewModel", viewModel is DashboardViewModel)
    }
    
    @Test
    fun testFactoryCreatesNodesViewModel() {
        val viewModel = factory.create(NodesViewModel::class.java)
        
        assertNotNull("NodesViewModel should be created", viewModel)
        assertTrue("Should be instance of NodesViewModel", viewModel is NodesViewModel)
    }
    
    @Test
    fun testFactoryThrowsExceptionForUnknownViewModel() {
        class UnknownViewModel : androidx.lifecycle.ViewModel()
        
        try {
            factory.create(UnknownViewModel::class.java)
            fail("Should throw IllegalArgumentException for unknown ViewModel type")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception message should mention unknown ViewModel", 
                e.message?.contains("Unknown ViewModel") == true)
        }
    }
    
    @Test
    fun testFactoryIsReusable() {
        // Create multiple ViewModels to verify factory is reusable
        val viewModel1 = factory.create(DashboardViewModel::class.java)
        val viewModel2 = factory.create(DashboardViewModel::class.java)
        val viewModel3 = factory.create(NodesViewModel::class.java)
        
        assertNotNull("First DashboardViewModel should be created", viewModel1)
        assertNotNull("Second DashboardViewModel should be created", viewModel2)
        assertNotNull("NodesViewModel should be created", viewModel3)
        
        // ViewModels should be different instances (factory creates new instances)
        assertNotSame("ViewModels should be different instances", viewModel1, viewModel2)
    }
}

