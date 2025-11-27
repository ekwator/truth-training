#!/usr/bin/env python3
"""Final cleanup of all remaining link formatting issues."""

import re
from pathlib import Path

def fix_final_link_issues(file_path: Path) -> bool:
    """Fix remaining link formatting issues in a file."""
    try:
        content = file_path.read_text(encoding='utf-8')
    except UnicodeDecodeError:
        try:
            content = file_path.read_text(encoding='latin-1')
        except:
            print(f"Could not read file: {file_path}")
            return False
    
    original_content = content
    
    # Pattern 1: Fix links with extra closing parenthesis: [text](path))
    pattern1 = r'(\[[^\]]+\]\([^\)]+\))\)'
    content = re.sub(pattern1, r'\1', content)
    
    # Pattern 2: Fix links with extra closing bracket: [text](path)]
    pattern2 = r'(\[[^\]]+\]\([^\)]+\))\]'
    content = re.sub(pattern2, r'\1', content)
    
    # Pattern 3: Fix malformed links in code blocks: `[text](path))`
    pattern3 = r'`(\[[^\]]+\]\([^\)]+\))\)`'
    content = re.sub(pattern3, r'`\1`', content)
    
    # Pattern 4: Fix double closing parentheses: [text](path)))
    pattern4 = r'(\[[^\]]+\]\([^\)]+\))\)\)'
    content = re.sub(pattern4, r'\1', content)
    
    # Pattern 5: Fix links with mixed brackets: [text](path))]
    pattern5 = r'(\[[^\]]+\]\([^\)]+\))\)\]'
    content = re.sub(pattern5, r'\1', content)
    
    # Write back if changes were made
    if content != original_content:
        try:
            file_path.write_text(content, encoding='utf-8')
            return True
        except Exception as e:
            print(f"Error writing {file_path}: {e}")
            return False
    
    return False

def find_problematic_links(file_path: Path) -> list[str]:
    """Find lines with potentially problematic links."""
    try:
        content = file_path.read_text(encoding='utf-8')
    except:
        return []
    
    problems = []
    lines = content.split('\n')
    
    for i, line in enumerate(lines, 1):
        # Look for suspicious patterns
        if re.search(r'\]\([^\)]*\)\)', line):  # Extra closing parenthesis
            problems.append(f"Line {i}: {line.strip()}")
        elif re.search(r'\]\([^\)]*\)\]', line):  # Extra closing bracket
            problems.append(f"Line {i}: {line.strip()}")
        elif re.search(r'\[\[.*\]\]', line) and '[' in line and '](' in line:  # Double brackets with links
            problems.append(f"Line {i}: {line.strip()}")
    
    return problems

def main():
    """Find and fix all remaining link formatting issues."""
    root_path = Path('.').resolve()
    
    # Skip certain directories
    skip_dirs = {'.git', '.venv', 'node_modules', '__pycache__', 'target', 'reports'}
    
    md_files = []
    for path in root_path.rglob('*.md'):
        if any(skip_dir in path.parts for skip_dir in skip_dirs):
            continue
        md_files.append(path)
    
    md_files = sorted(md_files)
    
    print("Scanning for remaining link issues...")
    
    # First, scan for problems
    total_problems = 0
    for file_path in md_files:
        problems = find_problematic_links(file_path)
        if problems:
            rel_path = file_path.relative_to(root_path)
            print(f"\n📄 {rel_path}:")
            for problem in problems:
                print(f"  ⚠️  {problem}")
                total_problems += 1
    
    if total_problems == 0:
        print("✅ No link formatting issues found!")
        return
    
    print(f"\nFound {total_problems} potential issues. Fixing...")
    
    # Now fix the issues
    fixed_count = 0
    for file_path in md_files:
        if fix_final_link_issues(file_path):
            rel_path = file_path.relative_to(root_path)
            print(f"✓ Fixed issues in: {rel_path}")
            fixed_count += 1
    
    print(f"\n✅ Fixed issues in {fixed_count} files.")

if __name__ == "__main__":
    main()
