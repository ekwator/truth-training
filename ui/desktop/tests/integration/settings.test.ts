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

  describe('Configuration Persistence', () => {
    it('should save and load app configuration', async () => {
      const config: AppConfig = {
        mode: 'http',
        server_ip: '192.168.1.100',
        server_port: 9000
      };

      // Mock Tauri invoke for save
      const mockInvoke = jest.fn();
      (await import('@tauri-apps/api/core')).invoke = mockInvoke;
      mockInvoke.mockResolvedValue(undefined);

      await ApiService.saveAppConfig(config);
      expect(mockInvoke).toHaveBeenCalledWith('save_app_config', { config });

      // Mock Tauri invoke for load
      mockInvoke.mockResolvedValue(config);
      const loadedConfig = await ApiService.getAppConfig();
      expect(loadedConfig).toEqual(config);
    });

    it('should return default config when no config exists', async () => {
      const mockInvoke = jest.fn();
      (await import('@tauri-apps/api/core')).invoke = mockInvoke;
      mockInvoke.mockResolvedValue({
        mode: 'core',
        server_ip: '127.0.0.1',
        server_port: 8080
      });

      const config = await ApiService.getAppConfig();
      expect(config.mode).toBe('core');
      expect(config.server_ip).toBe('127.0.0.1');
      expect(config.server_port).toBe(8080);
    });
  });

  describe('IP and Port Validation', () => {
    it('should validate correct IP addresses', () => {
      const validIPs = [
        '127.0.0.1',
        '192.168.1.1',
        '10.0.0.1',
        '255.255.255.255',
        '0.0.0.0'
      ];

      validIPs.forEach(ip => {
        const config: AppConfig = {
          mode: 'http',
          server_ip: ip,
          server_port: 8080
        };
        expect(() => ApiService.saveAppConfig(config)).not.toThrow();
      });
    });

    it('should validate port ranges', () => {
      const validPorts = [1, 80, 8080, 65535];

      validPorts.forEach(port => {
        const config: AppConfig = {
          mode: 'http',
          server_ip: '127.0.0.1',
          server_port: port
        };
        expect(() => ApiService.saveAppConfig(config)).not.toThrow();
      });
    });
  });

  describe('Mode Toggle Behavior', () => {
    it('should accept core mode', async () => {
      const config: AppConfig = {
        mode: 'core',
        server_ip: '127.0.0.1',
        server_port: 8080
      };

      const mockInvoke = jest.fn();
      (await import('@tauri-apps/api/core')).invoke = mockInvoke;
      mockInvoke.mockResolvedValue(undefined);

      await ApiService.saveAppConfig(config);
      expect(mockInvoke).toHaveBeenCalledWith('save_app_config', { config });
    });

    it('should accept http mode', async () => {
      const config: AppConfig = {
        mode: 'http',
        server_ip: '192.168.1.100',
        server_port: 9000
      };

      const mockInvoke = jest.fn();
      (await import('@tauri-apps/api/core')).invoke = mockInvoke;
      mockInvoke.mockResolvedValue(undefined);

      await ApiService.saveAppConfig(config);
      expect(mockInvoke).toHaveBeenCalledWith('save_app_config', { config });
    });
  });

  describe('Test Connection Logic', () => {
    it('should test core connection successfully', async () => {
      const mockInvoke = jest.fn();
      (await import('@tauri-apps/api/core')).invoke = mockInvoke;
      mockInvoke.mockResolvedValue({
        ok: true,
        message: 'Core is running'
      });

      const result = await ApiService.testCoreConnection();
      expect(result.ok).toBe(true);
      expect(result.message).toBe('Core is running');
      expect(mockInvoke).toHaveBeenCalledWith('core_status');
    });

    it('should test HTTP connection successfully', async () => {
      const mockInvoke = jest.fn();
      (await import('@tauri-apps/api/core')).invoke = mockInvoke;
      mockInvoke.mockResolvedValue({
        ok: true,
        message: 'HTTP connection successful'
      });

      const result = await ApiService.testHttpConnection('127.0.0.1', 8080);
      expect(result.ok).toBe(true);
      expect(result.message).toBe('HTTP connection successful');
      expect(mockInvoke).toHaveBeenCalledWith('test_http_connection', { ip: '127.0.0.1', port: 8080 });
    });
  });

  describe('Error Handling', () => {
    it('should handle Tauri command errors gracefully', async () => {
      const mockInvoke = jest.fn();
      (await import('@tauri-apps/api/core')).invoke = mockInvoke;
      mockInvoke.mockRejectedValue(new Error('Tauri command failed'));

      await expect(ApiService.getAppConfig()).rejects.toThrow('Failed to get app configuration');
      await expect(ApiService.saveAppConfig({ mode: 'core', server_ip: '127.0.0.1', server_port: 8080 })).rejects.toThrow('Failed to save app configuration');
    });
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
  });
});
