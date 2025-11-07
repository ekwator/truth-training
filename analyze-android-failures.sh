#!/usr/bin/env bash
# analyze-android-failures.sh
# Scans Android test artifacts directory and extracts failed tests, warnings and log snippets.
# Produces analysis into OUT_DIR and a Cursor AI prompt at cursor-prompt.md.
#
# Usage:
#   ./analyze-android-failures.sh
#   ART_DIR="test-results/" OUT_DIR="analysis/" ./analyze-android-failures.sh
#
set -euo pipefail

# Default paths for local usage
ART_DIR="${ART_DIR:-test-results/}"
OUT_DIR="${OUT_DIR:-analysis/}"

# Normalize paths (remove trailing slashes)
ART_DIR="${ART_DIR%/}"
OUT_DIR="${OUT_DIR%/}"

mkdir -p "${OUT_DIR}"

# Files
FAIL_SUM="${OUT_DIR}/failures-summary.txt"
WARN_SUM="${OUT_DIR}/warnings-summary.txt"
TOP_ERR="${OUT_DIR}/top-errors.txt"
FLAVOR_STATS="${OUT_DIR}/flavor-statistics.txt"
CURSOR_PROMPT="${OUT_DIR}/cursor-prompt.md"

# Helpers
ech() { printf '%s\n' "$*"; }
sep() { printf '\n==== %s ====\n\n' "$1"; }

# Clear old
: > "${FAIL_SUM}"
: > "${WARN_SUM}"
: > "${TOP_ERR}"
: > "${FLAVOR_STATS}"
: > "${CURSOR_PROMPT}"

ech "Starting analysis of test artifacts in: ${ART_DIR}"
ech "Output -> ${OUT_DIR}"
sep "Found TEST XML files"

# Find all TEST-*.xml and group by flavor
mapfile -t TEST_XMLS < <(find "${ART_DIR}" -type f -iname 'TEST-*.xml' 2>/dev/null | sort || true)

if [ "${#TEST_XMLS[@]}" -eq 0 ]; then
  ech "No TEST-*.xml files found under ${ART_DIR}. Still will scan logs for errors/warnings."
else
  ech "Found ${#TEST_XMLS[@]} TEST XML files."
fi

# Statistics per flavor
declare -A FLAVOR_FAIL_COUNT
declare -A FLAVOR_TEST_COUNT
declare -A FLAVOR_TOTAL_TESTS

fail_count=0
# Parse TEST XMLs: extract testcase with <failure> or <error>
for f in "${TEST_XMLS[@]}"; do
  # Extract flavor from path (e.g., outputs/local/... -> local)
  flavor="unknown"
  if [[ "$f" =~ /(local|mock|remote)/ ]]; then
    flavor="${BASH_REMATCH[1]}"
  fi
  
  ech "-> $f (flavor: ${flavor})"
  
  # Count total testcases in this XML
  total_tests=$(grep -o '<testcase' "${f}" 2>/dev/null | wc -l | tr -d ' \n' || echo "0")
  if [ -z "${total_tests}" ] || ! [[ "${total_tests}" =~ ^[0-9]+$ ]]; then
    total_tests=0
  fi
  prev_total="${FLAVOR_TOTAL_TESTS["${flavor}"]:-0}"
  FLAVOR_TOTAL_TESTS["${flavor}"]=$((prev_total + total_tests))
  prev_count="${FLAVOR_TEST_COUNT["${flavor}"]:-0}"
  FLAVOR_TEST_COUNT["${flavor}"]=$((prev_count + 1))
  
  # Count failures/errors in this xml
  failures_here=$(grep -oiE '<failure|<error' "${f}" | wc -l | tr -d ' \n' || echo "0")
  if [ -z "${failures_here}" ] || ! [[ "${failures_here}" =~ ^[0-9]+$ ]]; then
    failures_here=0
  fi
  prev_fails="${FLAVOR_FAIL_COUNT["${flavor}"]:-0}"
  FLAVOR_FAIL_COUNT["${flavor}"]=$((prev_fails + failures_here))
  
  if [ "${failures_here}" -gt 0 ]; then
    ech "  failures: ${failures_here}" >> "${FAIL_SUM}"
    ech "File: ${f} (flavor: ${flavor})" >> "${FAIL_SUM}"
    # Extract testcase entries and their failure text (works without xmllint)
    # We capture testcase start to end and then print classname/name and first lines of failure
    awk '
      BEGIN{RS="</testcase>"; ORS=""; }
      /<testcase/ {
        if ($0 ~ /<failure/ || $0 ~ /<error/) {
          # extract classname and name
          match($0, /classname="[^"]+"/); cls=substr($0,RSTART+11,RLENGTH-12)
          match($0, /name="[^"]+"/); nm=substr($0,RSTART+6,RLENGTH-6)
          print "  - test: " nm " (" cls ")\n"
          # extract failure block content (between <failure ...> and </failure>)
          match($0, /<failure[^>]*>([^<]*)/);
          if (RSTART) {
            s=substr($0,RSTART+RLENGTH-1,2000)
            gsub(/\r/,"",s)
            print "    failure: " substr(s,1,1000) "\n"
          } else {
            # fallback: print first 200 chars of block
            print "    failure: (see file)\n"
          }
          print "\n"
        }
      }
    ' "${f}" >> "${FAIL_SUM}" || true

    # Also append whole <failure> sections to separate file for deeper inspection
    # Extract with sed: between <testcase ...> and </testcase> that contains <failure>
    awk 'BEGIN{RS="</testcase>";ORS="\n\n----\n\n"} /<testcase/ && /<failure|<error/ {print $0}' "${f}" >> "${OUT_DIR}/raw-failures.log" || true
    fail_count=$((fail_count+failures_here))
  fi
done

# Write flavor statistics
sep "Flavor Statistics"
{
  echo "Test statistics per flavor:"
  echo ""
  for flavor in local mock remote; do
    total_tests="${FLAVOR_TOTAL_TESTS["${flavor}"]:-0}"
    failures="${FLAVOR_FAIL_COUNT["${flavor}"]:-0}"
    test_files="${FLAVOR_TEST_COUNT["${flavor}"]:-0}"
    if [ "${test_files}" -gt 0 ]; then
      success=$((total_tests - failures))
      success_pct=0
      if [ "${total_tests}" -gt 0 ]; then
        success_pct=$((success * 100 / total_tests))
      fi
      echo "Flavor: ${flavor}"
      echo "  Test files: ${test_files}"
      echo "  Total tests: ${total_tests}"
      echo "  Passed: ${success} (${success_pct}%)"
      echo "  Failed: ${failures}"
      echo ""
    fi
  done
} > "${FLAVOR_STATS}"
cat "${FLAVOR_STATS}"

sep "Scanning gradle-build-*.log and logcat/utp logs for errors & warnings"

# Find and process all gradle-build-*.log files
mapfile -t GRADLE_LOGS < <(find "${ART_DIR}/logs" -type f -iname 'gradle-build-*.log' 2>/dev/null | sort || true)

if [ "${#GRADLE_LOGS[@]}" -eq 0 ]; then
  # Fallback: try current directory
  mapfile -t GRADLE_LOGS < <(find . -maxdepth 1 -type f -iname 'gradle-build*.log' 2>/dev/null | sort || true)
fi

if [ "${#GRADLE_LOGS[@]}" -eq 0 ]; then
  ech "No gradle-build-*.log files found (searched: ${ART_DIR}/logs/gradle-build-*.log, ./gradle-build*.log)"
else
  ech "Found ${#GRADLE_LOGS[@]} Gradle log files:"
  for gl in "${GRADLE_LOGS[@]}"; do
    ech "  - ${gl}"
    ech "Top ERROR/Exception lines from ${gl}:" >> "${TOP_ERR}"
    ech "=== ${gl} ===" >> "${TOP_ERR}"
    grep -I -E '(^Caused by: |Exception|ERROR|FAILURE|FATAL)' "${gl}" 2>/dev/null | sed -E 's/^[[:space:]]+//' | awk 'length($0)>0' | sed -n '1,200p' >> "${TOP_ERR}" || true
    ech "" >> "${TOP_ERR}"
  done
fi

# Search logcat and utp logs
mapfile -t LOGFILES < <(find "${ART_DIR}" -type f \( -iname 'logcat*.txt' -o -iname 'utp.*.log' -o -iname '*.log' -o -iname 'testlog' \) 2>/dev/null || true)
ech "Found ${#LOGFILES[@]} log files to scan (logcat/utp/testlog/...)"

# Aggregate top error/warning messages across logs
declare -A ERRCOUNT
declare -A WARNCOUNT

# Process all gradle logs
for gl in "${GRADLE_LOGS[@]}"; do
  filesize=$(wc -c < "${gl}" || echo 0)
  if [ "${filesize}" -gt $((1024*1024*5)) ]; then
    tmp="${OUT_DIR}/sample-$(basename "${gl}").log"
    ( head -n 2000 "${gl}"; echo -e "\n...SKIP...\n"; tail -n 2000 "${gl}" ) > "${tmp}" || true
    scanfile="${tmp}"
  else
    scanfile="${gl}"
  fi

  # find ERROR/Exception/FATAL lines
  while IFS= read -r line; do
    key=$(printf '%s' "$line" | sed -E 's/^[[:space:]]+//' | sed -E 's/(:[[:space:]].*)$//' | cut -c1-200)
    key="${key:-<empty>}"
    ERRCOUNT["$key"]=$((ERRCOUNT["$key"]+1))
  done < <(grep -I -E 'Exception|ERROR|FATAL|Caused by:' "${scanfile}" 2>/dev/null || true)

  # find WARNING lines
  while IFS= read -r line; do
    key=$(printf '%s' "$line" | sed -E 's/^[[:space:]]+//' | cut -c1-240)
    WARNCOUNT["$key"]=$((WARNCOUNT["$key"]+1))
  done < <(grep -I -E 'WARN|WARNING' "${scanfile}" 2>/dev/null || true)
done

# Process other log files (logcat, utp, etc.)
for lf in "${LOGFILES[@]}"; do
  # Skip gradle logs as they're already processed
  if [[ "$lf" =~ gradle-build ]]; then
    continue
  fi
  
  # Reduce huge files by sampling first/last 2000 lines if huge
  filesize=$(wc -c < "${lf}" || echo 0)
  if [ "${filesize}" -gt $((1024*1024*5)) ]; then
    # create temp sampled file
    tmp="${OUT_DIR}/sample-$(basename "${lf}").log"
    ( head -n 2000 "${lf}"; echo -e "\n...SKIP...\n"; tail -n 2000 "${lf}" ) > "${tmp}" || true
    scanfile="${tmp}"
  else
    scanfile="${lf}"
  fi

  # find ERROR/Exception/FATAL lines
  while IFS= read -r line; do
    # normalize common exception message beginning up to ":" or at end of line
    key=$(printf '%s' "$line" | sed -E 's/^[[:space:]]+//' | sed -E 's/(:[[:space:]].*)$//' | cut -c1-200)
    key="${key:-<empty>}"
    ERRCOUNT["$key"]=$((${ERRCOUNT["$key"]:-0} + 1))
  done < <(grep -I -E 'Exception|ERROR|FATAL|Caused by:' "${scanfile}" 2>/dev/null || true)

  # find WARNING lines
  while IFS= read -r line; do
    key=$(printf '%s' "$line" | sed -E 's/^[[:space:]]+//' | cut -c1-240)
    WARNCOUNT["$key"]=$((${WARNCOUNT["$key"]:-0} + 1))
  done < <(grep -I -E 'WARN|WARNING' "${scanfile}" 2>/dev/null || true)
done

# Write top errors
ech "Top error patterns (sample) -> ${TOP_ERR}"
{
  echo "Top error/exception patterns across logs (count:pattern). Sample lines follow."
  echo
  for k in "${!ERRCOUNT[@]}"; do
    echo "${ERRCOUNT[$k]}|${k}"
  done | sort -rn -t'|' -k1 | head -n 60
} >> "${TOP_ERR}"

# Warnings summary
{
  echo "Top warnings (count:message)"
  echo
  for k in "${!WARNCOUNT[@]}"; do
    echo "${WARNCOUNT[$k]}|${k}"
  done | sort -rn -t'|' -k1 | head -n 60
} >> "${WARN_SUM}"

# Quick stats
total_tests_found=${#TEST_XMLS[@]}
ech ""
sep "SUMMARY"
ech "Detected test xml files: ${total_tests_found}"
ech "Detected failed assertions (approx): ${fail_count}"
ech "Gradle log files processed: ${#GRADLE_LOGS[@]}"
ech "Other log files processed: ${#LOGFILES[@]}"
ech "Analysis written to: ${OUT_DIR}"
ech ""

# Build cursor prompt
sep "Generating Cursor AI prompt -> ${CURSOR_PROMPT}"

# Build list of gradle logs found
GRADLE_LOG_LIST=""
if [ "${#GRADLE_LOGS[@]}" -gt 0 ]; then
  GRADLE_LOG_LIST=$(printf '%s\n' "${GRADLE_LOGS[@]}" | tr '\n' ',' | sed 's/,$//')
else
  GRADLE_LOG_LIST="none found"
fi

# Read flavor statistics into variable
FLAVOR_STATS_CONTENT=""
if [ -f "${FLAVOR_STATS}" ]; then
  FLAVOR_STATS_CONTENT=$(cat "${FLAVOR_STATS}")
fi

# Get repository name
REPO_NAME=$(basename "$(pwd)")

{
  cat <<MD
# Cursor AI prompt: fix Android instrumented tests & CI (auto-generated)
Repository: ${REPO_NAME}
Workflow: android-build.yml (emulator-runner)
Artifacts dir: ${ART_DIR}
Analysis dir: ${OUT_DIR}

## TL;DR
There are **${fail_count}** test failures across flavors (see failures-summary.txt) and many log entries (see top-errors.txt and warnings-summary.txt). We need Cursor AI to:
1. Inspect the failed tests and their stack traces
2. Propose and apply fixes (test stability, coroutine usage, Espresso waits)
3. Ensure CI workflow collects per-flavor reports reliably (avoid overwriting outputs between flavors), enable Gradle build cache, and ensure gradle output is captured to gradle-build-<flavor>.log for debugging.

## Test Statistics by Flavor
${FLAVOR_STATS_CONTENT}

## Files to inspect (priority)
- Failed test details: \`${OUT_DIR}/failures-summary.txt\`
- Raw failure dumps: \`${OUT_DIR}/raw-failures.log\`
- Flavor statistics: \`${OUT_DIR}/flavor-statistics.txt\`
- Gradle logs: ${GRADLE_LOG_LIST}
- Collected logs: \`${OUT_DIR}/top-errors.txt\`, \`${OUT_DIR}/warnings-summary.txt\`
- Per-flavor report dirs: \`${ART_DIR}/outputs/*\` and \`${ART_DIR}/reports/*\`
- Workflow file: \`.github/workflows/android-build.yml\` (emulator step / script generation)

## Observed failure examples (auto-extract samples)
<EXTRACTED_FAILURES_SNIPPET>

## Suggested fixes (high level)
- **Tests**:
  - Convert blocking uses to \`runTest\` from \`kotlinx.coroutines.test\` where appropriate.
  - Wrap suspend DAO calls in proper coroutine contexts or \`database.runInTransaction\` with \`runBlocking\` only if absolutely necessary; prefer \`runTest\`.
  - Add warm-up runs for DB/Room performance tests to avoid first-run variability.
  - Add small \`Thread.sleep(500)\` or \`IdlingResource\` waits where Espresso interacts with Activity startup to avoid focus/race flakiness.
  - Ensure instrumentation tests use \`ActivityScenario.launch()\` before interactions.
  - For performance tests, measure median instead of mean and run multiple warm-ups.

- **CI / workflow**:
  - Ensure gradle output is fully redirected to per-flavor \`gradle-build-<flavor>.log\` files:
    \`cd truth-android-client && ./gradlew connectedLocalDebugAndroidTest ... > ../gradle-build-local.log 2>&1\`
    Each flavor (local, mock, remote) and performance tests should have separate log files.
  - Enable Gradle build cache for performance: pass \`--build-cache\` to gradle invocations and ensure \`~/.gradle\` is cached with \`actions/cache\`.
  - After each flavor run, **move/copy** per-flavor reports into \`\${ART_DIR}\` and **clear** \`truth-android-client/app/build/outputs/androidTest-results\` and \`.../reports/androidTests/connected\` to avoid subsequent flavors overwriting previous reports.
  - Keep \`adb logcat -d\` executed **before** emulator kill.
  - Use an explicit \`shell: bash\` on steps that use advanced shell features.
  - Use a generated script (e.g., \`run_tests.sh\`) as the single entrypoint (already present) and call it via \`script: ./run_tests.sh\` in emulator action.

## Concrete PR tasks for Cursor AI (apply via single PR)
1. Tests: update identified failing test files (list below) to use \`runTest\`, add warm-ups and small waits, and fix coroutine usage.
   - Files: __FAILED_TEST_FILES__
   - Example change: change \`runBlocking { ... }\` -> \`runTest { ... }\`, make DAO operations suspend inside \`runTest\`.
2. CI: update \`.github/workflows/android-build.yml\`:
   - Ensure gradle invocations use \`--build-cache\` and redirect stdout/stderr to per-flavor \`gradle-build-<flavor>.log\`.
   - After each flavor run, copy per-flavor reports into \`\${{ env.ART_DIR }}/outputs/<flavor>\` and then \`rm -rf\` the build outputs in the project so next flavor does not overwrite.
   - Ensure \`adb logcat -d\` is executed (and saved) before emulator termination.
   - Set \`shell: bash\` for the step generating and running scripts.
3. Add the new script \`./.github/scripts/analyze-android-failures.sh\` (this file) and a \`README\` entry describing how to use analysis artifacts.

## Priority (1..3)
1. Capture full gradle logs per-flavor and persist.
2. Fix tests that fail consistently (see failures-summary).
3. Improve CI behavior to preserve per-flavor reports and enable build-cache.

---

Please open the files listed above, inspect the traces from \`${OUT_DIR}/raw-failures.log\` and \`${OUT_DIR}/failures-summary.txt\`, and create a PR implementing the three groups of changes. If you need, run the CI locally with the same emulator options to reproduce.

MD
} > "${CURSOR_PROMPT}"

# Inject dynamic snippets and counts
# Add failure snippet (first N lines)
if [ -s "${FAIL_SUM}" ]; then
  ech "" >> "${CURSOR_PROMPT}"
  ech "## Auto-extracted failures (first 80 lines):" >> "${CURSOR_PROMPT}"
  echo '```' >> "${CURSOR_PROMPT}"
  head -n 80 "${FAIL_SUM}" >> "${CURSOR_PROMPT}" || true
  echo '```' >> "${CURSOR_PROMPT}"
else
  ech "No failures found to include in the prompt." >> "${CURSOR_PROMPT}"
fi

# Attach top errors sample
if [ -s "${TOP_ERR}" ]; then
  ech "" >> "${CURSOR_PROMPT}"
  ech "## Top error patterns (sample):" >> "${CURSOR_PROMPT}"
  echo '```' >> "${CURSOR_PROMPT}"
  head -n 120 "${TOP_ERR}" >> "${CURSOR_PROMPT}" || true
  echo '```' >> "${CURSOR_PROMPT}"
fi

# Add list of candidate failed test files
ech "" >> "${CURSOR_PROMPT}"
ech "## Candidate files with failing tests (paths):" >> "${CURSOR_PROMPT}"
if [ -f "${OUT_DIR}/raw-failures.log" ]; then
  # try to extract classname or filename hints
  grep -oE 'File: .*' "${FAIL_SUM}" | sed 's/^  File: //' | sort -u >> "${CURSOR_PROMPT}" || true
else
  ech "(no raw failure file captured)" >> "${CURSOR_PROMPT}"
fi

# footer with where to find outputs
ech "" >> "${CURSOR_PROMPT}"
ech "## Artifacts produced by this analysis:" >> "${CURSOR_PROMPT}"
echo "- ${FAIL_SUM}" >> "${CURSOR_PROMPT}"
echo "- ${TOP_ERR}" >> "${CURSOR_PROMPT}"
echo "- ${WARN_SUM}" >> "${CURSOR_PROMPT}"
echo "- ${FLAVOR_STATS}" >> "${CURSOR_PROMPT}"
echo "- raw failures: ${OUT_DIR}/raw-failures.log" >> "${CURSOR_PROMPT}"
echo "" >> "${CURSOR_PROMPT}"
echo "----" >> "${CURSOR_PROMPT}"
echo "Generated at: $(date -u +"%Y-%m-%d %H:%M:%SZ")" >> "${CURSOR_PROMPT}"

ech "Analysis complete."
ech "Summary files:"
ech " - ${FAIL_SUM}"
ech " - ${WARN_SUM}"
ech " - ${TOP_ERR}"
ech " - ${FLAVOR_STATS}"
ech " - ${CURSOR_PROMPT}"
ech ""
ech "To run locally: ./analyze-android-failures.sh"
ech "To override paths: ART_DIR=\"test-results/\" OUT_DIR=\"analysis/\" ./analyze-android-failures.sh"
ech ""
