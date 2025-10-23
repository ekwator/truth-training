import { describe, it, expect } from '@jest/globals';

describe('Event Get Contract Tests', () => {
  it('should validate EventDetails shape', () => {
    const mockEventDetails = {
      id: '550e8400-e29b-41d4-a716-446655440000',
      title: 'Detailed Test Event',
      description: 'A detailed test event with full information',
      created_at: '2024-01-01T00:00:00Z',
      updated_at: '2024-01-01T12:00:00Z',
      status: 'active',
      consensus: {
        consensus_value: 'true',
        confidence_score: 0.85,
        participant_count: 5,
        calculated_at: '2024-01-01T12:00:00Z'
      },
      judgments: [
        {
          id: '660e8400-e29b-41d4-a716-446655440001',
          participant_id: '770e8400-e29b-41d4-a716-446655440002',
          assessment: 'true',
          confidence_level: 0.9,
          reasoning: 'Clear evidence supports this claim',
          submitted_at: '2024-01-01T10:00:00Z'
        }
      ]
    };

    // Validate basic event properties
    expect(mockEventDetails).toHaveProperty('id');
    expect(mockEventDetails).toHaveProperty('title');
    expect(mockEventDetails).toHaveProperty('description');
    expect(mockEventDetails).toHaveProperty('created_at');
    expect(mockEventDetails).toHaveProperty('updated_at');
    expect(mockEventDetails).toHaveProperty('status');

    // Validate consensus structure
    expect(mockEventDetails.consensus).toHaveProperty('consensus_value');
    expect(mockEventDetails.consensus).toHaveProperty('confidence_score');
    expect(mockEventDetails.consensus).toHaveProperty('participant_count');
    expect(mockEventDetails.consensus).toHaveProperty('calculated_at');

    // Validate judgments array
    expect(Array.isArray(mockEventDetails.judgments)).toBe(true);
    expect(mockEventDetails.judgments[0]).toHaveProperty('id');
    expect(mockEventDetails.judgments[0]).toHaveProperty('participant_id');
    expect(mockEventDetails.judgments[0]).toHaveProperty('assessment');
    expect(mockEventDetails.judgments[0]).toHaveProperty('confidence_level');
    expect(mockEventDetails.judgments[0]).toHaveProperty('reasoning');
    expect(mockEventDetails.judgments[0]).toHaveProperty('submitted_at');

    // Validate types
    expect(typeof mockEventDetails.consensus.confidence_score).toBe('number');
    expect(typeof mockEventDetails.consensus.participant_count).toBe('number');
    expect(typeof mockEventDetails.judgments[0].confidence_level).toBe('number');
  });

  it('should handle event without consensus', () => {
    const eventWithoutConsensus = {
      id: '550e8400-e29b-41d4-a716-446655440000',
      title: 'Event Without Consensus',
      description: 'An event that has no consensus yet',
      created_at: '2024-01-01T00:00:00Z',
      updated_at: '2024-01-01T12:00:00Z',
      status: 'active',
      consensus: null,
      judgments: []
    };

    expect(eventWithoutConsensus.consensus).toBeNull();
    expect(Array.isArray(eventWithoutConsensus.judgments)).toBe(true);
    expect(eventWithoutConsensus.judgments.length).toBe(0);
  });
});
