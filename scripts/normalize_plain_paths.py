#!/usr/bin/env python3
"""
Normalize plain-path references to Markdown links.
Excludes CHANGELOG.md and .cursor/ files per refactoring scope.
"""
import re
import sys
from pathlib import Path

def normalize_plain_paths_in_file(file_path, fixes_for_file):
    """Normalize plain paths in a single file."""
    if not Path(file_path).exists():
        return False, f"File not found: {file_path}"
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            lines = content.split('\n')
    except Exception as e:
        return False, f"Error reading {file_path}: {e}"
    
    modified = False
    # Sort fixes by line number (descending) to avoid offset issues
    fixes_sorted = sorted(fixes_for_file, key=lambda x: x['line'] or 0, reverse=True)
    
    for fix in fixes_sorted:
        line_num = fix['line']
        if line_num is None:
            continue
        
        # Convert to 0-based index
        line_idx = line_num - 1
        if line_idx < 0 or line_idx >= len(lines):
            continue
        
        line = lines[line_idx]
        raw_text = fix['raw_text']
        target = fix['target']
        
        # Check if already a link
        if f'[{raw_text}]' in line or f'({target})' in line:
            continue
        
        # Replace plain path with Markdown link
        # Use word boundaries to avoid partial matches
        pattern = r'\b' + re.escape(raw_text) + r'\b'
        replacement = f'[{raw_text}]({target})'
        
        new_line = re.sub(pattern, replacement, line)
        if new_line != line:
            lines[line_idx] = new_line
            modified = True
    
    if modified:
        try:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write('\n'.join(lines))
            return True, f"Updated {file_path}"
        except Exception as e:
            return False, f"Error writing {file_path}: {e}"
    
    return False, f"No changes needed in {file_path}"

def main():
    plain_paths_file = "specs/001-documentation-refactoring-0-0/tmp/plain_paths_to_fix.md"
    
    # Parse plain paths
    fixes_by_file = {}
    excluded_files = {'CHANGELOG.md', 'CONTRIBUTING.md', 'LICENSE.txt', 'SECURITY.md'}
    
    with open(plain_paths_file, 'r') as f:
        lines = f.readlines()
        for i, line in enumerate(lines[5:], start=6):  # Skip header
            if '|' in line and line.strip():
                parts = [p.strip() for p in line.split('|')]
                if len(parts) >= 5:
                    source = parts[1].strip('`')
                    raw_text = parts[2].strip('`')
                    target = parts[3].strip('`')
                    line_num_str = parts[4].strip()
                    
                    # Skip excluded files and .cursor/
                    if source in excluded_files or source.startswith('.cursor/'):
                        continue
                    
                    # Skip invalid entries
                    if not raw_text or raw_text == '----------' or not target:
                        continue
                    
                    try:
                        line_num = int(line_num_str) if line_num_str.isdigit() else None
                    except:
                        line_num = None
                    
                    if source not in fixes_by_file:
                        fixes_by_file[source] = []
                    
                    fixes_by_file[source].append({
                        'raw_text': raw_text,
                        'target': target,
                        'line': line_num
                    })
    
    # Process each file
    results = []
    for file_path, fixes in fixes_by_file.items():
        success, message = normalize_plain_paths_in_file(file_path, fixes)
        results.append((file_path, success, message))
    
    # Report
    success_count = sum(1 for _, s, _ in results if s)
    print(f"Processed {len(results)} files, {success_count} modified")
    
    for file_path, success, message in results:
        if success:
            print(f"  ✓ {message}")
        else:
            print(f"  - {message}")

if __name__ == '__main__':
    main()

