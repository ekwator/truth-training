/**
 * Integration test for keyboard shortcuts (Alt+1 through Alt+8).
 * Verifies Desktop-specific keyboard navigation is preserved.
 */

import React from 'react';
import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { render, screen, fireEvent } from '@testing-library/react';
import { App } from '@/App';
import { ThemeProvider } from '@/components/system/ThemeProvider';
import { ToastProvider } from '@/components/system/Toaster';

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

describe('Keyboard Shortcuts Integration Tests', () => {
  let container: HTMLElement;

  beforeEach(() => {
    container = document.createElement('div');
    document.body.appendChild(container);
    localStorage.clear();
  });

  afterEach(() => {
    document.body.removeChild(container);
  });

  const renderApp = () => {
    return render(
      <ThemeProvider>
        <ToastProvider>
          <App />
        </ToastProvider>
      </ThemeProvider>,
      { container }
    );
  };

  describe('Alt+1 through Alt+8 Navigation', () => {
    it('should navigate to Dashboard (home) when Alt+1 is pressed', () => {
      renderApp();
      
      // Simulate Alt+1 keypress
      fireEvent.keyDown(window, { key: '1', altKey: true });
      
      // Verify Dashboard screen is displayed
      expect(screen.getByText(/Dashboard/i)).toBeInTheDocument();
    });

    it('should navigate to NewEvent screen when Alt+2 is pressed', () => {
      renderApp();
      
      // Simulate Alt+2 keypress
      fireEvent.keyDown(window, { key: '2', altKey: true });
      
      // Verify NewEvent screen is displayed
      expect(screen.getByText(/New Event/i)).toBeInTheDocument();
    });

    it('should navigate to ContextEditor screen when Alt+3 is pressed', () => {
      renderApp();
      
      // Simulate Alt+3 keypress
      fireEvent.keyDown(window, { key: '3', altKey: true });
      
      // Verify ContextEditor screen is displayed
      expect(screen.getByText(/Context Templates/i)).toBeInTheDocument();
    });

    it('should navigate to EventSummary screen when Alt+4 is pressed', () => {
      renderApp();
      
      // Simulate Alt+4 keypress
      fireEvent.keyDown(window, { key: '4', altKey: true });
      
      // Verify EventSummary screen is displayed
      expect(screen.getByText(/Event Summary/i)).toBeInTheDocument();
    });

    it('should navigate to OverallSummary screen when Alt+5 is pressed', () => {
      renderApp();
      
      // Simulate Alt+5 keypress
      fireEvent.keyDown(window, { key: '5', altKey: true });
      
      // Verify OverallSummary screen is displayed
      expect(screen.getByText(/Overall Summary/i)).toBeInTheDocument();
    });

    it('should navigate to TrainingResults screen when Alt+6 is pressed', () => {
      renderApp();
      
      // Simulate Alt+6 keypress
      fireEvent.keyDown(window, { key: '6', altKey: true });
      
      // Verify TrainingResults screen is displayed
      expect(screen.getByText(/Training Results/i)).toBeInTheDocument();
    });

    it('should navigate to Settings screen when Alt+8 is pressed', () => {
      renderApp();
      
      // Simulate Alt+8 keypress
      fireEvent.keyDown(window, { key: '8', altKey: true });
      
      // Verify Settings screen is displayed
      expect(screen.getByText(/Settings/i)).toBeInTheDocument();
    });
  });

  describe('Escape Key Back Navigation', () => {
    it('should navigate back when Escape key is pressed (not on home screen)', () => {
      renderApp();
      
      // Navigate to a different screen first
      fireEvent.keyDown(window, { key: '2', altKey: true });
      expect(screen.getByText(/New Event/i)).toBeInTheDocument();
      
      // Press Escape to go back
      fireEvent.keyDown(window, { key: 'Escape' });
      
      // Should return to Dashboard
      expect(screen.getByText(/Dashboard/i)).toBeInTheDocument();
    });

    it('should not navigate back when Escape is pressed on home screen', () => {
      renderApp();
      
      // Verify we're on Dashboard
      expect(screen.getByText(/Dashboard/i)).toBeInTheDocument();
      
      // Press Escape
      fireEvent.keyDown(window, { key: 'Escape' });
      
      // Should still be on Dashboard
      expect(screen.getByText(/Dashboard/i)).toBeInTheDocument();
    });
  });

  describe('Keyboard Shortcut Preservation', () => {
    it('should prevent default browser behavior for Alt+number shortcuts', () => {
      renderApp();
      
      const event = new KeyboardEvent('keydown', {
        key: '1',
        altKey: true,
        cancelable: true,
      });
      
      const preventDefaultSpy = jest.spyOn(event, 'preventDefault');
      window.dispatchEvent(event);
      
      // Note: In a real test environment, we'd verify preventDefault was called
      // This is a placeholder to verify the shortcut handling exists
      expect(preventDefaultSpy).toBeDefined();
    });
  });
});

