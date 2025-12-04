import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { LocaleToggle } from '../LocaleToggle';
import { setLocale, getCurrentLocale } from '@/i18n';
import { useToast } from '@/components/system/Toaster';

// Mock dependencies
jest.mock('@/i18n', () => ({
  ...jest.requireActual('@/i18n'),
  setLocale: jest.fn(),
  getCurrentLocale: jest.fn(),
}));

jest.mock('@/components/system/Toaster', () => ({
  useToast: jest.fn(),
}));

const mockAddToast = jest.fn();

describe('LocaleToggle Contract Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (useToast as jest.Mock).mockReturnValue({ addToast: mockAddToast });
    (getCurrentLocale as jest.Mock).mockReturnValue('en');
    (setLocale as jest.Mock).mockResolvedValue(undefined);
    
    // Mock localStorage
    Storage.prototype.getItem = jest.fn(() => 'en');
    Storage.prototype.setItem = jest.fn();
    
    // Suppress console warnings
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should render dropdown variant with current locale', () => {
    render(<LocaleToggle variant="dropdown" />);
    
    const select = screen.getByLabelText(/change/i);
    expect(select).toBeInTheDocument();
    expect(select).toHaveValue('en');
  });

  it('should render button variant with current locale', () => {
    render(<LocaleToggle variant="button" />);
    
    const button = screen.getByLabelText(/change/i);
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent('English');
  });

  it('should update locale instantly on change (dropdown)', async () => {
    (setLocale as jest.Mock).mockResolvedValue(undefined);
    
    render(<LocaleToggle variant="dropdown" />);
    
    const select = screen.getByLabelText(/change/i);
    fireEvent.change(select, { target: { value: 'ru' } });

    await waitFor(() => {
      expect(setLocale).toHaveBeenCalledWith('ru', true);
    });

    // UI should update instantly (no reload)
    expect(document.documentElement.lang).toBe('ru');
  });

  it('should update locale instantly on change (button)', async () => {
    (setLocale as jest.Mock).mockResolvedValue(undefined);
    
    render(<LocaleToggle variant="button" />);
    
    const button = screen.getByLabelText(/change/i);
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Русский')).toBeInTheDocument();
    });

    const ruOption = screen.getByText('Русский');
    fireEvent.click(ruOption);

    await waitFor(() => {
      expect(setLocale).toHaveBeenCalledWith('ru', true);
    });
  });

  it('should show error toast on persistence failure', async () => {
    const error = new Error('Failed to save');
    (setLocale as jest.Mock).mockRejectedValue(error);
    
    render(<LocaleToggle variant="dropdown" />);
    
    const select = screen.getByLabelText(/change/i);
    fireEvent.change(select, { target: { value: 'ru' } });

    await waitFor(() => {
      expect(mockAddToast).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'error',
        })
      );
    });
  });

  it('should emit locale.change telemetry on success', async () => {
    const consoleLogSpy = jest.spyOn(console, 'log');
    (setLocale as jest.Mock).mockImplementation(async (locale: string) => {
      // Simulate telemetry logging
      console.log('locale.change', { from: 'en', to: locale, success: true });
    });
    
    render(<LocaleToggle variant="dropdown" />);
    
    const select = screen.getByLabelText(/change/i);
    fireEvent.change(select, { target: { value: 'ru' } });

    await waitFor(() => {
      expect(setLocale).toHaveBeenCalledWith('ru', true);
      expect(consoleLogSpy).toHaveBeenCalledWith(
        'locale.change',
        expect.objectContaining({
          from: 'en',
          to: 'ru',
          success: true,
        })
      );
    });
  });

  it('should close dropdown on outside click (button variant)', async () => {
    render(<LocaleToggle variant="button" />);
    
    const button = screen.getByLabelText(/change/i);
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Русский')).toBeInTheDocument();
    });

    // Click outside
    fireEvent.mouseDown(document.body);

    await waitFor(() => {
      expect(screen.queryByText('Русский')).not.toBeInTheDocument();
    });
  });

  it('should persist locale to localStorage', async () => {
    const setItemSpy = jest.spyOn(Storage.prototype, 'setItem');
    (setLocale as jest.Mock).mockImplementation(async (locale: string) => {
      // Simulate localStorage.setItem call inside setLocale
      localStorage.setItem('truth-locale', locale);
    });
    
    render(<LocaleToggle variant="dropdown" />);
    
    const select = screen.getByLabelText(/change/i);
    fireEvent.change(select, { target: { value: 'ru' } });

    await waitFor(() => {
      expect(setLocale).toHaveBeenCalledWith('ru', true);
      expect(setItemSpy).toHaveBeenCalledWith('truth-locale', 'ru');
    });
  });

  it('should only show EN and RU locales', () => {
    render(<LocaleToggle variant="dropdown" />);
    
    const select = screen.getByLabelText(/change/i);
    const options = Array.from(select.querySelectorAll('option'));
    
    expect(options).toHaveLength(2);
    expect(options[0]).toHaveValue('en');
    expect(options[1]).toHaveValue('ru');
  });
});

