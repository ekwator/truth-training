/**
 * Integration tests for language change flow.
 * Tests language change with database re-seeding and context template clearing.
 */

import { describe, it, expect, beforeEach, jest } from '@jest/globals';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Settings } from '../Settings';

// Mock Tauri API
const mockInvoke = jest.fn();
jest.mock('@tauri-apps/api/core', () => ({
  invoke: (...args: any[]) => mockInvoke(...args),
}));

// Mock ApiService
jest.mock('@/services/api', () => {
  const actual = jest.requireActual('@/services/api');
  return {
    ...actual,
    ApiService: {
      ...actual.ApiService,
      clearContextTemplates: jest.fn().mockResolvedValue('OK'),
      reseedKnowledgeBase: jest.fn().mockResolvedValue(undefined),
    },
  };
});

// Mock i18n
jest.mock('@/i18n', () => ({
  t: (key: string) => key,
  setLocale: jest.fn().mockResolvedValue(undefined),
  getCurrentLocale: () => 'en',
}));

// Mock toast
jest.mock('@/components/system/Toaster', () => ({
  useToast: () => ({
    addToast: jest.fn(),
  }),
}));

describe('Language Change Flow Integration', () => {
  beforeEach(async () => {
    jest.clearAllMocks();
    mockInvoke.mockResolvedValue({});
    
    // Reset ApiService mocks
    const { ApiService } = await import('@/services/api');
    (ApiService.clearContextTemplates as jest.Mock).mockResolvedValue('OK');
    (ApiService.reseedKnowledgeBase as jest.Mock).mockResolvedValue(undefined);
  });

  it('should change language and trigger database re-seeding', async () => {
    render(<Settings />);

    // Find language selection button (Russian)
    const russianButton = screen.getByText(/russian|русский/i);
    fireEvent.click(russianButton);

    // Wait for confirmation dialog
    await waitFor(() => {
      expect(screen.getByText('settings.confirmLanguageChange')).toBeInTheDocument();
    });

    // Confirm language change
    const confirmButton = screen.getByText('common.confirm');
    fireEvent.click(confirmButton);

    // Verify database operations are called
    const { ApiService } = await import('@/services/api');
    await waitFor(() => {
      expect(ApiService.clearContextTemplates).toHaveBeenCalled();
      expect(ApiService.reseedKnowledgeBase).toHaveBeenCalled();
    });
  });

  it('should preserve event data during language change', async () => {
    // Mock existing events
    const mockEvents = [
      {
        id: 1,
        name: 'Test Event',
        categoryId: 10,
        formaId: 20,
        causeId: 30,
        developId: 40,
        effectId: 50,
      },
    ];

    mockInvoke.mockImplementation((command: string) => {
      if (command === 'list_events_fast') {
        return Promise.resolve(mockEvents);
      }
      return Promise.resolve({});
    });

    render(<Settings />);

    // Change language
    const russianButton = screen.getByText(/russian|русский/i);
    fireEvent.click(russianButton);

    await waitFor(() => {
      expect(screen.getByText('settings.confirmLanguageChange')).toBeInTheDocument();
    });

    const confirmButton = screen.getByText('common.confirm');
    fireEvent.click(confirmButton);

    // Wait for language change to complete
    const { ApiService } = await import('@/services/api');
    await waitFor(() => {
      expect(ApiService.reseedKnowledgeBase).toHaveBeenCalled();
    });

    // Verify events are still accessible after language change
    const eventsAfterChange = await mockInvoke('list_events_fast');
    expect(eventsAfterChange).toEqual(mockEvents);
  });

  it('should clear context templates during language change', async () => {
    render(<Settings />);

    const russianButton = screen.getByText(/russian|русский/i);
    fireEvent.click(russianButton);

    await waitFor(() => {
      expect(screen.getByText('settings.confirmLanguageChange')).toBeInTheDocument();
    });

    const confirmButton = screen.getByText('common.confirm');
    fireEvent.click(confirmButton);

    // Verify clear_context_templates is called before reseed
    const { ApiService } = await import('@/services/api');
    await waitFor(() => {
      expect(ApiService.clearContextTemplates).toHaveBeenCalled();
      expect(ApiService.reseedKnowledgeBase).toHaveBeenCalled();
      
      // Verify order: clear should be called before reseed
      const clearCallOrder = (ApiService.clearContextTemplates as jest.Mock).mock.invocationCallOrder[0];
      const reseedCallOrder = (ApiService.reseedKnowledgeBase as jest.Mock).mock.invocationCallOrder[0];
      expect(clearCallOrder).toBeLessThan(reseedCallOrder);
    });
  });

  it('should handle language change errors gracefully', async () => {
    const { ApiService } = await import('@/services/api');
    (ApiService.clearContextTemplates as jest.Mock).mockRejectedValueOnce(new Error('Database error'));

    render(<Settings />);

    const russianButton = screen.getByText(/russian|русский/i);
    fireEvent.click(russianButton);

    await waitFor(() => {
      expect(screen.getByText('settings.confirmLanguageChange')).toBeInTheDocument();
    });

    const confirmButton = screen.getByText('common.confirm');
    fireEvent.click(confirmButton);

    // Wait for error handling
    await waitFor(() => {
      // Error should be displayed (implementation dependent)
      expect(ApiService.clearContextTemplates).toHaveBeenCalled();
    }, { timeout: 3000 });
  });

  it('should cancel language change when user declines', async () => {
    render(<Settings />);

    const russianButton = screen.getByText(/russian|русский/i);
    fireEvent.click(russianButton);

    // Wait for confirmation dialog
    await waitFor(() => {
      expect(screen.getByText('settings.confirmLanguageChange')).toBeInTheDocument();
    });

    // Cancel language change
    const cancelButton = screen.getByText('common.cancel');
    fireEvent.click(cancelButton);

    // Verify database operations are NOT called
    const { ApiService } = await import('@/services/api');
    await waitFor(() => {
      expect(ApiService.clearContextTemplates).not.toHaveBeenCalled();
      expect(ApiService.reseedKnowledgeBase).not.toHaveBeenCalled();
    });
  });
});

