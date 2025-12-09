/**
 * Contract test for dark theme system.
 * Verifies theme provider defaults to dark theme.
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { renderHook, act } from '@testing-library/react';
import { ThemeProvider, useTheme } from '@/components/system/ThemeProvider';

describe('Dark Theme System Contract Tests', () => {
  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    // Clean up localStorage after each test
    localStorage.clear();
  });

  describe('ThemeProvider Default Theme', () => {
    it('should default to dark theme when no theme is stored', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ({ children }) => (
          <ThemeProvider defaultTheme="dark">{children}</ThemeProvider>
        ),
      });

      expect(result.current.theme).toBe('dark');
      expect(result.current.resolvedTheme).toBe('dark');
    });

    it('should use dark theme as default when defaultTheme is dark', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ({ children }) => (
          <ThemeProvider defaultTheme="dark">{children}</ThemeProvider>
        ),
      });

      expect(result.current.theme).toBe('dark');
    });

    it('should apply dark theme class to document root', () => {
      renderHook(() => useTheme(), {
        wrapper: ({ children }) => (
          <ThemeProvider defaultTheme="dark">{children}</ThemeProvider>
        ),
      });

      expect(document.documentElement.classList.contains('dark')).toBe(true);
      expect(document.documentElement.classList.contains('light')).toBe(false);
    });
  });

  describe('Theme Persistence', () => {
    it('should persist dark theme preference to localStorage', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ({ children }) => (
          <ThemeProvider defaultTheme="dark">{children}</ThemeProvider>
        ),
      });

      act(() => {
        result.current.setTheme('dark');
      });

      expect(localStorage.getItem('truth-theme')).toBe('dark');
    });

    it('should load dark theme from localStorage if available', () => {
      localStorage.setItem('truth-theme', 'dark');

      const { result } = renderHook(() => useTheme(), {
        wrapper: ({ children }) => (
          <ThemeProvider defaultTheme="system">{children}</ThemeProvider>
        ),
      });

      expect(result.current.theme).toBe('dark');
      expect(result.current.resolvedTheme).toBe('dark');
    });
  });

  describe('Theme Resolution', () => {
    it('should resolve dark theme correctly', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ({ children }) => (
          <ThemeProvider defaultTheme="dark">{children}</ThemeProvider>
        ),
      });

      expect(result.current.resolvedTheme).toBe('dark');
    });

    it('should update resolvedTheme when theme changes to dark', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ({ children }) => (
          <ThemeProvider defaultTheme="light">{children}</ThemeProvider>
        ),
      });

      expect(result.current.resolvedTheme).toBe('light');

      act(() => {
        result.current.setTheme('dark');
      });

      expect(result.current.resolvedTheme).toBe('dark');
    });
  });
});

