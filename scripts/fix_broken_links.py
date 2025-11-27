#!/usr/bin/env python3
"""Fix broken nested markdown links in the repository."""

import re
from pathlib import Path

def fix_broken_links_in_file(file_path: Path) -> bool:
    """Fix broken nested markdown links in a file."""
    try:
        content = file_path.read_text(encoding='utf-8')
    except UnicodeDecodeError:
        try:
            content = file_path.read_text(encoding='latin-1')
        except:
            print(f"Could not read file: {file_path}")
            return False
    
    original_content = content
    
    # Pattern 1: Fix nested links like [text](d[ocs/file.md](ocs/file.md))
    # Should become [text](docs/file.md)
    pattern1 = r'\[([^\]]+)\]\(([a-zA-Z0-9_\-./]*)\[([^\]]+)\]\(([^\)]+)\)\)'
    def fix_nested_links(match):
        text = match.group(1)
        prefix = match.group(2)
        inner_text = match.group(3)
        inner_path = match.group(4)
        # Reconstruct the correct path
        full_path = prefix + inner_path
        return f'[{text}]({full_path})'
    
    content = re.sub(pattern1, fix_nested_links, content)
    
    # Pattern 2: Fix triple nested links like [[[text](path)](nested)](more_nested)
    # Extract the innermost valid path
    pattern2 = r'\[\[\[([^\]]+)\]\(([^\)]+)\)\]\([^\)]*\)\]\([^\)]*\)'
    def fix_triple_nested(match):
        text = match.group(1)
        path = match.group(2)
        return f'[{text}]({path})'
    
    content = re.sub(pattern2, fix_triple_nested, content)
    
    # Pattern 3: Fix double nested links like [[text](path)](nested)
    pattern3 = r'\[\[([^\]]+)\]\(([^\)]+)\)\]\([^\)]*\)'
    def fix_double_nested(match):
        text = match.group(1)
        path = match.group(2)
        return f'[{text}]({path})'
    
    content = re.sub(pattern3, fix_double_nested, content)
    
    # Pattern 4: Fix malformed links with extra brackets
    pattern4 = r'\[([^\]]+)\]\(([a-zA-Z0-9_\-./]+)\[([^\]]*)\]\(([^\)]*)\)\)'
    def fix_malformed(match):
        text = match.group(1)
        path_start = match.group(2)
        path_end = match.group(4)
        # Try to reconstruct the correct path
        if path_end.startswith(path_start[1:]):  # Remove first char overlap
            full_path = path_start + path_end[len(path_start)-1:]
        else:
            full_path = path_start + path_end
        return f'[{text}]({full_path})'
    
    content = re.sub(pattern4, fix_malformed, content)
    
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
    """Fix all broken nested links in the repository."""
    root_path = Path('.').resolve()
    
    print("Finding all markdown files...")
    md_files = find_all_md_files(root_path)
    print(f"Found {len(md_files)} markdown files")
    
    print("\nFixing broken nested links...")
    fixed_count = 0
    
    for file_path in md_files:
        if fix_broken_links_in_file(file_path):
            rel_path = file_path.relative_to(root_path)
            print(f"✓ Fixed broken links in: {rel_path}")
            fixed_count += 1
    
    print(f"\n✓ Fixed broken links in {fixed_count} files out of {len(md_files)} total files.")

if __name__ == "__main__":
    main()
