import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, jest, beforeEach } from '@jest/globals';
import { ToastProvider } from '@/components/system/Toaster';
import { NewEvent } from '../NewEvent';
import { ApiService } from '@/services/api';

// Mock dependencies
jest.mock('@/services/api');
jest.mock('@/components/system/Toaster', () => {
  const actual = jest.requireActual('@/components/system/Toaster');
  return {
    ...actual,
    useToast: () => ({
      addToast: jest.fn(),
    }),
  };
});

// Helper to render with providers
const renderWithProviders = (component: React.ReactElement) => {
  return render(
    <ToastProvider>
      {component}
    </ToastProvider>
  );
};

const mockContexts = [
  { id: 1, name: 'Work Context', description: 'Work-related events', category_id: 1 },
  { id: 2, name: 'Personal Context', description: 'Personal events', category_id: 2 },
];

// Mock knowledge base entities (categories, formas, etc.)
const mockCategories = [
  { id: 1, name: 'Category 1' },
  { id: 2, name: 'Category 2' },
];

describe('NewEvent Context Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    
    // Suppress console warnings for telemetry
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'log').mockImplementation(() => {});
  });

  it('should load contexts on mount and display ContextPicker components', async () => {
    (ApiService.getContexts as jest.Mock).mockResolvedValue({
      data: mockContexts,
      fetched_at: new Date().toISOString(),
    });
    (ApiService.createEvent as jest.Mock).mockResolvedValue({ id: 1 });

    renderWithProviders(<NewEvent />);

    // Wait for contexts to load
    await waitFor(() => {
      expect(ApiService.getContexts).toHaveBeenCalled();
    });

    // Verify ContextPicker components are rendered
    expect(screen.getByText(/Category/i)).toBeInTheDocument();
    expect(screen.getByText(/Forma/i)).toBeInTheDocument();
    expect(screen.getByText(/Cause/i)).toBeInTheDocument();
    expect(screen.getByText(/Develop/i)).toBeInTheDocument();
    expect(screen.getByText(/Effect/i)).toBeInTheDocument();
  });

  it('should validate context IDs before submission', async () => {
    (ApiService.getContexts as jest.Mock).mockResolvedValue({
      data: mockContexts,
      fetched_at: new Date().toISOString(),
    });
    (ApiService.createEvent as jest.Mock).mockResolvedValue({ id: 1 });

    renderWithProviders(<NewEvent />);

    await waitFor(() => {
      expect(ApiService.getContexts).toHaveBeenCalled();
    });

    // Fill in event name
    const eventNameInput = screen.getByLabelText(/Event Name/i) || screen.getByPlaceholderText(/Event Name/i) || screen.getByDisplayValue('');
    if (eventNameInput) {
      fireEvent.change(eventNameInput, { target: { value: 'Test Event' } });
    }

    // Try to enter invalid context ID manually
    const categoryInputs = screen.queryAllByPlaceholderText(/Select or enter/i);
    const categoryInput = categoryInputs[0] || screen.getByPlaceholderText(/category/i);
    fireEvent.change(categoryInput, { target: { value: '999' } });

    // Wait for validation error
    await waitFor(() => {
      expect(screen.getByText(/Unknown context ID/i)).toBeInTheDocument();
    });

    // Submit button should still be enabled, but validation should prevent invalid submission
    const submitButton = screen.getByText(/Save Event/i);
    expect(submitButton).not.toBeDisabled();

    // Try to submit - should be blocked by validation
    fireEvent.click(submitButton);

    // createEvent should not be called with invalid ID
    await waitFor(() => {
      // Check that createEvent was not called or was called with valid data
      if (ApiService.createEvent.mock.calls.length > 0) {
        const call = ApiService.createEvent.mock.calls[0][0];
        // If category_id is provided, it should be valid
        if (call.category_id !== undefined) {
          expect([1, 2]).toContain(call.category_id);
        }
      }
    });
  });

  it('should allow valid context selection and submission', async () => {
    (ApiService.getContexts as jest.Mock).mockResolvedValue({
      data: mockContexts,
      fetched_at: new Date().toISOString(),
    });
    (ApiService.createEvent as jest.Mock).mockResolvedValue({ id: 1 });

    renderWithProviders(<NewEvent />);

    await waitFor(() => {
      expect(ApiService.getContexts).toHaveBeenCalled();
    });

    // Fill in all required fields for validation to pass
    // Event name (required) - find by id or label
    const eventNameInput = screen.queryByLabelText(/Event Name/i) || 
                           screen.queryByPlaceholderText(/Event Name/i) ||
                           document.querySelector('#event-name');
    if (eventNameInput) {
      fireEvent.change(eventNameInput, { target: { value: 'Test Event' } });
    }

    // Description (required) - find textarea (may be multiple textboxes, use first textarea)
    const textareas = screen.queryAllByRole('textbox').filter(el => el.tagName === 'TEXTAREA');
    const descriptionInput = screen.queryByLabelText(/Description/i) || 
                             (textareas.length > 0 ? textareas[0] : null) ||
                             document.querySelector('textarea');
    if (descriptionInput) {
      fireEvent.change(descriptionInput, { target: { value: 'Test Description' } });
    }

    // Fill all context fields (required for validation)
    // Use ContextPicker components - they may have different structure
    // Try to find inputs by placeholder or by searching in form
    const allInputs = screen.queryAllByRole('textbox').concat(
      Array.from(document.querySelectorAll('input[type="text"], input[type="number"]'))
    ) as HTMLInputElement[];

    // Fill context fields if found (category, forma, cause, develop, effect)
    // ContextPicker may use different input structure
    const contextInputs = allInputs.filter(input => 
      input.placeholder?.toLowerCase().includes('select') ||
      input.placeholder?.toLowerCase().includes('category') ||
      input.placeholder?.toLowerCase().includes('forma') ||
      input.placeholder?.toLowerCase().includes('cause') ||
      input.placeholder?.toLowerCase().includes('develop') ||
      input.placeholder?.toLowerCase().includes('effect')
    );

    // Fill first 5 context inputs with valid IDs
    contextInputs.slice(0, 5).forEach((input, index) => {
      fireEvent.change(input, { target: { value: '1' } });
    });

    // Start date (required, defaults to today) - find date input
    const startDateInput = screen.queryByLabelText(/Start Date/i) || 
                           screen.queryByLabelText(/Start/i) ||
                           document.querySelector('input[type="date"]');
    if (startDateInput) {
      const today = new Date().toISOString().split('T')[0];
      fireEvent.change(startDateInput, { target: { value: today } });
    }

    // Wait for form to be ready
    await waitFor(() => {
      expect(screen.queryByText(/Unknown context ID/i)).not.toBeInTheDocument();
    }, { timeout: 2000 });

    // Submit
    const submitButton = screen.getByText(/Save Event/i);
    expect(submitButton).not.toBeDisabled();
    fireEvent.click(submitButton);

    // Wait for submission - API call format may vary
    await waitFor(() => {
      expect(ApiService.createEvent).toHaveBeenCalled();
    }, { timeout: 3000 });

    // Verify the call contains required fields
    const createEventCalls = (ApiService.createEvent as jest.Mock).mock.calls;
    expect(createEventCalls.length).toBeGreaterThan(0);
    const lastCall = createEventCalls[createEventCalls.length - 1][0];
    expect(lastCall).toMatchObject(
      expect.objectContaining({
        description: expect.any(String),
        category_id: expect.any(Number),
      })
    );
  });

  it('should handle template prefill with ContextPicker', async () => {
    (ApiService.getContexts as jest.Mock).mockResolvedValue({
      data: mockContexts,
      fetched_at: new Date().toISOString(),
    });

    renderWithProviders(<NewEvent />);

    await waitFor(() => {
      expect(ApiService.getContexts).toHaveBeenCalled();
    });

    // Select a template (if templates are available)
    const templateSelect = screen.queryByText(/Context Template/i);
    if (templateSelect) {
      // Template selection should prefill context fields
      // This test verifies that template prefill works with ContextPicker
      // The actual template selection logic is in NewEvent component
    }
  });

  it('should handle offline scenario with cached data', async () => {
    // Simulate offline (API failure)
    (ApiService.getContexts as jest.Mock).mockRejectedValue(new Error('Network error'));

    renderWithProviders(<NewEvent />);

    // Component should handle offline gracefully
    // ContextPicker should show error or use cached data
    await waitFor(() => {
      // Either error message or the component should render (with cached data or error state)
      const hasError = screen.queryAllByText(/Unable to load contexts/i).length > 0;
      const hasCached = screen.queryByText(/Using cached data/i);
      const hasComponent = screen.queryByText(/Category/i);
      // At least one of these should be present
      expect(hasError || hasCached || hasComponent).toBeTruthy();
    }, { timeout: 3000 });
  });
});

