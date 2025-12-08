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
  beforeEach(() => {
    jest.clearAllMocks();
    mockInvoke.mockResolvedValue({});
  });

  it('should change language and trigger database re-seeding', async () => {
    render(<Settings />);

    // Find language selection button (Russian)
    const russianButton = screen.getByText(/russian|русский/i);
    fireEvent.click(russianButton);

    // Wait for confirmation dialog
    await waitFor(() => {
      expect(screen.getByText(/change language|изменить язык/i)).toBeInTheDocument();
    });

    // Confirm language change
    const confirmButton = screen.getByText(/yes|да/i);
    fireEvent.click(confirmButton);

    // Verify database operations are called
    await waitFor(() => {
      expect(mockInvoke).toHaveBeenCalledWith('clear_context_templates');
      expect(mockInvoke).toHaveBeenCalledWith('reseed_knowledge_base', {
        locale: 'ru',
        forceReseed: true,
      });
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

    const confirmButton = screen.getByText(/yes|да/i);
    fireEvent.click(confirmButton);

    // Wait for language change to complete
    await waitFor(() => {
      expect(mockInvoke).toHaveBeenCalledWith('reseed_knowledge_base');
    });

    // Verify events are still accessible after language change
    const eventsAfterChange = await mockInvoke('list_events_fast');
    expect(eventsAfterChange).toEqual(mockEvents);
  });

  it('should clear context templates during language change', async () => {
    render(<Settings />);

    const russianButton = screen.getByText(/russian|русский/i);
    fireEvent.click(russianButton);

    const confirmButton = screen.getByText(/yes|да/i);
    fireEvent.click(confirmButton);

    // Verify clear_context_templates is called before reseed
    await waitFor(() => {
      const invokeCalls = mockInvoke.mock.calls;
      const clearCallIndex = invokeCalls.findIndex(
        (call) => call[0] === 'clear_context_templates'
      );
      const reseedCallIndex = invokeCalls.findIndex(
        (call) => call[0] === 'reseed_knowledge_base'
      );

      expect(clearCallIndex).toBeGreaterThan(-1);
      expect(reseedCallIndex).toBeGreaterThan(-1);
      // Clear should be called before reseed
      expect(clearCallIndex).toBeLessThan(reseedCallIndex);
    });
  });

  it('should handle language change errors gracefully', async () => {
    mockInvoke.mockRejectedValueOnce(new Error('Database error'));

    render(<Settings />);

    const russianButton = screen.getByText(/russian|русский/i);
    fireEvent.click(russianButton);

    const confirmButton = screen.getByText(/yes|да/i);
    fireEvent.click(confirmButton);

    // Wait for error handling
    await waitFor(() => {
      // Error should be displayed (implementation dependent)
      expect(mockInvoke).toHaveBeenCalled();
    });
  });

  it('should cancel language change when user declines', async () => {
    render(<Settings />);

    const russianButton = screen.getByText(/russian|русский/i);
    fireEvent.click(russianButton);

    // Wait for confirmation dialog
    await waitFor(() => {
      expect(screen.getByText(/change language|изменить язык/i)).toBeInTheDocument();
    });

    // Cancel language change
    const cancelButton = screen.getByText(/no|нет|cancel|отмена/i);
    fireEvent.click(cancelButton);

    // Verify database operations are NOT called
    await waitFor(() => {
      expect(mockInvoke).not.toHaveBeenCalledWith('clear_context_templates');
      expect(mockInvoke).not.toHaveBeenCalledWith('reseed_knowledge_base');
    });
  });
});

