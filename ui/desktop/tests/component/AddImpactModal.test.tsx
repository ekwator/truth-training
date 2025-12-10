/**
 * Component tests for AddImpactModal
 * Tests form validation, slider interaction, and submit functionality
 */

import React from 'react';
import { describe, it, expect, beforeEach, jest } from '@jest/globals';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AddImpactModal } from '@/components/impacts/AddImpactModal';
import { ThemeProvider } from '@/components/system/ThemeProvider';
import { ToastProvider } from '@/components/system/Toaster';
import { ApiService } from '@/services/api';

// Mock ApiService
jest.mock('@/services/api', () => ({
  ApiService: {
    addImpact: jest.fn(),
  },
}));

// Mock window.matchMedia for jsdom
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

// Mock @headlessui/react Portal
jest.mock('@headlessui/react', () => {
  const actual = jest.requireActual('@headlessui/react');
  return {
    ...actual,
    Portal: ({ children }: any) => children,
  };
});

describe('AddImpactModal Component Tests', () => {
  const mockEventId = 1;
  const mockOnClose = jest.fn();
  const mockOnImpactAdded = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderModal = (isOpen: boolean = true) => {
    return render(
      <ThemeProvider>
        <ToastProvider>
          <AddImpactModal
            isOpen={isOpen}
            onClose={mockOnClose}
            eventId={mockEventId}
            onImpactAdded={mockOnImpactAdded}
          />
        </ToastProvider>
      </ThemeProvider>
    );
  };

  describe('Modal Rendering', () => {
    it('should render modal when isOpen is true', () => {
      renderModal(true);
      expect(screen.getByText(/Add Impact/i)).toBeInTheDocument();
    });

    it('should not render modal when isOpen is false', () => {
      renderModal(false);
      expect(screen.queryByText(/Add Impact/i)).not.toBeInTheDocument();
    });

    it('should display impact level slider', () => {
      renderModal(true);
      const slider = screen.getByLabelText(/Impact Level/i);
      expect(slider).toBeInTheDocument();
      expect(slider).toHaveAttribute('type', 'range');
      expect(slider).toHaveAttribute('min', '1');
      expect(slider).toHaveAttribute('max', '5');
    });

    it('should display notes textarea', () => {
      renderModal(true);
      const notesField = screen.getByLabelText(/Notes/i);
      expect(notesField).toBeInTheDocument();
    });
  });

  describe('Form Validation', () => {
    it('should have default impact level of 3', () => {
      renderModal(true);
      const slider = screen.getByLabelText(/Impact Level/i) as HTMLInputElement;
      expect(slider.value).toBe('3');
    });

    it('should update impact level when slider changes', () => {
      renderModal(true);
      const slider = screen.getByLabelText(/Impact Level/i) as HTMLInputElement;
      
      fireEvent.change(slider, { target: { value: '5' } });
      expect(slider.value).toBe('5');
      
      fireEvent.change(slider, { target: { value: '1' } });
      expect(slider.value).toBe('1');
    });

    it('should update notes when textarea changes', () => {
      renderModal(true);
      const notesField = screen.getByLabelText(/Notes/i) as HTMLTextAreaElement;
      
      fireEvent.change(notesField, { target: { value: 'Test notes' } });
      expect(notesField.value).toBe('Test notes');
    });
  });

  describe('Submit Functionality', () => {
    it('should call ApiService.addImpact on submit', async () => {
      const mockImpact = {
        id: '1',
        event_id: mockEventId,
        impact_level: 4,
        notes: 'Test notes',
        created_at: new Date().toISOString(),
      };

      (ApiService.addImpact as jest.Mock).mockResolvedValue(mockImpact);

      renderModal(true);
      
      // Set impact level
      const slider = screen.getByLabelText(/Impact Level/i);
      fireEvent.change(slider, { target: { value: '4' } });
      
      // Add notes
      const notesField = screen.getByLabelText(/Notes/i);
      fireEvent.change(notesField, { target: { value: 'Test notes' } });
      
      // Submit
      const submitButton = screen.getByRole('button', { name: /Save/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(ApiService.addImpact).toHaveBeenCalledWith({
          event_id: mockEventId,
          impact_level: 4,
          notes: 'Test notes',
        });
      });
    });

    it('should call onImpactAdded callback after successful submit', async () => {
      const mockImpact = {
        id: '1',
        event_id: mockEventId,
        impact_level: 4,
        notes: 'Test notes',
        created_at: new Date().toISOString(),
      };

      (ApiService.addImpact as jest.Mock).mockResolvedValue(mockImpact);

      renderModal(true);
      
      const slider = screen.getByLabelText(/Impact Level/i);
      fireEvent.change(slider, { target: { value: '4' } });
      
      const submitButton = screen.getByRole('button', { name: /Save/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(mockOnImpactAdded).toHaveBeenCalledWith(mockImpact);
      });
    });

    it('should close modal after successful submit', async () => {
      const mockImpact = {
        id: '1',
        event_id: mockEventId,
        impact_level: 4,
        created_at: new Date().toISOString(),
      };

      (ApiService.addImpact as jest.Mock).mockResolvedValue(mockImpact);

      renderModal(true);
      
      const slider = screen.getByLabelText(/Impact Level/i);
      fireEvent.change(slider, { target: { value: '4' } });
      
      const submitButton = screen.getByRole('button', { name: /Save/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(mockOnClose).toHaveBeenCalled();
      });
    });
  });

  describe('Cancel Functionality', () => {
    it('should call onClose when cancel button is clicked', () => {
      renderModal(true);
      
      const cancelButton = screen.getByRole('button', { name: /Cancel/i });
      fireEvent.click(cancelButton);
      
      expect(mockOnClose).toHaveBeenCalled();
    });

    it('should call onClose when close icon is clicked', () => {
      renderModal(true);
      
      const closeButton = screen.getByRole('button', { name: /Close/i }) || 
                         screen.getByLabelText(/Close/i);
      if (closeButton) {
        fireEvent.click(closeButton);
        expect(mockOnClose).toHaveBeenCalled();
      }
    });
  });

  describe('Error Handling', () => {
    it('should display error message when API call fails', async () => {
      const errorMessage = 'Failed to add impact';
      (ApiService.addImpact as jest.Mock).mockRejectedValue(new Error(errorMessage));

      renderModal(true);
      
      const slider = screen.getByLabelText(/Impact Level/i);
      fireEvent.change(slider, { target: { value: '4' } });
      
      const submitButton = screen.getByRole('button', { name: /Save/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(errorMessage)).toBeInTheDocument();
      });
    });
  });
});

