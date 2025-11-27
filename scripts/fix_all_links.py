#!/usr/bin/env python3
"""Fix all plaintext .md links in the repository."""

import re
from pathlib import Path

def find_all_md_files(root_path: Path) -> list[Path]:
    """Find all markdown files in the repository."""
    md_files = []
    
    # Skip certain directories
    skip_dirs = {'.git', '.venv', 'node_modules', '__pycache__', 'target', 'reports'}
    
    for path in root_path.rglob('*.md'):
        # Skip if in excluded directory
        if any(skip_dir in path.parts for skip_dir in skip_dirs):
            continue
        md_files.append(path)
    
    return sorted(md_files)

def fix_links_in_file(file_path: Path, root_path: Path) -> bool:
    """Fix plaintext .md links in a file."""
    try:
        content = file_path.read_text(encoding='utf-8')
    except UnicodeDecodeError:
        try:
            content = file_path.read_text(encoding='latin-1')
        except:
            print(f"Could not read file: {file_path}")
            return False
    
    original_content = content
    
    # Find all plaintext .md references that are NOT already markdown links
    # Pattern explanation:
    # - (?<!\]\() : not preceded by ](
    # - (?<!\[.*) : not preceded by [ and any characters
    # - \b : word boundary
    # - ([A-Za-z0-9_\-./]+\.md) : capture the .md path
    # - \b : word boundary  
    # - (?!\)) : not followed by )
    
    lines = content.split('\n')
    new_lines = []
    changes_made = False
    
    for line in lines:
        original_line = line
        
        # Skip lines that already have markdown links
        if '[' in line and '](' in line and '.md)' in line:
            new_lines.append(line)
            continue
        
        # Find standalone .md paths
        md_pattern = r'\b([A-Za-z0-9_\-./]+\.md)\b'
        matches = list(re.finditer(md_pattern, line))
        
        # Process matches in reverse order to maintain positions
        for match in reversed(matches):
            path = match.group(1)
            start, end = match.span()
            
            # Check if this is already part of a link
            if start > 0 and line[start-1] == '(':
                continue  # Skip if it's already in a link
            
            # Replace with markdown link
            line = line[:start] + f'[{path}]({path})' + line[end:]
            changes_made = True
        
        new_lines.append(line)
    
    if changes_made:
        new_content = '\n'.join(new_lines)
        try:
            file_path.write_text(new_content, encoding='utf-8')
            rel_path = file_path.relative_to(root_path)
            print(f"✓ Fixed links in: {rel_path}")
            return True
        except Exception as e:
            print(f"✗ Error writing {file_path}: {e}")
            return False
    
    return False

def main():
    """Fix all markdown links in the repository."""
    root_path = Path('.').resolve()
    
    print("Finding all markdown files...")
    md_files = find_all_md_files(root_path)
    print(f"Found {len(md_files)} markdown files")
    
    print("\nFixing plaintext .md links...")
    fixed_count = 0
    
    for file_path in md_files:
        if fix_links_in_file(file_path, root_path):
            fixed_count += 1
    
    print(f"\n✓ Fixed links in {fixed_count} files out of {len(md_files)} total files.")
    
    if fixed_count > 0:
        print("\nRun 'git diff' to see the changes.")

if __name__ == "__main__":
    main()
