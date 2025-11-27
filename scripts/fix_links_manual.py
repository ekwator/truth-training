#!/usr/bin/env python3
"""Manual link fixing script for documentation refactoring."""

import re
from pathlib import Path

def fix_links_in_file(file_path: Path):
    """Fix plaintext .md links in a specific file."""
    if not file_path.exists():
        print(f"File not found: {file_path}")
        return
    
    content = file_path.read_text(encoding='utf-8')
    original_content = content
    
    # Pattern to match standalone .md paths (not already in links)
    # This matches paths like "spec/03-architecture.md" but not "[text](spec/03-architecture.md)"
    pattern = r'(?<!\]\()\b([A-Za-z0-9_\-./]+\.md)\b(?!\))'
    
    def replace_func(match):
        path = match.group(1)
        # Check if this path is already part of a markdown link in the surrounding context
        start = max(0, match.start() - 50)
        end = min(len(content), match.end() + 50)
        context = content[start:end]
        
        # If already in a link format, don't change
        if f"[{path}]({path})" in context or f"]({path})" in context:
            return path
        
        return f"[{path}]({path})"
    
    # Apply replacements
    new_content = re.sub(pattern, replace_func, content)
    
    # Only write if there were changes
    if new_content != original_content:
        file_path.write_text(new_content, encoding='utf-8')
        print(f"Fixed links in: {file_path}")
        return True
    else:
        print(f"No changes needed in: {file_path}")
        return False

def main():
    """Fix links in key documentation files."""
    root = Path(".")
    
    # Files that need link fixing
    files_to_fix = [
        "spec/17-offline-reliability.md",
        "spec/README.md",
        "spec/01-product-vision.md",
        "spec/02-requirements.md",
        "spec/03-architecture.md",
        "spec/04-data-model.md",
        "spec/05-api.md",
        "spec/06-expert-system.md",
        "spec/07-event-rating-protocol.md",
        "spec/08-p2p-sync.md",
        "spec/09-ux-guidelines.md",
        "spec/10-cli.md",
        "spec/11-decision-log.md",
        "spec/12-open-questions.md",
        "spec/13-traceability.md",
        "spec/14-quality-gates.md",
        "spec/15-prompts-and-automation.md",
        "spec/16-test-plan.md",
        "spec/18-cross-platform-architecture.md",
        "spec/19-build-instructions.md",
        "spec/20-cargo-configuration.md",
        "spec/21-roadmap.md"
    ]
    
    fixed_count = 0
    for file_path in files_to_fix:
        full_path = root / file_path
        if fix_links_in_file(full_path):
            fixed_count += 1
    
    print(f"\nFixed links in {fixed_count} files.")

if __name__ == "__main__":
    main()
