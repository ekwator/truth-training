/**
 * Integration test for Context Editor template creation.
 * Verifies create template form, save functionality, and template management.
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { ApiService } from '@/services/api';
import type { CreateContextRequest, ContextTemplate } from '@/types/contexts';

describe('Context Editor Template Creation Integration', () => {
  let createdTemplateId: number | null = null;

  beforeEach(async () => {
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
    await ApiService.createContext(template);
    
    // Try to create duplicate
    await expect(
      ApiService.createContext(template)
    ).rejects.toThrow(/409|identical|duplicate/i);
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

    const created = await ApiService.createContext(newTemplate);
    const contexts = await ApiService.getContexts();
    
    const found = contexts.data.find(t => t.id === created.id);
    expect(found).toBeDefined();
    expect(found?.name).toBe(created.name);
  });
});

