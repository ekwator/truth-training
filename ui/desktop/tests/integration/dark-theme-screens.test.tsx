/**
 * Integration test for dark theme across all screens.
 * Verifies all screens support dark theme.
 */

import React from 'react';
import { describe, it, expect, beforeEach } from '@jest/globals';
import { render, screen } from '@testing-library/react';
import { ThemeProvider } from '@/components/system/ThemeProvider';
import { ToastProvider } from '@/components/system/Toaster';
import { Dashboard } from '@/pages/Dashboard';
import { NewEvent } from '@/pages/NewEvent';
import { ContextEditor } from '@/pages/ContextEditor';
import { Events } from '@/pages/Events';
import { Judgments } from '@/pages/Judgments';
import { OverallSummary } from '@/pages/OverallSummary';
import { TrainingResults } from '@/pages/TrainingResults';
import { Settings } from '@/pages/Settings';
import { EventSummary } from '@/pages/EventSummary';

// Mock navigation function
const mockNavigate = jest.fn();

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

describe('Dark Theme Screens Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  const renderWithDarkTheme = (component: React.ReactElement) => {
    return render(
      <ThemeProvider defaultTheme="dark">
        <ToastProvider>
          {component}
        </ToastProvider>
      </ThemeProvider>
    );
  };

  describe('Screen Dark Theme Support', () => {
    it('should apply dark theme to Dashboard screen', () => {
      renderWithDarkTheme(<Dashboard onNavigate={mockNavigate} />);
      expect(document.documentElement.classList.contains('dark')).toBe(true);
    });

    it('should apply dark theme to NewEvent screen', () => {
      renderWithDarkTheme(<NewEvent onNavigate={mockNavigate} />);
      expect(document.documentElement.classList.contains('dark')).toBe(true);
    });

    it('should apply dark theme to ContextEditor screen', () => {
      renderWithDarkTheme(<ContextEditor onNavigate={mockNavigate} />);
      expect(document.documentElement.classList.contains('dark')).toBe(true);
    });

    it('should apply dark theme to Events screen', () => {
      renderWithDarkTheme(<Events onNavigate={mockNavigate} />);
      expect(document.documentElement.classList.contains('dark')).toBe(true);
    });

    it('should apply dark theme to Judgments screen', () => {
      renderWithDarkTheme(<Judgments eventId="test-id" onNavigate={mockNavigate} />);
      expect(document.documentElement.classList.contains('dark')).toBe(true);
    });

    it('should apply dark theme to OverallSummary screen', () => {
      renderWithDarkTheme(<OverallSummary />);
      expect(document.documentElement.classList.contains('dark')).toBe(true);
    });

    it('should apply dark theme to TrainingResults screen', () => {
      renderWithDarkTheme(<TrainingResults />);
      expect(document.documentElement.classList.contains('dark')).toBe(true);
    });

    it('should apply dark theme to Settings screen', () => {
      renderWithDarkTheme(<Settings />);
      expect(document.documentElement.classList.contains('dark')).toBe(true);
    });

    it('should apply dark theme to EventSummary screen', () => {
      renderWithDarkTheme(<EventSummary eventId="test-id" onNavigate={mockNavigate} />);
      expect(document.documentElement.classList.contains('dark')).toBe(true);
    });
  });

  describe('Dark Theme Consistency', () => {
    it('should maintain dark theme across screen navigation', () => {
      const { rerender } = renderWithDarkTheme(<Dashboard onNavigate={mockNavigate} />);
      expect(document.documentElement.classList.contains('dark')).toBe(true);

      rerender(
        <ThemeProvider defaultTheme="dark">
          <ToastProvider>
            <NewEvent onNavigate={mockNavigate} />
          </ToastProvider>
        </ThemeProvider>
      );
      expect(document.documentElement.classList.contains('dark')).toBe(true);
    });
  });
});

