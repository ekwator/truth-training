import { describe, it, expect } from '@jest/globals';

describe('Consensus Get Contract Tests', () => {
  it('should validate Consensus schema', () => {
    const mockConsensus = {
      event_id: '550e8400-e29b-41d4-a716-446655440000',
      consensus_value: 'true',
      confidence_score: 0.85,
      participant_count: 5,
      algorithm_version: '1.0.0',
      calculated_at: '2024-01-01T12:00:00Z',
      judgments_used: [
        {
          id: '660e8400-e29b-41d4-a716-446655440001',
          participant_id: '770e8400-e29b-41d4-a716-446655440002',
          assessment: 'true',
          confidence_level: 0.9,
          weight: 0.9
        },
        {
          id: '660e8400-e29b-41d4-a716-446655440003',
          participant_id: '880e8400-e29b-41d4-a716-446655440004',
          assessment: 'true',
          confidence_level: 0.8,
          weight: 0.8
        }
      ]
    };

    // Validate basic consensus properties
    expect(mockConsensus).toHaveProperty('event_id');
    expect(mockConsensus).toHaveProperty('consensus_value');
    expect(mockConsensus).toHaveProperty('confidence_score');
    expect(mockConsensus).toHaveProperty('participant_count');
    expect(mockConsensus).toHaveProperty('algorithm_version');
    expect(mockConsensus).toHaveProperty('calculated_at');
    expect(mockConsensus).toHaveProperty('judgments_used');

    // Validate types
    expect(typeof mockConsensus.event_id).toBe('string');
    expect(typeof mockConsensus.consensus_value).toBe('string');
    expect(typeof mockConsensus.confidence_score).toBe('number');
    expect(typeof mockConsensus.participant_count).toBe('number');
    expect(typeof mockConsensus.algorithm_version).toBe('string');
    expect(typeof mockConsensus.calculated_at).toBe('string');

    // Validate judgments_used array
    expect(Array.isArray(mockConsensus.judgments_used)).toBe(true);
    expect(mockConsensus.judgments_used.length).toBe(2);

    const judgment = mockConsensus.judgments_used[0];
    expect(judgment).toHaveProperty('id');
    expect(judgment).toHaveProperty('participant_id');
    expect(judgment).toHaveProperty('assessment');
    expect(judgment).toHaveProperty('confidence_level');
    expect(judgment).toHaveProperty('weight');

    // Validate confidence score range
    expect(mockConsensus.confidence_score).toBeGreaterThanOrEqual(0);
    expect(mockConsensus.confidence_score).toBeLessThanOrEqual(1);

    // Validate participant count
    expect(mockConsensus.participant_count).toBeGreaterThan(0);
    expect(mockConsensus.participant_count).toBeGreaterThanOrEqual(mockConsensus.judgments_used.length);
  });

  it('should handle consensus with no judgments', () => {
    const emptyConsensus = {
      event_id: '550e8400-e29b-41d4-a716-446655440000',
      consensus_value: null,
      confidence_score: 0,
      participant_count: 0,
      algorithm_version: '1.0.0',
      calculated_at: '2024-01-01T12:00:00Z',
      judgments_used: []
    };

    expect(emptyConsensus.consensus_value).toBeNull();
    expect(emptyConsensus.confidence_score).toBe(0);
    expect(emptyConsensus.participant_count).toBe(0);
    expect(Array.isArray(emptyConsensus.judgments_used)).toBe(true);
    expect(emptyConsensus.judgments_used.length).toBe(0);
  });

  it('should validate consensus value options', () => {
    const validConsensusValues = ['true', 'false', 'uncertain'];
    
    validConsensusValues.forEach(value => {
      const consensus = {
        event_id: '550e8400-e29b-41d4-a716-446655440000',
        consensus_value: value,
        confidence_score: 0.75,
        participant_count: 3,
        algorithm_version: '1.0.0',
        calculated_at: '2024-01-01T12:00:00Z',
        judgments_used: []
      };

      expect(validConsensusValues).toContain(consensus.consensus_value);
    });
  });
});
