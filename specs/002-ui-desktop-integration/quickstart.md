# Quickstart: UI Desktop Integration (Tauri)

**Feature**: UI Desktop Integration (Tauri)  
**Date**: 2025-01-18  
**Phase**: 1 - Design & Contracts

## Overview

This quickstart guide provides step-by-step instructions for setting up and testing the UI Desktop Integration feature. The guide covers development environment setup, API integration testing, and validation of core functionality.

## Prerequisites

- Node.js 18+ and npm/yarn
- Rust 1.75+ and Cargo
- Tauri CLI (`npm install -g @tauri-apps/cli`)
- Truth Core system running locally on port 8080

## Development Setup

### 1. Initialize Tauri Project

```bash
# Create new Tauri project
npm create tauri-app@latest truth-ui
cd truth-ui

# Install dependencies
npm install
```

### 2. Configure Tauri

Update `src-tauri/tauri.conf.json`:

```json
{
  "build": {
    "beforeDevCommand": "npm run dev",
    "beforeBuildCommand": "npm run build",
    "devPath": "http://localhost:1420",
    "distDir": "../dist"
  },
  "package": {
    "productName": "Truth Training",
    "version": "0.1.0"
  },
  "tauri": {
    "allowlist": {
      "all": false,
      "shell": {
        "all": false,
        "open": true
      },
      "http": {
        "all": true,
        "request": true
      }
    }
  }
}
```

### 3. Install Frontend Dependencies

```bash
# Core dependencies
npm install react react-dom typescript
npm install @types/react @types/react-dom

# State management
npm install zustand

# HTTP client
npm install axios

# UI components
npm install @headlessui/react @heroicons/react

# Testing
npm install --save-dev jest @testing-library/react @testing-library/jest-dom
npm install --save-dev playwright @playwright/test
```

## API Integration Testing

### 1. Test Core System Connection

```bash
# Start Truth Core system
cd /path/to/truth-training
cargo run --features desktop --bin truth_core

# Test API connectivity
curl -X GET http://localhost:8080/api/v1/sync/status
```

Expected response:
```json
{
  "isOnline": true,
  "pendingOperations": 0,
  "coreSystemStatus": "healthy",
  "syncErrors": []
}
```

### 2. Test Event Creation

```bash
# Create a test event
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Test event for UI integration",
    "context": "This is a test event created during UI development",
    "signature": "test_signature",
    "publicKey": "test_public_key"
  }'
```

Expected response:
```json
{
  "id": "uuid-here",
  "description": "Test event for UI integration",
  "context": "This is a test event created during UI development",
  "createdAt": "2025-01-18T10:00:00Z",
  "updatedAt": "2025-01-18T10:00:00Z",
  "consensusStatus": "pending",
  "participantCount": 0
}
```

### 3. Test Judgment Submission

```bash
# Submit a judgment
curl -X POST http://localhost:8080/api/v1/judgments \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "event-uuid-from-previous-step",
    "assessment": "true",
    "confidenceLevel": 0.8,
    "reasoning": "This seems accurate based on available information",
    "signature": "test_signature",
    "publicKey": "test_public_key"
  }'
```

Expected response:
```json
{
  "id": "judgment-uuid-here",
  "eventId": "event-uuid-from-previous-step",
  "assessment": "true",
  "confidenceLevel": 0.8,
  "reasoning": "This seems accurate based on available information",
  "submittedAt": "2025-01-18T10:05:00Z"
}
```

## Frontend Development Testing

### 1. Create Basic API Service

Create `src/services/api.ts`:

```typescript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 5000,
});

export const eventsApi = {
  list: (page = 1, limit = 20) => 
    apiClient.get(`/events?page=${page}&limit=${limit}`),
  
  create: (event: CreateEventRequest) => 
    apiClient.post('/events', event),
  
  get: (eventId: string) => 
    apiClient.get(`/events/${eventId}`),
};

export const judgmentsApi = {
  list: (eventId?: string, page = 1, limit = 20) => {
    const params = new URLSearchParams({ page: page.toString(), limit: limit.toString() });
    if (eventId) params.append('eventId', eventId);
    return apiClient.get(`/judgments?${params}`);
  },
  
  submit: (judgment: SubmitJudgmentRequest) => 
    apiClient.post('/judgments', judgment),
};

export const syncApi = {
  getStatus: () => 
    apiClient.get('/sync/status'),
};
```

### 2. Test API Service

Create `src/services/__tests__/api.test.ts`:

```typescript
import { eventsApi, judgmentsApi, syncApi } from '../api';

describe('API Services', () => {
  test('should fetch sync status', async () => {
    const response = await syncApi.getStatus();
    expect(response.status).toBe(200);
    expect(response.data).toHaveProperty('isOnline');
    expect(response.data).toHaveProperty('coreSystemStatus');
  });

  test('should list events', async () => {
    const response = await eventsApi.list();
    expect(response.status).toBe(200);
    expect(response.data).toHaveProperty('events');
    expect(response.data).toHaveProperty('pagination');
  });

  test('should create event', async () => {
    const event = {
      description: 'Test event',
      signature: 'test_signature',
      publicKey: 'test_public_key',
    };
    
    const response = await eventsApi.create(event);
    expect(response.status).toBe(201);
    expect(response.data).toHaveProperty('id');
    expect(response.data.description).toBe('Test event');
  });
});
```

Run tests:
```bash
npm test
```

### 3. Test UI Components

Create `src/components/EventList.tsx`:

```typescript
import React, { useEffect, useState } from 'react';
import { eventsApi } from '../services/api';

interface Event {
  id: string;
  description: string;
  consensusStatus: string;
  participantCount: number;
}

export const EventList: React.FC = () => {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchEvents = async () => {
      try {
        const response = await eventsApi.list();
        setEvents(response.data.events);
      } catch (err) {
        setError('Failed to fetch events');
      } finally {
        setLoading(false);
      }
    };

    fetchEvents();
  }, []);

  if (loading) return <div>Loading events...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h2>Events</h2>
      {events.map(event => (
        <div key={event.id} className="event-item">
          <p>{event.description}</p>
          <span>Status: {event.consensusStatus}</span>
          <span>Participants: {event.participantCount}</span>
        </div>
      ))}
    </div>
  );
};
```

## End-to-End Testing

### 1. Install Playwright

```bash
npm install --save-dev @playwright/test
npx playwright install
```

### 2. Create E2E Test

Create `tests/e2e/event-creation.test.ts`:

```typescript
import { test, expect } from '@playwright/test';

test.describe('Event Creation Flow', () => {
  test('should create and display new event', async ({ page }) => {
    // Navigate to app
    await page.goto('http://localhost:1420');
    
    // Click create event button
    await page.click('[data-testid="create-event-button"]');
    
    // Fill event form
    await page.fill('[data-testid="event-description"]', 'E2E Test Event');
    await page.fill('[data-testid="event-context"]', 'Created during E2E testing');
    
    // Submit form
    await page.click('[data-testid="submit-event"]');
    
    // Verify event appears in list
    await expect(page.locator('[data-testid="event-list"]')).toContainText('E2E Test Event');
    
    // Verify consensus status
    await expect(page.locator('[data-testid="consensus-status"]')).toContainText('pending');
  });
});
```

### 3. Run E2E Tests

```bash
# Start Tauri app in dev mode
npm run tauri dev &

# Run E2E tests
npx playwright test
```

## Performance Testing

### 1. Response Time Validation

Create `tests/performance/api-performance.test.ts`:

```typescript
import { eventsApi, syncApi } from '../services/api';

describe('API Performance', () => {
  test('sync status should respond within 200ms', async () => {
    const start = Date.now();
    await syncApi.getStatus();
    const duration = Date.now() - start;
    
    expect(duration).toBeLessThan(200);
  });

  test('event list should respond within 200ms', async () => {
    const start = Date.now();
    await eventsApi.list();
    const duration = Date.now() - start;
    
    expect(duration).toBeLessThan(200);
  });
});
```

### 2. Memory Usage Testing

```bash
# Monitor memory usage during development
npm run tauri dev -- --profile
```

## Offline Testing

### 1. Test Offline Queue

```typescript
// Disconnect from network
// Create event
// Verify it's queued locally
// Reconnect network
// Verify sync occurs
```

### 2. Test Error Handling

```typescript
// Test network errors
// Test core system errors
// Test validation errors
// Verify error messages are user-friendly
```

## Validation Checklist

- [ ] Core system API is accessible
- [ ] Event creation works end-to-end
- [ ] Judgment submission works end-to-end
- [ ] Sync status displays correctly
- [ ] Offline operations queue properly
- [ ] Error handling works gracefully
- [ ] Performance targets met (<200ms response)
- [ ] E2E tests pass
- [ ] Memory usage within limits (<100MB)

## Troubleshooting

### Common Issues

1. **API Connection Failed**
   - Verify Truth Core system is running
   - Check port 8080 is accessible
   - Verify CORS settings

2. **Tauri Build Errors**
   - Update Rust to 1.75+
   - Clear Cargo cache: `cargo clean`
   - Reinstall Tauri CLI

3. **Frontend Build Errors**
   - Clear node_modules: `rm -rf node_modules && npm install`
   - Check TypeScript configuration
   - Verify React version compatibility

### Debug Commands

```bash
# Check API connectivity
curl -v http://localhost:8080/api/v1/sync/status

# Check Tauri logs
npm run tauri dev -- --verbose

# Check frontend build
npm run build -- --verbose
```

## Final Commands

### Development Commands
```bash
# Start development server
npm run tauri:dev

# Run all tests
npm test
npx playwright test

# Build for production
npm run tauri:build

# Lint and format
npm run lint
npm run format
```

### Testing Commands
```bash
# Unit tests
npm test

# E2E tests
npx playwright test

# Performance tests
npm run test:performance

# Coverage report
npm run test:coverage
```

### Build Commands
```bash
# Development build
npm run build

# Production build
npm run tauri:build

# Check build
npm run tauri:build -- --debug
```

## Next Steps

After completing this quickstart:

1. ✅ Implement remaining UI components
2. ✅ Add comprehensive error handling
3. ✅ Implement offline synchronization
4. ✅ Add user preferences
5. ✅ Create comprehensive test suite
6. ✅ Performance optimization
7. Security hardening
8. Production deployment preparation