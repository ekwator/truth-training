#!/usr/bin/env bash
# ===============================================
# 📋 Android Test Failure Analyzer
# -----------------------------------------------
# Scans Android test-results and logcats for:
#  - Failures
#  - Exceptions
#  - Warnings
# Produces readable summary + prompt for Cursor AI
# ===============================================

set -euo pipefail
BASE_DIR="${1:-.}"
OUT_FILE="android-test-summary.txt"

echo "🧪 Analyzing Android test results in: ${BASE_DIR}"
echo "==============================================" > "$OUT_FILE"

# --- Helper function to format sections ---
print_section() {
  echo -e "\n\n## $1" | tee -a "$OUT_FILE"
  echo "----------------------------------------------" | tee -a "$OUT_FILE"
}

# --- 1. JUnit XML failures ---
print_section "JUnit Failures (from XML)"
grep -R "<failure" "$BASE_DIR" --include="TEST-*.xml" -n 2>/dev/null | \
  sed 's/^/❌ /' | tee -a "$OUT_FILE" || echo "✅ No failures found" | tee -a "$OUT_FILE"

# --- 2. Exceptions in logcat files ---
print_section "Exceptions (from logcat)"
grep -R -E "Exception|AssertionFailed|Error:" "$BASE_DIR" \
  --include="logcat-*.txt" -A3 -B1 2>/dev/null | \
  sed 's/^/🔥 /' | tee -a "$OUT_FILE" || echo "✅ No exceptions found" | tee -a "$OUT_FILE"

# --- 3. Warnings and potential issues ---
print_section "Warnings"
grep -R -E "WARN|Deprecated|timeout|slow|Skipped" "$BASE_DIR" \
  --include="logcat-*.txt" -A1 -B1 2>/dev/null | \
  sed 's/^/⚠️ /' | tee -a "$OUT_FILE" || echo "✅ No warnings found" | tee -a "$OUT_FILE"

# --- 4. Summarize found test names ---
print_section "Failed Test Names"
grep -R -E "<testcase|Test" "$BASE_DIR" \
  | grep -E "failure|Exception|Assertion" -B1 2>/dev/null | \
  grep -E "name=" | \
  sed -E 's/.*name="([^"]+)".*/• \1/' | sort -u | tee -a "$OUT_FILE" || echo "✅ No failed tests" | tee -a "$OUT_FILE"

# --- 5. Create prompt for Cursor AI ---
print_section "Cursor AI Prompt (auto-generated)"
cat <<'EOF' | tee -a "$OUT_FILE"
# 🧠 Cursor AI — Android Test Fix Suggestions
Analyze the test failures and warnings below and generate targeted fixes.

## Context:
This is an Android instrumentation test report containing both data-layer (Room DB, Repository) and integration tests (Sync, ContextTemplate, CrossPlatform).

## Task:
1. Identify root causes of failures/exceptions.
2. Suggest code-level fixes or test adjustments.
3. For performance warnings, propose optimizations or caching improvements.
4. Output structured PR suggestions.

EOF

echo -e "\n✅ Analysis complete. Results saved to: ${OUT_FILE}"
