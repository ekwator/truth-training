import '@testing-library/jest-dom';
import { jest } from '@jest/globals';

// Mock Tauri API
Object.defineProperty(window, '__TAURI__', {
  value: {
    invoke: jest.fn(),
    tauri: {
      invoke: jest.fn(),
    },
  },
});

// Mock environment variables
process.env.VITE_API_BASE = 'http://localhost:8080/api/v1';
process.env.VITE_APP_NAME = 'Truth Training';
process.env.VITE_APP_VERSION = '0.1.0';

// Mock console methods to reduce noise in tests
global.console = {
  ...console,
  log: jest.fn(),
  debug: jest.fn(),
  info: jest.fn(),
  warn: jest.fn(),
  error: jest.fn(),
};

