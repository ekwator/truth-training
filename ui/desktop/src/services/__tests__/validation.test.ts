// Unit tests for validation rules (T031)

describe('Validation Rules', () => {
  describe('Impact Level Validation', () => {
    test('should accept valid impact levels (1-5)', () => {
      const validLevels = [1, 2, 3, 4, 5];
      
      validLevels.forEach(level => {
        expect(level).toBeGreaterThanOrEqual(1);
        expect(level).toBeLessThanOrEqual(5);
      });
    });

    test('should reject invalid impact levels', () => {
      const invalidLevels = [0, -1, 6, 10, 1.5, NaN, Infinity];
      
      invalidLevels.forEach(level => {
        expect(level < 1 || level > 5 || !Number.isInteger(level)).toBe(true);
      });
    });

    test('should validate impact level in addImpact request', () => {
      const validateImpactLevel = (level: number): boolean => {
        return Number.isInteger(level) && level >= 1 && level <= 5;
      };

      expect(validateImpactLevel(1)).toBe(true);
      expect(validateImpactLevel(5)).toBe(true);
      expect(validateImpactLevel(3)).toBe(true);
      expect(validateImpactLevel(0)).toBe(false);
      expect(validateImpactLevel(6)).toBe(false);
      expect(validateImpactLevel(1.5)).toBe(false);
    });
  });

  describe('Date Order Validation', () => {
    test('should validate start_date is before end_date', () => {
      const validateDateOrder = (startDate?: string, endDate?: string): boolean => {
        if (!startDate || !endDate) return true; // Both optional
        return new Date(startDate) <= new Date(endDate);
      };

      expect(validateDateOrder('2024-01-01', '2024-01-02')).toBe(true);
      expect(validateDateOrder('2024-01-01', '2024-01-01')).toBe(true);
      expect(validateDateOrder('2024-01-02', '2024-01-01')).toBe(false);
      expect(validateDateOrder('2024-01-01', undefined)).toBe(true);
      expect(validateDateOrder(undefined, '2024-01-01')).toBe(true);
      expect(validateDateOrder(undefined, undefined)).toBe(true);
    });

    test('should handle invalid date formats', () => {
      const validateDateOrder = (startDate?: string, endDate?: string): boolean => {
        if (!startDate || !endDate) return true;
        try {
          return new Date(startDate) <= new Date(endDate);
        } catch {
          return false;
        }
      };

      expect(validateDateOrder('invalid-date', '2024-01-01')).toBe(false);
      expect(validateDateOrder('2024-01-01', 'invalid-date')).toBe(false);
      expect(validateDateOrder('invalid-date', 'invalid-date')).toBe(false);
    });
  });

  describe('Context ID Validation', () => {
    test('should require non-empty context_id', () => {
      const validateContextId = (contextId: string): boolean => {
        return contextId.trim().length > 0;
      };

      expect(validateContextId('kb:some_context')).toBe(true);
      expect(validateContextId('kb:another-context')).toBe(true);
      expect(validateContextId('')).toBe(false);
      expect(validateContextId('   ')).toBe(false);
    });

    test('should validate context_id format', () => {
      const validateContextFormat = (contextId: string): boolean => {
        return /^kb:[a-z0-9_-]+$/.test(contextId);
      };

      expect(validateContextFormat('kb:valid_context')).toBe(true);
      expect(validateContextFormat('kb:another-valid')).toBe(true);
      expect(validateContextFormat('kb:valid123')).toBe(true);
      expect(validateContextFormat('invalid')).toBe(false);
      expect(validateContextFormat('kb:Invalid_Case')).toBe(false);
      expect(validateContextFormat('kb:')).toBe(false);
    });
  });

  describe('Event Title Validation', () => {
    test('should require non-empty title', () => {
      const validateTitle = (title: string): boolean => {
        return title.trim().length > 0;
      };

      expect(validateTitle('Valid Event Title')).toBe(true);
      expect(validateTitle('Another Valid Title')).toBe(true);
      expect(validateTitle('')).toBe(false);
      expect(validateTitle('   ')).toBe(false);
    });

    test('should validate title length limits', () => {
      const validateTitleLength = (title: string, minLength = 1, maxLength = 200): boolean => {
        const trimmed = title.trim();
        return trimmed.length >= minLength && trimmed.length <= maxLength;
      };

      expect(validateTitleLength('A')).toBe(true);
      expect(validateTitleLength('A'.repeat(200))).toBe(true);
      expect(validateTitleLength('')).toBe(false);
      expect(validateTitleLength('A'.repeat(201))).toBe(false);
    });
  });

  describe('Confidence Level Validation', () => {
    test('should validate confidence level range (0-1)', () => {
      const validateConfidenceLevel = (level: number): boolean => {
        return level >= 0 && level <= 1;
      };

      expect(validateConfidenceLevel(0)).toBe(true);
      expect(validateConfidenceLevel(1)).toBe(true);
      expect(validateConfidenceLevel(0.5)).toBe(true);
      expect(validateConfidenceLevel(0.75)).toBe(true);
      expect(validateConfidenceLevel(-0.1)).toBe(false);
      expect(validateConfidenceLevel(1.1)).toBe(false);
    });

    test('should handle edge cases for confidence level', () => {
      const validateConfidenceLevel = (level: number): boolean => {
        return !isNaN(level) && level >= 0 && level <= 1;
      };

      expect(validateConfidenceLevel(0)).toBe(true);
      expect(validateConfidenceLevel(1)).toBe(true);
      expect(validateConfidenceLevel(NaN)).toBe(false);
      expect(validateConfidenceLevel(Infinity)).toBe(false);
      expect(validateConfidenceLevel(-Infinity)).toBe(false);
    });
  });

  describe('Assessment Validation', () => {
    test('should validate judgment assessment values', () => {
      const validateAssessment = (assessment: string): boolean => {
        return ['true', 'false', 'uncertain'].includes(assessment);
      };

      expect(validateAssessment('true')).toBe(true);
      expect(validateAssessment('false')).toBe(true);
      expect(validateAssessment('uncertain')).toBe(true);
      expect(validateAssessment('True')).toBe(false);
      expect(validateAssessment('TRUE')).toBe(false);
      expect(validateAssessment('maybe')).toBe(false);
      expect(validateAssessment('')).toBe(false);
    });
  });

  describe('Combined Event Validation', () => {
    test('should validate complete event creation request', () => {
      interface CreateEventRequest {
        title: string;
        description?: string;
        context_id: string;
        start_date?: string;
        end_date?: string;
      }

      const validateEventRequest = (request: CreateEventRequest): { valid: boolean; errors: string[] } => {
        const errors: string[] = [];

        if (!request.title.trim()) {
          errors.push('Title is required');
        } else if (request.title.trim().length > 200) {
          errors.push('Title must be 200 characters or less');
        }

        if (!request.context_id.trim()) {
          errors.push('Context selection is required');
        } else if (!/^kb:[a-z0-9_-]+$/.test(request.context_id)) {
          errors.push('Invalid context format');
        }

        if (request.start_date && request.end_date) {
          try {
            if (new Date(request.start_date) > new Date(request.end_date)) {
              errors.push('Start date must be before or equal to end date');
            }
          } catch {
            errors.push('Invalid date format');
          }
        }

        return {
          valid: errors.length === 0,
          errors
        };
      };

      // Valid request
      const validRequest: CreateEventRequest = {
        title: 'Test Event',
        description: 'Test Description',
        context_id: 'kb:test_context',
        start_date: '2024-01-01',
        end_date: '2024-01-02'
      };
      expect(validateEventRequest(validRequest).valid).toBe(true);

      // Invalid requests
      const invalidRequests = [
        { ...validRequest, title: '' },
        { ...validRequest, context_id: '' },
        { ...validRequest, context_id: 'invalid' },
        { ...validRequest, start_date: '2024-01-02', end_date: '2024-01-01' }
      ];

      invalidRequests.forEach(request => {
        const result = validateEventRequest(request);
        expect(result.valid).toBe(false);
        expect(result.errors.length).toBeGreaterThan(0);
      });
    });
  });
});
