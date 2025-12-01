<!-- Archived from [docs/prompt/ekwator/specify2/implement.md](docs/prompt/ekwator/specify2/implement.md) -->

/speckit.implement

You must execute the implementation tasks automatically, without asking clarifying questions.  
If the current plan or tasks are incomplete or conflicting, update them yourself in compliance with the Specification.

Your objective is to fully implement the documentation refactor workflow described in the updated spec:

- Complete Phases 1, 2 and 3.
- Create the full Markdown inventory across the repository.
- Build the automated link-integrity traversal starting from [README.md](README.md).
- Fix, normalize or create missing .md files.
- Establish correct directory structure (/docs, /spec).
- Migrate detailed content into /docs and keep summaries in root-level files.
- Ensure cross-file referential integrity.
- Update the final documents to comply with v1.0.0.
- Produce scripts and automated tests for link integrity and hierarchy checks.

Instructions:

1. **Do not ask questions.**  
   If something is ambiguous, resolve it yourself using engineering judgment and the spec.

2. **If the current plan or tasks are insufficient**, correct them automatically:
   - Add missing tasks.
   - Remove redundant ones.
   - Rewrite the plan to match the Specification precisely.
   - Ensure each task is actionable and fully implementable.

3. **Then execute** normally under /speckit.implement:
   - Modify files.
   - Create new files where required.
   - Update documentation.
   - Add automation scripts under scripts/doc_refactor/.
   - Add tests (script + markdown-level tests).
   - Ensure CI integration if needed.

4. When performing changes:
   - Make all updates directly to the repository.
   - Maintain atomic commits per task.
   - Avoid asking for confirmation.

5. Deliverables required:
   - Updated /docs and /spec trees.
   - A fully linked, hierarchical documentation system.
   - A working traversal + integrity-checking script.
   - A corrected [README.md](README.md) with minimal summary.
   - All missing referenced .md files created.
   - Removed all dead links.
   - All references converted to clickable Markdown links.
   - Tests for documentation consistency.

You must complete the entire implementation flow automatically.  
Begin now and continue until all work is finished.

