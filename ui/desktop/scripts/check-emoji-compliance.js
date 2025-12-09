#!/usr/bin/env node

/**
 * Automatic emoji compliance checker for Desktop UI.
 * Verifies that all UI elements have emojis according to Rule 8.
 * 
 * Usage: node scripts/check-emoji-compliance.js
 */

const fs = require('fs');
const path = require('path');

const EMOJI_REGEX = /[\u{1F300}-\u{1F9FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/u;
const GET_EMOJI_PATTERN = /getEmoji\(/;

// Directories to check
const COMPONENTS_DIR = path.join(__dirname, '../src/components');
const PAGES_DIR = path.join(__dirname, '../src/pages');

// Files to exclude
const EXCLUDE_PATTERNS = [
  '__tests__',
  '.test.',
  '.spec.',
  'node_modules',
  '.d.ts'
];

// UI element patterns to check
const UI_ELEMENT_PATTERNS = [
  { pattern: /<button[^>]*>([^<]*)<\/button>/g, name: 'button' },
  { pattern: /<label[^>]*>([^<]*)<\/label>/g, name: 'label' },
  { pattern: /title=["']([^"']+)["']/g, name: 'title attribute' },
  { pattern: /aria-label=["']([^"']+)["']/g, name: 'aria-label' },
];

function findFiles(dir, extension = '.tsx') {
  const files = [];
  try {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory() && !EXCLUDE_PATTERNS.some(p => entry.name.includes(p))) {
        files.push(...findFiles(fullPath, extension));
      } else if (entry.isFile() && entry.name.endsWith(extension) && 
                 !EXCLUDE_PATTERNS.some(p => entry.name.includes(p))) {
        files.push(fullPath);
      }
    }
  } catch (err) {
    // Directory doesn't exist or can't be read
  }
  return files;
}

function checkFile(filePath) {
  const content = fs.readFileSync(filePath, 'utf-8');
  const issues = [];
  
  // Check if file uses getEmoji function (indirect emoji usage)
  const usesGetEmoji = GET_EMOJI_PATTERN.test(content);
  
  // Check UI elements
  for (const { pattern, name } of UI_ELEMENT_PATTERNS) {
    const matches = [...content.matchAll(pattern)];
    for (const match of matches) {
      const elementText = match[1]?.trim() || '';
      
      // Skip empty elements
      if (!elementText || elementText.length === 0) continue;
      
      // Skip if element text is just a variable or expression
      if (elementText.startsWith('{') || elementText.includes('${')) continue;
      
      // Check if element has emoji
      const hasEmoji = EMOJI_REGEX.test(elementText);
      
      // If no emoji and file doesn't use getEmoji, it's a potential issue
      if (!hasEmoji && !usesGetEmoji && elementText.length > 0) {
        // But allow if it's a simple variable reference or function call
        if (!elementText.match(/^[a-zA-Z_$][a-zA-Z0-9_$]*$/) && 
            !elementText.includes('()') &&
            !elementText.includes('t(')) {
          issues.push({
            type: name,
            text: elementText.substring(0, 50),
            line: content.substring(0, match.index).split('\n').length
          });
        }
      }
    }
  }
  
  return { file: filePath, usesGetEmoji, issues };
}

function main() {
  console.log('🔍 Checking emoji compliance (Rule 8)...\n');
  
  const componentFiles = findFiles(COMPONENTS_DIR);
  const pageFiles = findFiles(PAGES_DIR);
  const allFiles = [...componentFiles, ...pageFiles];
  
  const results = allFiles.map(checkFile);
  const filesWithIssues = results.filter(r => r.issues.length > 0);
  const filesUsingGetEmoji = results.filter(r => r.usesGetEmoji);
  
  console.log(`📊 Statistics:`);
  console.log(`   Total files checked: ${allFiles.length}`);
  console.log(`   Files using getEmoji(): ${filesUsingGetEmoji.length}`);
  console.log(`   Files with potential issues: ${filesWithIssues.length}\n`);
  
  if (filesWithIssues.length > 0) {
    console.log('⚠️  Potential emoji compliance issues:\n');
    filesWithIssues.forEach(({ file, issues }) => {
      const relativePath = path.relative(process.cwd(), file);
      console.log(`   ${relativePath}:`);
      issues.forEach(issue => {
        console.log(`     - Line ${issue.line}: ${issue.type} "${issue.text}"`);
      });
      console.log('');
    });
    console.log('💡 Note: Some issues may be false positives if emojis are added dynamically via getEmoji().');
    console.log('   Review the files above to ensure all UI elements have appropriate emojis.\n');
    process.exit(1);
  } else {
    console.log('✅ All UI elements appear to have emojis or use getEmoji() function!');
    console.log('   Rule 8 compliance verified.\n');
    process.exit(0);
  }
}

if (require.main === module) {
  main();
}

module.exports = { checkFile, findFiles };

