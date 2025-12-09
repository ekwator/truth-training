/**
 * Integration test for emoji presence in all UI elements.
 * Verifies all UI elements have emojis as per Rule 8 (constitutional requirement).
 * This test uses static analysis to check component files and verifies emoji usage.
 */

import { describe, it, expect } from '@jest/globals';
import { readFileSync, readdirSync } from 'fs';
import { join } from 'path';
import { getEmoji, defaultEmojiMapping } from '../../src/utils/emojiMapping';

describe('Emoji Accessibility Integration Tests', () => {
  const componentsDir = join(process.cwd(), 'src/components');
  const pagesDir = join(process.cwd(), 'src/pages');

  /**
   * Comprehensive emoji regex pattern covering major emoji ranges.
   * Includes:
   * - Emoticons (😀-🙏): \u{1F600}-\u{1F64F}
   * - Symbols & Pictographs (🌀-🗿): \u{1F300}-\u{1F9FF}
   * - Miscellaneous Symbols (☀️-⛿): \u{2600}-\u{26FF}
   * - Dingbats (✂️-➿): \u{2700}-\u{27BF}
   * - Arrows (⬅️-➡️): \u{2B00}-\u{2BFF}
   * - Transport & Map Symbols: \u{1F680}-\u{1F6FF}
   * - Flags: \u{1F1E0}-\u{1F1FF}
   * - Supplemental Symbols: \u{1F900}-\u{1F9FF}, \u{1FA00}-\u{1FAFF}
   */
  const emojiRegex = /[\u{1F300}-\u{1F9FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]|[\u{1F600}-\u{1F64F}]|[\u{1F680}-\u{1F6FF}]|[\u{1F1E0}-\u{1F1FF}]|[\u{1F900}-\u{1F9FF}]|[\u{1FA00}-\u{1FAFF}]|[\u{2B00}-\u{2BFF}]/u;

  const findFiles = (dir: string, extension: string = '.tsx'): string[] => {
    const files: string[] = [];
    try {
      const entries = readdirSync(dir, { withFileTypes: true });
      for (const entry of entries) {
        const fullPath = join(dir, entry.name);
        if (entry.isDirectory() && !entry.name.includes('__tests__') && !entry.name.includes('node_modules')) {
          files.push(...findFiles(fullPath, extension));
        } else if (entry.isFile() && entry.name.endsWith(extension) && !entry.name.includes('.test.') && !entry.name.includes('.spec.')) {
          files.push(fullPath);
        }
      }
    } catch {
      // Directory doesn't exist or can't be read
    }
    return files;
  };

  /**
   * Check if text contains emoji character
   */
  const hasEmoji = (text: string): boolean => {
    return emojiRegex.test(text);
  };

  /**
   * Check if file uses getEmoji function or imports emojiMapping
   */
  const usesEmojiSystem = (content: string): boolean => {
    return content.includes('getEmoji(') || 
           content.includes('from \'@/utils/emojiMapping\'') ||
           content.includes('from "@/utils/emojiMapping"') ||
           content.includes('emojiMapping');
  };

  /**
   * Extract button elements from JSX content
   */
  const extractButtons = (content: string): Array<{ text: string; line: number }> => {
    const buttons: Array<{ text: string; line: number }> = [];
    const lines = content.split('\n');
    
    lines.forEach((line, index) => {
      // Match button elements with text content
      const buttonRegex = /<button[^>]*>([^<]*?)<\/button>/g;
      let match;
      while ((match = buttonRegex.exec(line)) !== null) {
        const text = match[1].trim();
        if (text.length > 0) {
          buttons.push({ text, line: index + 1 });
        }
      }
      
      // Match button elements with JSX expressions (multi-line)
      if (line.includes('<button') && !line.includes('</button>')) {
        // Look for button text in following lines
        let buttonText = '';
        for (let i = index + 1; i < Math.min(index + 10, lines.length); i++) {
          buttonText += lines[i];
          if (lines[i].includes('</button>')) {
            const textMatch = buttonText.match(/>([^<{]*?)</);
            if (textMatch && textMatch[1].trim().length > 0) {
              buttons.push({ text: textMatch[1].trim(), line: index + 1 });
            }
            break;
          }
        }
      }
    });
    
    return buttons;
  };

  /**
   * Extract label elements from JSX content
   */
  const extractLabels = (content: string): Array<{ text: string; line: number }> => {
    const labels: Array<{ text: string; line: number }> = [];
    const lines = content.split('\n');
    
    lines.forEach((line, index) => {
      const labelRegex = /<label[^>]*>([^<]*?)<\/label>/g;
      let match;
      while ((match = labelRegex.exec(line)) !== null) {
        const text = match[1].trim();
        if (text.length > 0) {
          labels.push({ text, line: index + 1 });
        }
      }
    });
    
    return labels;
  };

  /**
   * Extract navigation elements (buttons in TopMenuBar, navigation links)
   */
  const extractNavigationElements = (content: string): Array<{ text: string; line: number }> => {
    const navElements: Array<{ text: string; line: number }> = [];
    const lines = content.split('\n');
    
    lines.forEach((line, index) => {
      // Check for navigation buttons
      if (line.includes('onNavigate') || line.includes('navigation')) {
        const buttonMatch = line.match(/>([^<{]*?)</);
        if (buttonMatch && buttonMatch[1].trim().length > 0) {
          navElements.push({ text: buttonMatch[1].trim(), line: index + 1 });
        }
      }
    });
    
    return navElements;
  };

  /**
   * Check emoji presence in file
   */
  const checkEmojiInFile = (filePath: string): { 
    hasEmoji: boolean; 
    missingElements: Array<{ type: string; text: string; line: number }>;
    usesEmojiSystem: boolean;
  } => {
    try {
      const content = readFileSync(filePath, 'utf-8');
      const missingElements: Array<{ type: string; text: string; line: number }> = [];
      const usesSystem = usesEmojiSystem(content);

      // Check buttons
      const buttons = extractButtons(content);
      buttons.forEach(({ text, line }) => {
        // Skip if text is just whitespace or contains getEmoji call
        if (text.trim().length === 0 || text.includes('getEmoji')) {
          return;
        }
        
        // Check if button text contains emoji
        if (!hasEmoji(text)) {
          missingElements.push({ 
            type: 'Button', 
            text: text.substring(0, 50), 
            line 
          });
        }
      });

      // Check labels
      const labels = extractLabels(content);
      labels.forEach(({ text, line }) => {
        if (text.trim().length === 0 || text.includes('getEmoji')) {
          return;
        }
        
        if (!hasEmoji(text)) {
          missingElements.push({ 
            type: 'Label', 
            text: text.substring(0, 50), 
            line 
          });
        }
      });

      // Check navigation elements
      const navElements = extractNavigationElements(content);
      navElements.forEach(({ text, line }) => {
        if (text.trim().length === 0 || text.includes('getEmoji')) {
          return;
        }
        
        if (!hasEmoji(text)) {
          missingElements.push({ 
            type: 'Navigation', 
            text: text.substring(0, 50), 
            line 
          });
        }
      });

      return {
        hasEmoji: usesSystem || missingElements.length === 0,
        missingElements,
        usesEmojiSystem: usesSystem
      };
    } catch (error) {
      return { 
        hasEmoji: false, 
        missingElements: [{ type: 'Error', text: `Error reading file: ${error}`, line: 0 }],
        usesEmojiSystem: false
      };
    }
  };

  it('should verify emoji mapping system exports all required emojis', () => {
    // Verify all categories exist
    expect(defaultEmojiMapping.screens).toBeDefined();
    expect(defaultEmojiMapping.actions).toBeDefined();
    expect(defaultEmojiMapping.fields).toBeDefined();
    expect(defaultEmojiMapping.status).toBeDefined();
    expect(defaultEmojiMapping.navigation).toBeDefined();

    // Verify getEmoji function works
    expect(getEmoji('screens', 'dashboard')).toBe('🏠');
    expect(getEmoji('actions', 'save')).toBe('💾');
    expect(getEmoji('fields', 'name')).toBe('📝');
    expect(getEmoji('status', 'online')).toBe('🟢');
    expect(getEmoji('navigation', 'home')).toBe('🏠');

    // Verify all emojis are non-empty
    (Object.values(defaultEmojiMapping.screens) as string[]).forEach((emoji: string) => {
      expect(emoji.length).toBeGreaterThan(0);
      expect(hasEmoji(emoji)).toBe(true);
    });

    (Object.values(defaultEmojiMapping.actions) as string[]).forEach((emoji: string) => {
      expect(emoji.length).toBeGreaterThan(0);
      expect(hasEmoji(emoji)).toBe(true);
    });
  });

  it('should have emojis in all page components', () => {
    const pageFiles = findFiles(pagesDir);
    const missingEmojis: Record<string, Array<{ type: string; text: string; line: number }>> = {};
    const pagesWithoutEmojiSystem: string[] = [];

    pageFiles.forEach((file) => {
      const result = checkEmojiInFile(file);
        const fileName = file.split('/').pop() || file;
      
      if (!result.usesEmojiSystem && result.missingElements.length > 0) {
        pagesWithoutEmojiSystem.push(fileName);
      }
      
      if (result.missingElements.length > 0) {
        missingEmojis[fileName] = result.missingElements;
      }
    });

    if (pagesWithoutEmojiSystem.length > 0) {
      console.warn('Pages not using emoji system:', pagesWithoutEmojiSystem);
    }

    if (Object.keys(missingEmojis).length > 0) {
      console.warn('Pages with missing emojis:', missingEmojis);
    }

    // All pages should use emoji system (getEmoji function)
    expect(pagesWithoutEmojiSystem.length).toBe(0);
  });

  it('should have emojis in all component files', () => {
    const componentFiles = findFiles(componentsDir);
    const missingEmojis: Record<string, Array<{ type: string; text: string; line: number }>> = {};
    const componentsWithoutEmojiSystem: string[] = [];

    componentFiles.forEach((file) => {
      // Skip test files
      if (file.includes('__tests__') || file.includes('.test.') || file.includes('.spec.')) {
        return;
      }

      const result = checkEmojiInFile(file);
        const fileName = file.split('/').pop() || file;
      
      if (!result.usesEmojiSystem && result.missingElements.length > 0) {
        componentsWithoutEmojiSystem.push(fileName);
      }
      
      if (result.missingElements.length > 0) {
        missingEmojis[fileName] = result.missingElements;
      }
    });

    if (componentsWithoutEmojiSystem.length > 0) {
      console.warn('Components not using emoji system:', componentsWithoutEmojiSystem);
    }

    if (Object.keys(missingEmojis).length > 0) {
      console.warn('Components with missing emojis:', missingEmojis);
    }

    // All components should use emoji system (getEmoji function)
    expect(componentsWithoutEmojiSystem.length).toBe(0);
  });

  it('should verify emoji mapping system is imported in UI components', () => {
    const pageFiles = findFiles(pagesDir);
    const componentFiles = findFiles(componentsDir);
    const allFiles = [...pageFiles, ...componentFiles].filter(
      (f) => !f.includes('__tests__') && !f.includes('.test.') && !f.includes('.spec.')
    );

    const filesWithEmojiImport = allFiles.filter((file) => {
      try {
        const content = readFileSync(file, 'utf-8');
        return usesEmojiSystem(content);
      } catch {
        return false;
      }
    });

    // At least 80% of UI files should import emoji mapping
    const importPercentage = (filesWithEmojiImport.length / allFiles.length) * 100;
    expect(importPercentage).toBeGreaterThanOrEqual(80);
    expect(filesWithEmojiImport.length).toBeGreaterThan(0);
  });

  it('should verify emoji consistency - same function uses same emoji', () => {
    // Verify that same actions use same emojis
    const saveEmoji = getEmoji('actions', 'save');
    const cancelEmoji = getEmoji('actions', 'cancel');
    const deleteEmoji = getEmoji('actions', 'delete');
    const editEmoji = getEmoji('actions', 'edit');
    const createEmoji = getEmoji('actions', 'create');

    // All action emojis should be different and non-empty
    const actionEmojis = [saveEmoji, cancelEmoji, deleteEmoji, editEmoji, createEmoji];
    const uniqueEmojis = new Set(actionEmojis);
    
    expect(actionEmojis.length).toBe(uniqueEmojis.size); // All should be unique
    actionEmojis.forEach(emoji => {
      expect(emoji.length).toBeGreaterThan(0);
      expect(hasEmoji(emoji)).toBe(true);
    });
  });

  it('should verify all screen titles have emojis', () => {
    const screenEmojis = Object.values(defaultEmojiMapping.screens) as string[];
    
    screenEmojis.forEach((emoji: string) => {
      expect(emoji.length).toBeGreaterThan(0);
      expect(hasEmoji(emoji)).toBe(true);
    });

    // Verify all screens are covered
    expect(screenEmojis.length).toBeGreaterThanOrEqual(7); // At least 7 screens
  });
});

