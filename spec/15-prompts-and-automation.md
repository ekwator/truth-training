# Prompts & Automation

Use /spec as the primary decision source before reading /docs.
Version: v1.0.0
Updated: 2025-01-XX
Spec ID: 15

## Required Tools (v1.0.0)

### Cursor AI IDE (Mandatory)
- **Required for all contributions**: All changes must be developed using Cursor AI IDE
- **Features**: Inline type checking, automated refactoring, integrated AI assistant, Spec-Kit prompt preparation
- **Integration**: Cursor AI IDE automatically applies changes ensuring correct formatting, type safety, and spec compliance

### Spec-Kit (Mandatory)
- **Location**: Installed in `.cursor/` directory
- **Repository**: [GitHub Spec-Kit](https://github.com/github/spec-kit)
- **Purpose**: Spec-driven development workflow ensuring all work is planned, validated, and implemented consistently

## Spec-Kit Workflow (Required for All PRs)

All contributions must follow this sequence:

1. **`/speckit.specify`** — Define the task
   - Output: `docs/prompt/<author>/specifyN/specN.md`
   - Describes: goal, constraints, affected modules, acceptance criteria, security implications

2. **`/speckit.plan`** — Create implementation plan
   - Output: `docs/prompt/<author>/specifyN/plan/planN.md`
   - Describes: implementation steps, data structures, API touchpoints, testing strategy, risks

3. **`/speckit.task`** — Generate atomic developer tasks
   - Converts plan into small, testable, reversible tasks

4. **`/speckit.clarify`** — Resolve ambiguities (if needed)

5. **`/speckit.implementation`** — Apply changes
   - Cursor AI IDE applies changes automatically or semi-automatically

**⚠️ PRs not backed by a Spec-Kit workflow will be closed without review.**

## Automation & Prompts

- **PR Templates**: Must include Spec-Kit chain links (`/speckit.specify`, `/speckit.plan`, `/speckit.task` or `/speckit.implementation`)
- **Issue Templates**: Capture acceptance criteria tied to Spec-Kit specifications
- **LLM Workflows**: After `/speckit.specify`, attach [spec/README.md](README.md); use prompts to generate endpoint skeletons and tests from [spec/05-api.md](05-api.md)
- **Cursor/Agents**: Use [spec/13-traceability.md](13-traceability.md) to focus searches and edits
- **Spec-Kit Prompt Library**: Examples in `docs/prompt/<author>/` serve as templates for contributors

See [CONTRIBUTING.md](../CONTRIBUTING.md) for detailed workflow requirements.

_Version: v1.0.0_

- See [docs/README.md](../docs/README.md) for detailed explanations.

- See [spec/README.md](README.md) for detailed explanations.
