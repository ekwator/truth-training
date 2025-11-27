#!/usr/bin/env python3
"""Fix extra brackets around markdown links."""

import re
from pathlib import Path

def fix_extra_brackets_in_file(file_path: Path) -> bool:
    """Fix extra brackets around markdown links in a file."""
    try:
        content = file_path.read_text(encoding='utf-8')
    except UnicodeDecodeError:
        try:
            content = file_path.read_text(encoding='latin-1')
        except:
            print(f"Could not read file: {file_path}")
            return False
    
    original_content = content
    
    # Pattern: Fix [[link](path)] -> [link](path)
    pattern1 = r'\[\[([^\]]+)\]\(([^\)]+)\)\]'
    content = re.sub(pattern1, r'[\1](\2)', content)
    
    # Pattern: Fix [link](path))] -> [link](path)
    pattern2 = r'(\[[^\]]+\]\([^\)]+\))\]'
    content = re.sub(pattern2, r'\1', content)
    
    # Pattern: Fix [[link](path) -> [link](path)
    pattern3 = r'\[\[([^\]]+)\]\(([^\)]+)\)'
    content = re.sub(pattern3, r'[\1](\2)', content)
    
    # Write back if changes were made
    if content != original_content:
        try:
            file_path.write_text(content, encoding='utf-8')
            return True
        except Exception as e:
            print(f"Error writing {file_path}: {e}")
            return False
    
    return False

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

def main():
    """Fix all extra brackets around links in the repository."""
    root_path = Path('.').resolve()
    
    print("Finding all markdown files...")
    md_files = find_all_md_files(root_path)
    print(f"Found {len(md_files)} markdown files")
    
    print("\nFixing extra brackets around links...")
    fixed_count = 0
    
    for file_path in md_files:
        if fix_extra_brackets_in_file(file_path):
            rel_path = file_path.relative_to(root_path)
            print(f"✓ Fixed extra brackets in: {rel_path}")
            fixed_count += 1
    
    print(f"\n✓ Fixed extra brackets in {fixed_count} files out of {len(md_files)} total files.")

if __name__ == "__main__":
    main()
