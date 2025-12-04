import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, jest, beforeEach } from '@jest/globals';
import { ContextPicker } from '../ContextPicker';
import { ApiService } from '@/services/api';

// Mock ApiService
jest.mock('@/services/api', () => ({
  ApiService: {
    getContexts: jest.fn(),
  },
}));

const mockContexts = [
  { id: 1, name: 'Work Context', description: 'Work-related events', category_id: 1 },
  { id: 2, name: 'Personal Context', description: 'Personal events', category_id: 2 },
  { id: 3, name: 'Social Context', description: 'Social interactions', category_id: 3 },
];

describe('ContextPicker Contract Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Suppress console.warn for telemetry events
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'log').mockImplementation(() => {});
  });

  it('should show loading state on mount', async () => {
    (ApiService.getContexts as jest.Mock).mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ data: mockContexts, fetched_at: new Date().toISOString() }), 100))
    );

    render(<ContextPicker label="Context" onChange={() => {}} />);
    
    expect(screen.getByText(/Loading contexts/i)).toBeInTheDocument();
    
    await waitFor(() => {
      expect(screen.queryByText(/Loading contexts/i)).not.toBeInTheDocument();
    });
  });

  it('should load and display contexts successfully', async () => {
    (ApiService.getContexts as jest.Mock).mockResolvedValue({
      data: mockContexts,
      fetched_at: new Date().toISOString(),
    });

    render(<ContextPicker label="Context" onChange={() => {}} />);

    await waitFor(() => {
      expect(ApiService.getContexts).toHaveBeenCalled();
    });

    // Should not show error
    expect(screen.queryByText(/Unable to load contexts/i)).not.toBeInTheDocument();
  });

  it('should show error state and retry button on fetch failure', async () => {
    (ApiService.getContexts as jest.Mock).mockRejectedValue(new Error('Network error'));

    render(<ContextPicker label="Context" onChange={() => {}} />);

    await waitFor(() => {
      expect(screen.getByText(/Unable to load contexts/i)).toBeInTheDocument();
    });

    const retryButton = screen.getByText('Retry');
    expect(retryButton).toBeInTheDocument();

    // Click retry
    (ApiService.getContexts as jest.Mock).mockResolvedValue({
      data: mockContexts,
      fetched_at: new Date().toISOString(),
    });
    fireEvent.click(retryButton);

    await waitFor(() => {
      expect(ApiService.getContexts).toHaveBeenCalledTimes(2);
    });
  });

  it('should block invalid manual ID entry and show error', async () => {
    (ApiService.getContexts as jest.Mock).mockResolvedValue({
      data: mockContexts,
      fetched_at: new Date().toISOString(),
    });

    const onChange = jest.fn();
    render(<ContextPicker label="Context" onChange={onChange} />);

    await waitFor(() => {
      expect(ApiService.getContexts).toHaveBeenCalled();
    });

    const input = screen.getByPlaceholderText(/Select or enter context ID/i);
    
    // Enter invalid ID
    fireEvent.change(input, { target: { value: '999' } });

    await waitFor(() => {
      expect(screen.getByText(/Unknown context ID/i)).toBeInTheDocument();
    });

    // onChange should not be called with invalid ID
    expect(onChange).not.toHaveBeenCalledWith(999);
  });

  it('should allow valid manual ID entry', async () => {
    (ApiService.getContexts as jest.Mock).mockResolvedValue({
      data: mockContexts,
      fetched_at: new Date().toISOString(),
    });

    const onChange = jest.fn();
    render(<ContextPicker label="Context" onChange={onChange} />);

    await waitFor(() => {
      expect(ApiService.getContexts).toHaveBeenCalled();
    });

    const input = screen.getByPlaceholderText(/Select or enter context ID/i);
    
    // Enter valid ID
    fireEvent.change(input, { target: { value: '1' } });

    await waitFor(() => {
      expect(onChange).toHaveBeenCalledWith(1);
    });

    // Should not show error
    expect(screen.queryByText(/Unknown context ID/i)).not.toBeInTheDocument();
  });

  it('should show stale cache warning when data is >24h old', async () => {
    const oldDate = new Date();
    oldDate.setHours(oldDate.getHours() - 25);
    
    (ApiService.getContexts as jest.Mock).mockResolvedValue({
      data: mockContexts,
      fetched_at: oldDate.toISOString(),
    });

    render(<ContextPicker label="Context" onChange={() => {}} />);

    await waitFor(() => {
      expect(screen.getByText(/Data is stale/i)).toBeInTheDocument();
    });
  });

  it('should filter contexts by search query', async () => {
    (ApiService.getContexts as jest.Mock).mockResolvedValue({
      data: mockContexts,
      fetched_at: new Date().toISOString(),
    });

    render(<ContextPicker label="Context" onChange={() => {}} />);

    await waitFor(() => {
      expect(ApiService.getContexts).toHaveBeenCalled();
    });

    const input = screen.getByPlaceholderText(/Select or enter context ID/i);
    fireEvent.focus(input);
    fireEvent.change(input, { target: { value: 'Work' } });

    await waitFor(() => {
      expect(screen.getByText('Work Context')).toBeInTheDocument();
      expect(screen.queryByText('Personal Context')).not.toBeInTheDocument();
    });
  });

  it('should show empty state when no contexts available', async () => {
    (ApiService.getContexts as jest.Mock).mockResolvedValue({
      data: [],
      fetched_at: new Date().toISOString(),
    });

    render(<ContextPicker label="Context" onChange={() => {}} />);

    await waitFor(() => {
      expect(screen.getByText(/No contexts available/i)).toBeInTheDocument();
    });
  });

  it('should emit telemetry events for load success', async () => {
    const consoleLogSpy = jest.spyOn(console, 'log');
    
    (ApiService.getContexts as jest.Mock).mockResolvedValue({
      data: mockContexts,
      fetched_at: new Date().toISOString(),
    });

    render(<ContextPicker label="Context" onChange={() => {}} />);

    await waitFor(() => {
      expect(consoleLogSpy).toHaveBeenCalledWith(
        'context_picker.load.success',
        expect.objectContaining({
          count: 3,
        })
      );
    });
  });

  it('should emit telemetry events for validation failure', async () => {
    const consoleWarnSpy = jest.spyOn(console, 'warn');
    
    (ApiService.getContexts as jest.Mock).mockResolvedValue({
      data: mockContexts,
      fetched_at: new Date().toISOString(),
    });

    render(<ContextPicker label="Context" onChange={() => {}} />);

    await waitFor(() => {
      expect(ApiService.getContexts).toHaveBeenCalled();
    });

    const input = screen.getByPlaceholderText(/Select or enter context ID/i);
    fireEvent.change(input, { target: { value: '999' } });

    await waitFor(() => {
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        'context_picker.validation.failure',
        expect.objectContaining({
          invalid_id: 999,
        })
      );
    });
  });
});

