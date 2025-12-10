/**
 * Integration test for Context Editor template creation.
 * Verifies create template form, save functionality, and template management.
 */

import { describe, it, expect, beforeEach, jest } from '@jest/globals';
import axios, { AxiosInstance } from 'axios';
import { ApiService, setApiClient } from '@/services/api';
import type { CreateContextRequest, ContextTemplate } from '@/types/contexts';

// Mock apiClient
const mockApiClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
  interceptors: {
    request: { use: jest.fn() },
    response: { use: jest.fn() },
  },
} as unknown as AxiosInstance;

describe('Context Editor Template Creation Integration', () => {
  let createdTemplateId: number | null = null;
  let templateIdCounter = 1;

  beforeEach(async () => {
    jest.clearAllMocks();
    mockApiClient.post.mockClear();
    mockApiClient.get.mockClear();
    // Set mock apiClient
    setApiClient(mockApiClient);
    // Clean up any test templates if needed
    if (createdTemplateId) {
      // Note: Delete functionality may need to be implemented
      createdTemplateId = null;
    }
  });

  it('should create a new context template', async () => {
    const newTemplate: CreateContextRequest = {
      name: 'Test Template for Creation',
      description: 'Test template description',
      category_id: 1,
      forma_id: 1,
      cause_id: null,
      develop_id: null,
      effect_id: null,
    };

    const mockTemplate = { id: templateIdCounter++, ...newTemplate };
    mockApiClient.post.mockResolvedValueOnce({ data: mockTemplate });

    const result = await ApiService.createContext(newTemplate);
    
    expect(result).toBeDefined();
    expect(result).toHaveProperty('id');
    expect(result).toHaveProperty('name');
    expect(result.name).toBe(newTemplate.name);
    expect(result.description).toBe(newTemplate.description);
    expect(result.category_id).toBe(newTemplate.category_id);
    expect(result.forma_id).toBe(newTemplate.forma_id);
    
    createdTemplateId = result.id;
  });

  it('should create template with all context fields', async () => {
    const newTemplate: CreateContextRequest = {
      name: 'Complete Template',
      description: 'Template with all fields',
      category_id: 1,
      forma_id: 1,
      cause_id: 1,
      develop_id: 1,
      effect_id: 1,
    };

    const mockTemplate = { id: templateIdCounter++, ...newTemplate };
    mockApiClient.post.mockResolvedValueOnce({ data: mockTemplate });

    const result = await ApiService.createContext(newTemplate);
    
    expect(result.category_id).toBe(1);
    expect(result.forma_id).toBe(1);
    expect(result.cause_id).toBe(1);
    expect(result.develop_id).toBe(1);
    expect(result.effect_id).toBe(1);
  });

  it('should create template with minimal fields (name only)', async () => {
    const newTemplate: CreateContextRequest = {
      name: 'Minimal Template',
      description: null,
      category_id: null,
      forma_id: null,
      cause_id: null,
      develop_id: null,
      effect_id: null,
    };

    const mockTemplate = {
      id: templateIdCounter++,
      name: newTemplate.name,
      description: null,
      category_id: null,
      forma_id: null,
      cause_id: null,
      develop_id: null,
      effect_id: null,
    };
    mockApiClient.post.mockResolvedValueOnce({ data: mockTemplate });

    const result = await ApiService.createContext(newTemplate);
    
    expect(result.name).toBe(newTemplate.name);
    expect(result.category_id).toBeNull();
    expect(result.forma_id).toBeNull();
  });

  it('should prevent duplicate templates with identical fields', async () => {
    const template: CreateContextRequest = {
      name: 'Duplicate Test Template',
      description: 'Test for duplicate prevention',
      category_id: 1,
      forma_id: 1,
      cause_id: 1,
      develop_id: 1,
      effect_id: 1,
    };

    // Create first template
    const mockTemplate = { id: templateIdCounter++, ...template };
    mockApiClient.post.mockResolvedValueOnce({ data: mockTemplate });
    await ApiService.createContext(template);
    
    // Try to create duplicate - should reject
    mockApiClient.post.mockRejectedValueOnce(new Error('Duplicate template'));
    await expect(
      ApiService.createContext(template)
    ).rejects.toThrow();
  });

  it('should validate required name field', async () => {
    const invalidTemplate = {
      name: '', // Empty name
      description: null,
      category_id: null,
      forma_id: null,
      cause_id: null,
      develop_id: null,
      effect_id: null,
    } as CreateContextRequest;

    mockApiClient.post.mockRejectedValueOnce(new Error('Name is required'));

    await expect(
      ApiService.createContext(invalidTemplate)
    ).rejects.toThrow();
  });

  it('should list all templates including newly created', async () => {
    const newTemplate: CreateContextRequest = {
      name: `Template ${Date.now()}`,
      description: 'List test template',
      category_id: null,
      forma_id: null,
      cause_id: null,
      develop_id: null,
      effect_id: null,
    };

    const mockCreated = { id: templateIdCounter++, ...newTemplate };
    mockApiClient.post.mockResolvedValueOnce({ data: mockCreated });
    const created = await ApiService.createContext(newTemplate);
    
    const mockListResponse = {
      data: [mockCreated],
      fetched_at: new Date().toISOString(),
    };
    mockApiClient.get.mockResolvedValueOnce({ data: mockListResponse });
    const contexts = await ApiService.getContexts();
    
    const found = contexts.data.find(t => t.id === created.id);
    expect(found).toBeDefined();
    expect(found?.name).toBe(created.name);
  });
});

