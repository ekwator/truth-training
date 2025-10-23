import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from '@jest/globals';
import { SyncStatus } from '../system/SyncStatus';

describe('SyncStatus', () => {
  it('should render online status correctly', () => {
    render(
      <SyncStatus
        isOnline={true}
        pendingOperations={0}
        lastSync="2024-01-01T12:00:00Z"
      />
    );

    expect(screen.getByText('Synced')).toBeInTheDocument();
    // Check that time is displayed (format may vary by locale)
    expect(screen.getByText(/17:00:00|12:00:00/)).toBeInTheDocument();
  });

  it('should render offline status correctly', () => {
    render(
      <SyncStatus
        isOnline={false}
        pendingOperations={3}
        lastSync={null}
      />
    );

    expect(screen.getByText('Offline')).toBeInTheDocument();
  });

  it('should render pending operations status correctly', () => {
    render(
      <SyncStatus
        isOnline={true}
        pendingOperations={5}
        lastSync="2024-01-01T12:00:00Z"
      />
    );

    expect(screen.getByText('5 pending')).toBeInTheDocument();
  });

  it('should apply correct CSS classes for online status', () => {
    const { container } = render(
      <SyncStatus
        isOnline={true}
        pendingOperations={0}
        lastSync="2024-01-01T12:00:00Z"
      />
    );

    const statusElement = container.firstChild as HTMLElement;
    expect(statusElement).toHaveClass('text-green-600', 'bg-green-100');
  });

  it('should apply correct CSS classes for offline status', () => {
    const { container } = render(
      <SyncStatus
        isOnline={false}
        pendingOperations={0}
        lastSync={null}
      />
    );

    const statusElement = container.firstChild as HTMLElement;
    expect(statusElement).toHaveClass('text-red-600', 'bg-red-100');
  });

  it('should apply correct CSS classes for pending status', () => {
    const { container } = render(
      <SyncStatus
        isOnline={true}
        pendingOperations={3}
        lastSync="2024-01-01T12:00:00Z"
      />
    );

    const statusElement = container.firstChild as HTMLElement;
    expect(statusElement).toHaveClass('text-yellow-600', 'bg-yellow-100');
  });

  it('should display last sync time when available', () => {
    render(
      <SyncStatus
        isOnline={true}
        pendingOperations={0}
        lastSync="2024-01-01T12:00:00Z"
      />
    );

    // Check that time is displayed (format may vary by locale)
    expect(screen.getByText(/17:00:00|12:00:00/)).toBeInTheDocument();
  });

  it('should not display last sync time when null', () => {
    render(
      <SyncStatus
        isOnline={false}
        pendingOperations={0}
        lastSync={null}
      />
    );

    expect(screen.queryByText(/12:00:00 PM/)).not.toBeInTheDocument();
  });
});
