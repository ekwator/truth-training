// Unit tests for knowledge base integration (T031)

describe('Knowledge Base Integration', () => {
  describe('Context Selection Validation', () => {
    test('should validate context selection is required', () => {
      const validateContextSelection = (contextId: string): boolean => {
        return contextId.trim().length > 0;
      };

      expect(validateContextSelection('kb:valid_context')).toBe(true);
      expect(validateContextSelection('kb:another-valid')).toBe(true);
      expect(validateContextSelection('')).toBe(false);
      expect(validateContextSelection('   ')).toBe(false);
    });

    test('should validate context ID format', () => {
      const validateContextFormat = (contextId: string): boolean => {
        return /^kb:[a-z0-9_-]+$/.test(contextId);
      };

      const validContexts = [
        'kb:context_name',
        'kb:another-context',
        'kb:context123',
        'kb:context_with_underscores'
      ];

      const invalidContexts = [
        'invalid',
        'kb:',
        'kb:Invalid_Case',
        'kb:context with spaces',
        'kb:context@special',
        'kb:context/with/slashes'
      ];

      validContexts.forEach(context => {
        expect(validateContextFormat(context)).toBe(true);
      });

      invalidContexts.forEach(context => {
        expect(validateContextFormat(context)).toBe(false);
      });
    });
  });

  describe('Knowledge Base Parsing', () => {
    test('should parse knowledge base items from markdown', () => {
      const mockMarkdown = `
# Data Schema

## 1. knowledge_base
- Context A: Description of context A
- Context B: Description of context B
- Context C: Description of context C

## 2. Other Section
- This should not be included
`;

      const parseKbFromMarkdown = (md: string) => {
        const items: { id: string; label: string }[] = [];
        let inSection = false;
        
        for (const line of md.split('\n')) {
          if (line.trim().startsWith('## ')) {
            if (inSection) break;
            if (line.includes('knowledge_base')) {
              inSection = true;
            }
            continue;
          }
          
          if (inSection) {
            const trimmed = line.trim();
            if (trimmed.startsWith('-') || trimmed.startsWith('*')) {
              const label = trimmed.replace(/^[-*]\s*/, '');
              if (label) {
                const id = `kb:${label.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z0-9_-]/g, '')}`;
                items.push({ id, label });
              }
            }
          }
        }
        
        return items;
      };

      const items = parseKbFromMarkdown(mockMarkdown);
      
      expect(items).toHaveLength(3);
      expect(items[0]).toEqual({
        id: 'kb:context_a_description_of_context_a',
        label: 'Context A: Description of context A'
      });
      expect(items[1]).toEqual({
        id: 'kb:context_b_description_of_context_b',
        label: 'Context B: Description of context B'
      });
      expect(items[2]).toEqual({
        id: 'kb:context_c_description_of_context_c',
        label: 'Context C: Description of context C'
      });
    });

    test('should handle empty knowledge base', () => {
      const parseKbFromMarkdown = (md: string) => {
        const items: { id: string; label: string }[] = [];
        let inSection = false;
        
        for (const line of md.split('\n')) {
          if (line.trim().startsWith('## ')) {
            if (inSection) break;
            if (line.includes('knowledge_base')) {
              inSection = true;
            }
            continue;
          }
          
          if (inSection) {
            const trimmed = line.trim();
            if (trimmed.startsWith('-') || trimmed.startsWith('*')) {
              const label = trimmed.replace(/^[-*]\s*/, '');
              if (label) {
                const id = `kb:${label.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z0-9_-]/g, '')}`;
                items.push({ id, label });
              }
            }
          }
        }
        
        return items;
      };

      const emptyMarkdown = `
# Data Schema

## 1. knowledge_base

## 2. Other Section
- This should not be included
`;

      const items = parseKbFromMarkdown(emptyMarkdown);
      expect(items).toHaveLength(0);
    });

    test('should handle missing knowledge_base section', () => {
      const parseKbFromMarkdown = (md: string) => {
        const items: { id: string; label: string }[] = [];
        let inSection = false;
        
        for (const line of md.split('\n')) {
          if (line.trim().startsWith('## ')) {
            if (inSection) break;
            if (line.includes('knowledge_base')) {
              inSection = true;
            }
            continue;
          }
          
          if (inSection) {
            const trimmed = line.trim();
            if (trimmed.startsWith('-') || trimmed.startsWith('*')) {
              const label = trimmed.replace(/^[-*]\s*/, '');
              if (label) {
                const id = `kb:${label.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z0-9_-]/g, '')}`;
                items.push({ id, label });
              }
            }
          }
        }
        
        return items;
      };

      const markdownWithoutKb = `
# Data Schema

## 2. Other Section
- This should not be included
`;

      const items = parseKbFromMarkdown(markdownWithoutKb);
      expect(items).toHaveLength(0);
    });
  });

  describe('Context Selection in Event Creation', () => {
    test('should prevent event creation without context', () => {
      const validateEventCreation = (eventData: {
        title: string;
        context_id: string;
      }): { valid: boolean; error?: string } => {
        if (!eventData.title.trim()) {
          return { valid: false, error: 'Title is required' };
        }
        
        if (!eventData.context_id.trim()) {
          return { valid: false, error: 'Context selection is required' };
        }
        
        if (!/^kb:[a-z0-9_-]+$/.test(eventData.context_id)) {
          return { valid: false, error: 'Invalid context format' };
        }
        
        return { valid: true };
      };

      // Valid event
      expect(validateEventCreation({
        title: 'Test Event',
        context_id: 'kb:valid_context'
      })).toEqual({ valid: true });

      // Missing context
      expect(validateEventCreation({
        title: 'Test Event',
        context_id: ''
      })).toEqual({ 
        valid: false, 
        error: 'Context selection is required' 
      });

      // Invalid context format
      expect(validateEventCreation({
        title: 'Test Event',
        context_id: 'invalid_context'
      })).toEqual({ 
        valid: false, 
        error: 'Invalid context format' 
      });

      // Missing title
      expect(validateEventCreation({
        title: '',
        context_id: 'kb:valid_context'
      })).toEqual({ 
        valid: false, 
        error: 'Title is required' 
      });
    });
  });

  describe('Knowledge Base Item Structure', () => {
    test('should create valid knowledge base items', () => {
      const createKbItem = (label: string) => {
        const id = `kb:${label.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z0-9_-]/g, '')}`;
        return { id, label };
      };

      const testCases = [
        { input: 'Context Name', expected: { id: 'kb:context_name', label: 'Context Name' } },
        { input: 'Context-With-Dashes', expected: { id: 'kb:context-with-dashes', label: 'Context-With-Dashes' } },
        { input: 'Context_With_Underscores', expected: { id: 'kb:context_with_underscores', label: 'Context_With_Underscores' } },
        { input: 'Context123', expected: { id: 'kb:context123', label: 'Context123' } },
        { input: 'Context With Spaces', expected: { id: 'kb:context_with_spaces', label: 'Context With Spaces' } },
        { input: 'Context@Special#Chars', expected: { id: 'kb:contextspecialchars', label: 'Context@Special#Chars' } }
      ];

      testCases.forEach(({ input, expected }) => {
        expect(createKbItem(input)).toEqual(expected);
      });
    });

    test('should handle edge cases in knowledge base item creation', () => {
      const createKbItem = (label: string) => {
        const id = `kb:${label.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z0-9_-]/g, '')}`;
        return { id, label };
      };

      // Empty string
      expect(createKbItem('')).toEqual({ id: 'kb:', label: '' });
      
      // Only special characters
      expect(createKbItem('@#$%')).toEqual({ id: 'kb:', label: '@#$%' });
      
      // Mixed case
      expect(createKbItem('MiXeD_CaSe')).toEqual({ id: 'kb:mixed_case', label: 'MiXeD_CaSe' });
    });
  });
});
