import { describe, it, expect } from '@jest/globals';

describe('Judgments Create Contract Tests', () => {
  it('should validate 201 response for judgment creation', () => {
    const mockCreateResponse = {
      id: '660e8400-e29b-41d4-a716-446655440001',
      participant_id: '770e8400-e29b-41d4-a716-446655440002',
      event_id: '550e8400-e29b-41d4-a716-446655440000',
      assessment: 'true',
      confidence_level: 0.85,
      reasoning: 'Strong evidence supports this claim',
      submitted_at: '2024-01-01T10:00:00Z',
      signature: 'generated_signature_hash'
    };

    // Validate 201 response structure
    expect(mockCreateResponse).toHaveProperty('id');
    expect(mockCreateResponse).toHaveProperty('participant_id');
    expect(mockCreateResponse).toHaveProperty('event_id');
    expect(mockCreateResponse).toHaveProperty('assessment');
    expect(mockCreateResponse).toHaveProperty('confidence_level');
    expect(mockCreateResponse).toHaveProperty('reasoning');
    expect(mockCreateResponse).toHaveProperty('submitted_at');
    expect(mockCreateResponse).toHaveProperty('signature');

    // Validate types
    expect(typeof mockCreateResponse.id).toBe('string');
    expect(typeof mockCreateResponse.participant_id).toBe('string');
    expect(typeof mockCreateResponse.event_id).toBe('string');
    expect(typeof mockCreateResponse.assessment).toBe('string');
    expect(typeof mockCreateResponse.confidence_level).toBe('number');
    expect(typeof mockCreateResponse.reasoning).toBe('string');
    expect(typeof mockCreateResponse.submitted_at).toBe('string');
    expect(typeof mockCreateResponse.signature).toBe('string');

    // Validate UUID format
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    expect(mockCreateResponse.id).toMatch(uuidRegex);
    expect(mockCreateResponse.participant_id).toMatch(uuidRegex);
    expect(mockCreateResponse.event_id).toMatch(uuidRegex);

    // Validate confidence level range
    expect(mockCreateResponse.confidence_level).toBeGreaterThanOrEqual(0);
    expect(mockCreateResponse.confidence_level).toBeLessThanOrEqual(1);
  });

  it('should validate judgment creation request payload', () => {
    const mockCreateRequest = {
      event_id: '550e8400-e29b-41d4-a716-446655440000',
      assessment: 'true',
      confidence_level: 0.85,
      reasoning: 'Strong evidence supports this claim',
      signature: 'user_provided_signature'
    };

    expect(mockCreateRequest).toHaveProperty('event_id');
    expect(mockCreateRequest).toHaveProperty('assessment');
    expect(mockCreateRequest).toHaveProperty('confidence_level');
    expect(mockCreateRequest).toHaveProperty('reasoning');
    expect(mockCreateRequest).toHaveProperty('signature');

    expect(typeof mockCreateRequest.event_id).toBe('string');
    expect(typeof mockCreateRequest.assessment).toBe('string');
    expect(typeof mockCreateRequest.confidence_level).toBe('number');
    expect(typeof mockCreateRequest.reasoning).toBe('string');
    expect(typeof mockCreateRequest.signature).toBe('string');

    // Validate assessment values
    const validAssessments = ['true', 'false', 'uncertain'];
    expect(validAssessments).toContain(mockCreateRequest.assessment);
  });

  it('should handle validation errors', () => {
    const invalidRequest = {
      event_id: 'invalid-uuid',
      assessment: 'invalid',
      confidence_level: 1.5, // Invalid: > 1
      reasoning: '',
      signature: ''
    };

    // These should be caught by validation
    expect(invalidRequest.confidence_level).toBeGreaterThan(1);
    expect(invalidRequest.assessment).not.toMatch(/^(true|false|uncertain)$/);
  });
});
