#!/bin/bash
# Link graph extractor for Markdown documentation
# Outputs JSON representation of all links between .md files

python3 << 'PYEOF'
import os
import re
import json
from pathlib import Path
from urllib.parse import unquote

repo_root = Path(".")
inventory_file = "specs/001-documentation-refactoring-0-0/tmp/all_markdown_files.txt"
output_file = "specs/001-documentation-refactoring-0-0/tmp/link_graph.json"

# Load all markdown files
with open(inventory_file, 'r') as f:
    all_files = {line.strip() for line in f if line.strip()}

# Normalize paths (remove leading ./)
def normalize_path(p):
    p = p.strip()
    if p.startswith("./"):
        p = p[2:]
    return p

# Check if path exists as .md file
def is_md_file(path_str, base_dir):
    path = Path(base_dir) / path_str
    if path.is_file() and path.suffix == '.md':
        return True
    # Try with .md extension
    if not path_str.endswith('.md'):
        path = path.with_suffix('.md')
        if path.is_file():
            return True
    return False

# Resolve relative path
def resolve_path(target, source_file):
    source_dir = Path(source_file).parent
    if target.startswith('/'):
        # Absolute from repo root
        target = target[1:]
    elif target.startswith('./'):
        target = target[2:]
    
    resolved = (source_dir / target).resolve()
    try:
        rel = resolved.relative_to(Path('.').resolve())
        return str(rel)
    except ValueError:
        return None

# Patterns for Markdown links and plain paths
markdown_link_pattern = re.compile(r'\[([^\]]+)\]\(([^\)]+)\)')
plain_path_pattern = re.compile(r'([a-zA-Z0-9_\-/]+\.md)')

graph = {
    "nodes": [],
    "edges": []
}

# Track all files as nodes
for file_path in sorted(all_files):
    normalized = normalize_path(file_path)
    graph["nodes"].append({
        "path": normalized,
        "exists": True
    })

# Process each file for links
for file_path in sorted(all_files):
    normalized_source = normalize_path(file_path)
    if not Path(file_path).is_file():
        continue
    
    try:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
            lines = content.split('\n')
    except Exception as e:
        continue
    
    # Find Markdown links
    for match in markdown_link_pattern.finditer(content):
        link_text = match.group(1)
        link_target = match.group(2)
        
        # Remove fragment/anchor
        if '#' in link_target:
            link_target = link_target.split('#')[0]
        
        # Skip external URLs
        if link_target.startswith(('http://', 'https://', 'mailto:', 'ftp://')):
            continue
        
        # Decode URL encoding
        link_target = unquote(link_target)
        
        # Resolve relative path
        resolved = resolve_path(link_target, file_path)
        if resolved:
            resolved = normalize_path(resolved)
            # Check if it's a .md file
            if resolved.endswith('.md') or is_md_file(resolved, Path(file_path).parent):
                if not resolved.endswith('.md'):
                    resolved += '.md'
                
                graph["edges"].append({
                    "source": normalized_source,
                    "target": resolved,
                    "raw_text": match.group(0),
                    "is_markdown_link": True,
                    "is_plain_path_candidate": False,
                    "line_number": content[:match.start()].count('\n') + 1
                })
    
    # Find plain path candidates (standalone .md references)
    for line_num, line in enumerate(lines, 1):
        # Skip code blocks
        if line.strip().startswith('```'):
            continue
        
        for match in plain_path_pattern.finditer(line):
            candidate = match.group(1)
            # Skip if already in a markdown link
            if f'[{candidate}]' in line or f'({candidate})' in line:
                continue
            
            # Resolve path
            resolved = resolve_path(candidate, file_path)
            if resolved:
                resolved = normalize_path(resolved)
                if resolved.endswith('.md') or is_md_file(resolved, Path(file_path).parent):
                    if not resolved.endswith('.md'):
                        resolved += '.md'
                    
                    # Check if this link already exists
                    existing = any(
                        e["source"] == normalized_source and 
                        e["target"] == resolved and
                        e["line_number"] == line_num
                        for e in graph["edges"]
                    )
                    
                    if not existing:
                        graph["edges"].append({
                            "source": normalized_source,
                            "target": resolved,
                            "raw_text": candidate,
                            "is_markdown_link": False,
                            "is_plain_path_candidate": True,
                            "line_number": line_num
                        })

# Write output
with open(output_file, 'w') as f:
    json.dump(graph, f, indent=2)

print(f"Link graph extracted: {len(graph['nodes'])} nodes, {len(graph['edges'])} edges")
print(f"Output: {output_file}")
PYEOF

