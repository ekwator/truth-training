<!-- Archived from [docs/prompt/ekwator/specify2/implement1.md](docs/prompt/ekwator/specify2/implement1.md) -->

/speckit.implement

Execute the entire implementation plan fully and autonomously.

Important:
• Do NOT ask clarifying questions.
• If uncertainties exist, resolve them yourself using the specification, constitution, existing code, and repository structure.
• Apply the plan exactly as defined in the current /speckit.specify output. If the plan is insufficient, update the plan AND regenerate tasks before proceeding.
• Then execute all tasks end-to-end without requiring user confirmation.

Required execution flow:

1) Validate that the current plan is complete, actionable, and consistent with the v1.0.0 Documentation Refactor specification.
   - If the plan lacks required phases or details → automatically update the plan.
   - If any task is missing or ambiguous → regenerate the full task list.

2) Execute the finalized tasks sequentially and fully:
   - Implement Phase 1 (Inventory & Audit)
   - Implement Phase 2 (Link Integrity & Graph Build)
   - Implement Phase 3 (Restructuring & Migration of Content)
   - Implement all supporting automation (scripts, helpers)
   - Implement tests for every module created
   - Update documentation in /docs and /spec according to the spec
   - Update [README.md](README.md) summaries and cross-links

3) Ensure the implementation is fully integrated:
   - All code added is idiomatic, tested, and runnable
   - CLI tooling for the refactor pipeline is complete
   - CI workflow for the documentation refactor is created or updated
   - All markdown references resolve to correct relative paths
   - No dangling or broken links remain
   - No unreferenced files are left without an index entry

4) Produce the final result:
   - Commit all changes
   - Open a complete Pull Request with:
     • Description of all changes
     • Summary of the pipeline implementation
     • Notes on any improvements made to the plan/tasks
     • Confirmation that all phases from the spec are implemented
     • Confirmation that tests and CI succeed

Do not pause for confirmation between phases.
Do not request user decisions.
If something is unclear, resolve it autonomously and proceed.

Begin now.