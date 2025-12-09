/**
 * Integration test for NodesPanel component functionality.
 * Verifies Desktop-specific NodesPanel component is preserved and functional.
 */

import React from 'react';
import { describe, it, expect, beforeEach, jest } from '@jest/globals';
import { render, screen, waitFor } from '@testing-library/react';
import { NodesPanel } from '@/components/NodesPanel';
import { ThemeProvider } from '@/components/system/ThemeProvider';
import { ToastProvider } from '@/components/system/Toaster';
import ApiService from '@/services/api';

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

// Mock ApiService
jest.mock('@/services/api', () => ({
  listNodes: jest.fn(),
  manualDiscover: jest.fn(),
  cleanupNodes: jest.fn(),
  runNodesHealthCheck: jest.fn(),
}));

describe('NodesPanel Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderNodesPanel = () => {
    return render(
      <ThemeProvider>
        <ToastProvider>
          <NodesPanel />
        </ToastProvider>
      </ThemeProvider>
    );
  };

  describe('NodesPanel Component Preservation', () => {
    it('should render NodesPanel component', () => {
      (ApiService.listNodes as jest.Mock).mockResolvedValue([]);
      
      renderNodesPanel();
      
      // Verify NodesPanel title is displayed
      expect(screen.getByText(/Nodes/i)).toBeInTheDocument();
    });

    it('should display loading state when fetching nodes', async () => {
      (ApiService.listNodes as jest.Mock).mockImplementation(() => 
        new Promise(resolve => setTimeout(() => resolve([]), 100))
      );
      
      renderNodesPanel();
      
      // Verify loading state is shown
      expect(screen.getByText(/Loading/i)).toBeInTheDocument();
    });

    it('should display nodes list when nodes are loaded', async () => {
      const mockNodes = [
        {
          id: 'node1',
          address: '192.168.1.1',
          node_type: 'LAN',
          reachable: true,
          ttl: 3600,
          expires_in: 1800,
          source: 'discovery',
          last_seen: Math.floor(Date.now() / 1000),
        },
      ];
      
      (ApiService.listNodes as jest.Mock).mockResolvedValue(mockNodes);
      
      renderNodesPanel();
      
      await waitFor(() => {
        expect(screen.getByText('192.168.1.1')).toBeInTheDocument();
      });
    });

    it('should display empty state when no nodes are found', async () => {
      (ApiService.listNodes as jest.Mock).mockResolvedValue([]);
      
      renderNodesPanel();
      
      await waitFor(() => {
        expect(screen.getByText(/No nodes/i)).toBeInTheDocument();
      });
    });
  });

  describe('NodesPanel Actions', () => {
    it('should have refresh button', async () => {
      (ApiService.listNodes as jest.Mock).mockResolvedValue([]);
      
      renderNodesPanel();
      
      await waitFor(() => {
        expect(screen.getByText(/Refresh/i)).toBeInTheDocument();
      });
    });

    it('should have discover button', async () => {
      (ApiService.listNodes as jest.Mock).mockResolvedValue([]);
      
      renderNodesPanel();
      
      await waitFor(() => {
        expect(screen.getByText(/Discover/i)).toBeInTheDocument();
      });
    });

    it('should have cleanup button', async () => {
      (ApiService.listNodes as jest.Mock).mockResolvedValue([]);
      
      renderNodesPanel();
      
      await waitFor(() => {
        expect(screen.getByText(/Cleanup/i)).toBeInTheDocument();
      });
    });

    it('should have health check button', async () => {
      (ApiService.listNodes as jest.Mock).mockResolvedValue([]);
      
      renderNodesPanel();
      
      await waitFor(() => {
        expect(screen.getByText(/Health Check/i)).toBeInTheDocument();
      });
    });
  });

  describe('NodesPanel Filtering', () => {
    it('should have node type filter dropdown', async () => {
      (ApiService.listNodes as jest.Mock).mockResolvedValue([]);
      
      renderNodesPanel();
      
      await waitFor(() => {
        // Verify filter dropdown exists (check for ALL option)
        expect(screen.getByText(/ALL/i)).toBeInTheDocument();
      });
    });

    it('should have reachability filter dropdown', async () => {
      (ApiService.listNodes as jest.Mock).mockResolvedValue([]);
      
      renderNodesPanel();
      
      await waitFor(() => {
        // Verify reachability filter exists
        expect(screen.getByText(/All/i)).toBeInTheDocument();
      });
    });
  });
});

