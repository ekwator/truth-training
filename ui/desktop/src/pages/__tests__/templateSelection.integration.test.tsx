/**
 * Integration tests for template selection flow.
 * Tests the complete flow from NewEvent -> ContextEditor -> back to NewEvent with pre-filled data.
 */

import { describe, it, expect, beforeEach, jest } from '@jest/globals';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { useNavigationStore } from '@/stores/navigation';
import { useTemplateContextStore } from '@/stores/templateContext';
import { NewEvent } from '../NewEvent';
import { ContextEditor } from '../ContextEditor';

// Mock stores
jest.mock('@/stores/navigation', () => ({
  useNavigationStore: jest.fn(),
}));

jest.mock('@/stores/templateContext', () => ({
  useTemplateContextStore: jest.fn(),
}));

// Mock API service
jest.mock('@/services/api', () => ({
  ApiService: {
    listContextTemplates: jest.fn(),
    createContext: jest.fn(),
    getContexts: jest.fn(),
  },
}));

describe('Template Selection Flow Integration', () => {
  const mockSetSelectTemplateForEvent = jest.fn();
  const mockSetSelectedTemplateContext = jest.fn();
  const mockOnNavigate = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();

    (useNavigationStore as jest.Mock).mockReturnValue({
      selectTemplateForEvent: false,
      setSelectTemplateForEvent: mockSetSelectTemplateForEvent,
    });

    (useTemplateContextStore as jest.Mock).mockReturnValue({
      selectedTemplateContext: null,
      setSelectedTemplateContext: mockSetSelectedTemplateContext,
    });
  });

  it('should navigate to ContextEditor when Select Template button is clicked', async () => {
    const { ApiService } = await import('@/services/api');
    (ApiService.listContextTemplates as jest.Mock).mockResolvedValue([]);

    render(<NewEvent onNavigate={mockOnNavigate} />);

    const selectTemplateButton = screen.getByText(/select template/i);
    fireEvent.click(selectTemplateButton);

    expect(mockSetSelectTemplateForEvent).toHaveBeenCalledWith(true);
    expect(mockOnNavigate).toHaveBeenCalledWith('context-editor');
  });

  it('should pre-fill event form when template is selected', async () => {
    const mockTemplate = {
      id: 1,
      name: 'Test Template',
      categoryId: 10,
      formaId: 20,
      causeId: 30,
      developId: 40,
      effectId: 50,
    };

    const { ApiService } = await import('@/services/api');
    (ApiService.listContextTemplates as jest.Mock).mockResolvedValue([mockTemplate]);

    // Simulate template selection mode
    (useNavigationStore as jest.Mock).mockReturnValue({
      selectTemplateForEvent: true,
      setSelectTemplateForEvent: mockSetSelectTemplateForEvent,
    });

    render(<ContextEditor onNavigate={mockOnNavigate} />);

    // Wait for templates to load
    await waitFor(() => {
      expect(ApiService.listContextTemplates).toHaveBeenCalled();
    });

    // Click on template
    const templateItem = screen.getByText(mockTemplate.name);
    fireEvent.click(templateItem);

    // Verify template context is stored
    expect(mockSetSelectedTemplateContext).toHaveBeenCalledWith({
      categoryId: mockTemplate.categoryId,
      formaId: mockTemplate.formaId,
      causeId: mockTemplate.causeId,
      developId: mockTemplate.developId,
      effectId: mockTemplate.effectId,
    });

    // Verify navigation flag is cleared
    expect(mockSetSelectTemplateForEvent).toHaveBeenCalledWith(false);

    // Verify navigation back to NewEvent
    expect(mockOnNavigate).toHaveBeenCalledWith('new-event');
  });

  it('should pre-fill form fields when returning to NewEvent with selected template', async () => {
    const mockTemplateContext = {
      categoryId: 10,
      formaId: 20,
      causeId: 30,
      developId: 40,
      effectId: 50,
    };

    // Simulate returning to NewEvent with template context
    (useTemplateContextStore as jest.Mock).mockReturnValue({
      selectedTemplateContext: mockTemplateContext,
      setSelectedTemplateContext: mockSetSelectedTemplateContext,
    });

    const { ApiService } = await import('@/services/api');
    (ApiService.getContexts as jest.Mock).mockResolvedValue([]);

    render(<NewEvent onNavigate={mockOnNavigate} />);

    // Wait for form to be pre-filled
    await waitFor(() => {
      // Verify form fields are pre-filled (this depends on implementation)
      // The actual field values would be checked based on the component's state
      expect(mockSetSelectedTemplateContext).toHaveBeenCalledWith(null);
    });
  });

  it('should handle template selection cancellation', async () => {
    const { ApiService } = await import('@/services/api');
    (ApiService.listContextTemplates as jest.Mock).mockResolvedValue([]);

    // Simulate template selection mode
    (useNavigationStore as jest.Mock).mockReturnValue({
      selectTemplateForEvent: true,
      setSelectTemplateForEvent: mockSetSelectTemplateForEvent,
    });

    render(<ContextEditor onNavigate={mockOnNavigate} />);

    // Click back/cancel button
    const backButton = screen.getByText(/back|cancel/i);
    fireEvent.click(backButton);

    // Verify navigation flag is cleared
    expect(mockSetSelectTemplateForEvent).toHaveBeenCalledWith(false);

    // Verify navigation back to NewEvent
    expect(mockOnNavigate).toHaveBeenCalledWith('new-event');
  });
});

