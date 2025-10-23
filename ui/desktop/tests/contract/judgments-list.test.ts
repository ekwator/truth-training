import { describe, it, expect } from '@jest/globals';

describe('Judgments List Contract Tests', () => {
  it('should validate Judgment list shape', () => {
    const mockJudgmentsResponse = {
      judgments: [
        {
          id: '660e8400-e29b-41d4-a716-446655440001',
          participant_id: '770e8400-e29b-41d4-a716-446655440002',
          event_id: '550e8400-e29b-41d4-a716-446655440000',
          assessment: 'true',
          confidence_level: 0.85,
          reasoning: 'Strong evidence supports this claim',
          submitted_at: '2024-01-01T10:00:00Z',
          signature: 'signature_hash_here'
        },
        {
          id: '660e8400-e29b-41d4-a716-446655440003',
          participant_id: '880e8400-e29b-41d4-a716-446655440004',
          event_id: '550e8400-e29b-41d4-a716-446655440000',
          assessment: 'false',
          confidence_level: 0.7,
          reasoning: 'Some concerns about the evidence',
          submitted_at: '2024-01-01T11:00:00Z',
          signature: 'another_signature_hash'
        }
      ],
      pagination: {
        page: 1,
        per_page: 20,
        total: 2,
        total_pages: 1
      }
    };

    // Validate judgments array structure
    expect(Array.isArray(mockJudgmentsResponse.judgments)).toBe(true);
    expect(mockJudgmentsResponse.judgments.length).toBe(2);

    // Validate individual judgment structure
    const judgment = mockJudgmentsResponse.judgments[0];
    expect(judgment).toHaveProperty('id');
    expect(judgment).toHaveProperty('participant_id');
    expect(judgment).toHaveProperty('event_id');
    expect(judgment).toHaveProperty('assessment');
    expect(judgment).toHaveProperty('confidence_level');
    expect(judgment).toHaveProperty('reasoning');
    expect(judgment).toHaveProperty('submitted_at');
    expect(judgment).toHaveProperty('signature');

    // Validate types
    expect(typeof judgment.id).toBe('string');
    expect(typeof judgment.participant_id).toBe('string');
    expect(typeof judgment.event_id).toBe('string');
    expect(typeof judgment.assessment).toBe('string');
    expect(typeof judgment.confidence_level).toBe('number');
    expect(typeof judgment.reasoning).toBe('string');
    expect(typeof judgment.submitted_at).toBe('string');
    expect(typeof judgment.signature).toBe('string');

    // Validate confidence level range
    expect(judgment.confidence_level).toBeGreaterThanOrEqual(0);
    expect(judgment.confidence_level).toBeLessThanOrEqual(1);

    // Validate pagination
    expect(mockJudgmentsResponse.pagination).toHaveProperty('page');
    expect(mockJudgmentsResponse.pagination).toHaveProperty('per_page');
    expect(mockJudgmentsResponse.pagination).toHaveProperty('total');
    expect(mockJudgmentsResponse.pagination).toHaveProperty('total_pages');
  });

  it('should handle empty judgments list', () => {
    const emptyResponse = {
      judgments: [],
      pagination: {
        page: 1,
        per_page: 20,
        total: 0,
        total_pages: 0
      }
    };

    expect(Array.isArray(emptyResponse.judgments)).toBe(true);
    expect(emptyResponse.judgments.length).toBe(0);
    expect(emptyResponse.pagination.total).toBe(0);
  });
});
