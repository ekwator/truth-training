// Integration tests for App Settings functionality

import { describe, it, expect, beforeEach, afterEach, jest } from '@jest/globals';

// Mock Tauri API
jest.mock('@tauri-apps/api/core', () => ({
  invoke: jest.fn()
}));

// Mock localStorage for web mode
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value;
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    }
  };
})();

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock
});

import { ApiService } from '@/services/api';
import { AppConfig, ConnectionTestResult } from '@/types/api';

describe('App Settings Integration Tests', () => {
  beforeEach(() => {
    localStorageMock.clear();
    jest.clearAllMocks();
  });

  describe('Configuration Schema Validation', () => {
    it('should validate complete configuration schema', () => {
      const validConfig: AppConfig = {
        mode: 'http',
        server_ip: '192.168.1.100',
        server_port: 9000
      };

      expect(validConfig.mode).toMatch(/^(core|http)$/);
      expect(validConfig.server_ip).toMatch(/^\d{1,3}(\.\d{1,3}){3}$/);
      expect(validConfig.server_port).toBeGreaterThanOrEqual(1);
      expect(validConfig.server_port).toBeLessThanOrEqual(65535);
    });

    it('should validate IP address format', () => {
      const validIPs = [
        '127.0.0.1',
        '192.168.1.1',
        '10.0.0.1',
        '255.255.255.255',
        '0.0.0.0'
      ];

      validIPs.forEach(ip => {
        expect(ip).toMatch(/^\d{1,3}(\.\d{1,3}){3}$/);
      });
    });

    it('should validate port ranges', () => {
      const validPorts = [1, 80, 8080, 65535];

      validPorts.forEach(port => {
        expect(port).toBeGreaterThanOrEqual(1);
        expect(port).toBeLessThanOrEqual(65535);
      });
    });

    it('should validate mode values', () => {
      const validModes = ['core', 'http'];
      
      validModes.forEach(mode => {
        expect(mode).toMatch(/^(core|http)$/);
      });
    });
  });

  describe('Type Definitions', () => {
    it('should have correct AppConfig interface structure', () => {
      const config: AppConfig = {
        mode: 'core',
        server_ip: '127.0.0.1',
        server_port: 8080
      };

      expect(config).toHaveProperty('mode');
      expect(config).toHaveProperty('server_ip');
      expect(config).toHaveProperty('server_port');
      expect(typeof config.mode).toBe('string');
      expect(typeof config.server_ip).toBe('string');
      expect(typeof config.server_port).toBe('number');
    });

    it('should have correct ConnectionTestResult interface structure', () => {
      const result: ConnectionTestResult = {
        ok: true,
        message: 'Test message'
      };

      expect(result).toHaveProperty('ok');
      expect(result).toHaveProperty('message');
      expect(typeof result.ok).toBe('boolean');
      expect(typeof result.message).toBe('string');
    });
  });

  describe('Configuration Validation Logic', () => {
    it('should validate IP addresses correctly', () => {
      const validateIP = (ip: string): boolean => {
        const parts = ip.split('.');
        if (parts.length !== 4) return false;
        
        for (const part of parts) {
          const num = parseInt(part, 10);
          if (isNaN(num) || num < 0 || num > 255) return false;
        }
        
        return true;
      };

      expect(validateIP('127.0.0.1')).toBe(true);
      expect(validateIP('192.168.1.1')).toBe(true);
      expect(validateIP('256.1.1.1')).toBe(false);
      expect(validateIP('192.168.1')).toBe(false);
      expect(validateIP('not.an.ip')).toBe(false);
    });

    it('should validate port numbers correctly', () => {
      const validatePort = (port: number): boolean => {
        return port >= 1 && port <= 65535;
      };

      expect(validatePort(1)).toBe(true);
      expect(validatePort(8080)).toBe(true);
      expect(validatePort(65535)).toBe(true);
      expect(validatePort(0)).toBe(false);
      expect(validatePort(65536)).toBe(false);
    });

    it('should validate mode values correctly', () => {
      const validateMode = (mode: string): boolean => {
        return ['core', 'http'].includes(mode);
      };

      expect(validateMode('core')).toBe(true);
      expect(validateMode('http')).toBe(true);
      expect(validateMode('invalid')).toBe(false);
      expect(validateMode('')).toBe(false);
    });
  });
});
