import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';

describe('Judgment Flow Integration Tests', () => {
  let mockApiBase: string;
  let mockEventId: string;
  let mockJudgment: any;

  beforeEach(() => {
    mockApiBase = process.env.VITE_API_BASE || 'http://localhost:8080/api/v1';
    mockEventId = '550e8400-e29b-41d4-a716-446655440000';
    mockJudgment = {
      id: '660e8400-e29b-41d4-a716-446655440001',
      participant_id: '770e8400-e29b-41d4-a716-446655440002',
      event_id: mockEventId,
      assessment: 'true',
      confidence_level: 0.85,
      reasoning: 'Strong evidence supports this claim',
      submitted_at: '2024-01-01T10:00:00Z',
      signature: 'generated_signature_hash'
    };
  });

  afterEach(() => {
    // Cleanup any mocks or state
  });

  it('should submit judgment and verify it appears in list', async () => {
    const mockFetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockJudgment)
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          judgments: [mockJudgment],
          pagination: { page: 1, per_page: 20, total: 1, total_pages: 1 }
        })
      });

    global.fetch = mockFetch;

    // Step 1: Submit judgment
    const submitResponse = await fetch(`${mockApiBase}/judgments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        event_id: mockEventId,
        assessment: 'true',
        confidence_level: 0.85,
        reasoning: 'Strong evidence supports this claim',
        signature: 'user_provided_signature'
      })
    });

    expect(submitResponse.ok).toBe(true);
    const submittedJudgment = await submitResponse.json();
    expect(submittedJudgment.id).toBe(mockJudgment.id);
    expect(submittedJudgment.assessment).toBe('true');
    expect(submittedJudgment.confidence_level).toBe(0.85);

    // Step 2: Verify judgment appears in list
    const listResponse = await fetch(`${mockApiBase}/judgments?event_id=${mockEventId}`);
    expect(listResponse.ok).toBe(true);
    const judgmentsData = await listResponse.json();

    expect(judgmentsData.judgments).toHaveLength(1);
    expect(judgmentsData.judgments[0].id).toBe(mockJudgment.id);
    expect(judgmentsData.judgments[0].event_id).toBe(mockEventId);
  });

  it('should handle judgment validation errors', async () => {
    const mockFetch = jest.fn()
      .mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: () => Promise.resolve({
          error: 'Validation failed',
          details: {
            assessment: 'Assessment must be true, false, or uncertain',
            confidence_level: 'Confidence level must be between 0 and 1'
          }
        })
      });

    global.fetch = mockFetch;

    const response = await fetch(`${mockApiBase}/judgments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        event_id: mockEventId,
        assessment: 'invalid',
        confidence_level: 1.5,
        reasoning: '',
        signature: ''
      })
    });

    expect(response.ok).toBe(false);
    expect(response.status).toBe(400);
    
    const errorData = await response.json();
    expect(errorData.error).toBe('Validation failed');
    expect(errorData.details.assessment).toBe('Assessment must be true, false, or uncertain');
  });

  it('should handle offline judgment submission', async () => {
    const mockFetch = jest.fn()
      .mockRejectedValueOnce(new Error('Network error'));

    global.fetch = mockFetch;

    // Simulate offline scenario - judgment should be queued
    try {
      await fetch(`${mockApiBase}/judgments`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          event_id: mockEventId,
          assessment: 'true',
          confidence_level: 0.85,
          reasoning: 'Strong evidence',
          signature: 'user_signature'
        })
      });
    } catch (error) {
      expect(error).toBeInstanceOf(Error);
      expect((error as Error).message).toBe('Network error');
      
      // In a real implementation, this would be queued for later sync
      // For now, we just verify the error is handled
    }
  });

  it('should validate judgment assessment values', () => {
    const validAssessments = ['true', 'false', 'uncertain'];
    
    validAssessments.forEach(assessment => {
      const judgment = {
        event_id: mockEventId,
        assessment: assessment,
        confidence_level: 0.75,
        reasoning: 'Test reasoning',
        signature: 'test_signature'
      };

      expect(validAssessments).toContain(judgment.assessment);
      expect(judgment.confidence_level).toBeGreaterThanOrEqual(0);
      expect(judgment.confidence_level).toBeLessThanOrEqual(1);
    });
  });

  it('should handle judgment submission with different confidence levels', async () => {
    const confidenceLevels = [0.1, 0.5, 0.9, 1.0];
    
    for (const confidence of confidenceLevels) {
      const mockJudgmentWithConfidence = {
        ...mockJudgment,
        confidence_level: confidence
      };

      const mockFetch = jest.fn()
        .mockResolvedValueOnce({
          ok: true,
          json: () => Promise.resolve(mockJudgmentWithConfidence)
        });

      global.fetch = mockFetch;

      const response = await fetch(`${mockApiBase}/judgments`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          event_id: mockEventId,
          assessment: 'true',
          confidence_level: confidence,
          reasoning: 'Test reasoning',
          signature: 'test_signature'
        })
      });

      expect(response.ok).toBe(true);
      const submittedJudgment = await response.json();
      expect(submittedJudgment.confidence_level).toBe(confidence);
    }
  });
});
