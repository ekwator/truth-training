# Quality Gates  
Version: v1.0.0  
Updated: 2025-11-26  
Spec ID: 14

## Required Quality Gates for Pull Requests

A Pull Request may be accepted for review only if all the following gates pass:

### 🔧 Linting
- `cargo fmt --all --check`  
- `cargo clippy --workspace --all-targets --all-features -- -D warnings`

### 🏗 Build
- `cargo check --all-targets --all-features` must succeed  
- No warnings permitted from Rust compiler

### 🧪 Tests
- All unit tests must pass across workspace  
- No test flakiness or intermittent failures  
- API signature verification tests must pass  
- Database/storage initialization tests must pass  

### 📄 Documentation
- PR must reference or update relevant Spec-Kit specification IDs  
- All modified APIs must include or update documentation comments  
- No broken links or unresolved references in markdown

### 🔒 Security Gates
- No new high-severity vulnerabilities in dependencies  
- No unsafe code blocks unless explicitly justified and approved  
- All cryptographic or signature-related code paths must include valid tests

Only PRs that fully satisfy these gates are eligible for maintainers’ review and potential merge.
