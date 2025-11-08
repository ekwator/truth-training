#!/usr/bin/env bash
# analyze-android-failures.sh
# Scans Android test artifacts directory and extracts failed tests, warnings and log snippets.
# Produces comprehensive analysis into OUT_DIR and a self-contained Cursor AI prompt at cursor-prompt.md.
#
# Merged from old and new versions to combine:
# - Sophisticated XML parsing and flavor statistics (new)
# - Context lines around exceptions/warnings (old)
# - Failed test name extraction (old)
# - Comprehensive logcat sampling (merged)
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
FAILED_TEST_NAMES="${OUT_DIR}/failed-test-names.txt"
LOGCAT_SAMPLE="${OUT_DIR}/sample-logcat.txt.log"
CURSOR_PROMPT="${OUT_DIR}/cursor-prompt.md"

# Helpers
ech() { printf '%s\n' "$*"; }
sep() { printf '\n==== %s ====\n\n' "$1"; }

# Clear old
: > "${FAIL_SUM}"
: > "${WARN_SUM}"
: > "${TOP_ERR}"
: > "${FLAVOR_STATS}"
: > "${FAILED_TEST_NAMES}"
: > "${LOGCAT_SAMPLE}"
: > "${CURSOR_PROMPT}"

ech "🧪 Starting analysis of test artifacts in: ${ART_DIR}"
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
    ech "  ❌ failures: ${failures_here}" >> "${FAIL_SUM}"
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

    # Extract failed test names (from old script logic)
    grep -E "<testcase|Test" "${f}" 2>/dev/null | \
      grep -E "failure|Exception|Assertion" -B1 2>/dev/null | \
      grep -E "name=" | \
      sed -E 's/.*name="([^"]+)".*/• \1/' | sort -u >> "${FAILED_TEST_NAMES}" || true

    # Also append whole <failure> sections to separate file for deeper inspection
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

# Search logcat and utp logs (enhanced from old script)
mapfile -t LOGCAT_FILES < <(find "${ART_DIR}" -type f -iname 'logcat*.txt' 2>/dev/null | sort || true)
mapfile -t LOGFILES < <(find "${ART_DIR}" -type f \( -iname 'utp.*.log' -o -iname '*.log' -o -iname 'testlog' \) 2>/dev/null | sort || true)

ech "Found ${#LOGCAT_FILES[@]} logcat files and ${#LOGFILES[@]} other log files to scan"

# Create logcat sample file (merged feature)
if [ "${#LOGCAT_FILES[@]}" -gt 0 ]; then
  ech "Creating logcat sample from first logcat file..."
  first_logcat="${LOGCAT_FILES[0]}"
  filesize=$(wc -c < "${first_logcat}" || echo 0)
  if [ "${filesize}" -gt $((1024*1024*5)) ]; then
    # Sample first and last 2000 lines for huge files
    ( head -n 2000 "${first_logcat}"; echo -e "\n...SKIP (file too large, showing first/last 2000 lines)...\n"; tail -n 2000 "${first_logcat}" ) > "${LOGCAT_SAMPLE}" || true
  else
    cp "${first_logcat}" "${LOGCAT_SAMPLE}" || true
  fi
fi

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

# Process logcat files with context lines (from old script)
# Limit to first 20 logcat files to avoid hanging
ech "Processing logcat files with context lines (limited to first 20 files)..."
logcat_count=0
for lf in "${LOGCAT_FILES[@]}"; do
  if [ "${logcat_count}" -ge 20 ]; then
    break
  fi
  logcat_count=$((logcat_count + 1))
  # Extract exceptions with context (from old script: -A3 -B1)
  grep -E "Exception|AssertionFailed|Error:" "${lf}" -A3 -B1 2>/dev/null | \
    sed 's/^/🔥 /' >> "${TOP_ERR}" || true
  
  # Extract warnings with context
  grep -E "WARN|Deprecated|timeout|slow|Skipped" "${lf}" -A1 -B1 2>/dev/null | \
    sed 's/^/⚠️ /' >> "${WARN_SUM}" || true
  
  # Aggregate for pattern counting
  filesize=$(wc -c < "${lf}" || echo 0)
  if [ "${filesize}" -gt $((1024*1024*5)) ]; then
    tmp="${OUT_DIR}/sample-$(basename "${lf}").log"
    ( head -n 2000 "${lf}"; echo -e "\n...SKIP...\n"; tail -n 2000 "${lf}" ) > "${tmp}" || true
    scanfile="${tmp}"
  else
    scanfile="${lf}"
  fi

  # find ERROR/Exception/FATAL lines for pattern counting
  while IFS= read -r line; do
    key=$(printf '%s' "$line" | sed -E 's/^[[:space:]]+//' | sed -E 's/(:[[:space:]].*)$//' | cut -c1-200)
    key="${key:-<empty>}"
    ERRCOUNT["$key"]=$((${ERRCOUNT["$key"]:-0} + 1))
  done < <(grep -I -E 'Exception|ERROR|FATAL|Caused by:' "${scanfile}" 2>/dev/null || true)

  # find WARNING lines for pattern counting
  while IFS= read -r line; do
    key=$(printf '%s' "$line" | sed -E 's/^[[:space:]]+//' | cut -c1-240)
    WARNCOUNT["$key"]=$((${WARNCOUNT["$key"]:-0} + 1))
  done < <(grep -I -E 'WARN|WARNING' "${scanfile}" 2>/dev/null || true)
done

# Process other log files (utp, etc.)
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

# Write top errors (enhanced with pattern counts)
ech "Top error patterns (sample) -> ${TOP_ERR}"
{
  echo "Top error/exception patterns across logs (count:pattern). Sample lines follow."
  echo
  for k in "${!ERRCOUNT[@]}"; do
    echo "${ERRCOUNT[$k]}|${k}"
  done | sort -rn -t'|' -k1 | head -n 60
} >> "${TOP_ERR}"

# Warnings summary (enhanced)
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
ech "Logcat files processed: ${#LOGCAT_FILES[@]}"
ech "Other log files processed: ${#LOGFILES[@]}"
ech "Analysis written to: ${OUT_DIR}"
ech ""

# Build comprehensive cursor prompt (self-contained with actual content)
sep "Generating comprehensive Cursor AI prompt -> ${CURSOR_PROMPT}"

# Build list of gradle logs found
GRADLE_LOG_LIST=""
if [ "${#GRADLE_LOGS[@]}" -gt 0 ]; then
  GRADLE_LOG_LIST=$(printf '%s\n' "${GRADLE_LOGS[@]}" | tr '\n' ',' | sed 's/,$//')
else
  GRADLE_LOG_LIST="none found"
fi

# Read all summary files into variables for embedding
FLAVOR_STATS_CONTENT=""
if [ -f "${FLAVOR_STATS}" ] && [ -s "${FLAVOR_STATS}" ]; then
  FLAVOR_STATS_CONTENT=$(cat "${FLAVOR_STATS}")
else
  FLAVOR_STATS_CONTENT="(no flavor statistics available)"
fi

FAIL_SUM_CONTENT=""
if [ -f "${FAIL_SUM}" ] && [ -s "${FAIL_SUM}" ]; then
  FAIL_SUM_CONTENT=$(head -n 100 "${FAIL_SUM}")
else
  FAIL_SUM_CONTENT="(no failures found)"
fi

TOP_ERR_CONTENT=""
if [ -f "${TOP_ERR}" ] && [ -s "${TOP_ERR}" ]; then
  TOP_ERR_CONTENT=$(head -n 150 "${TOP_ERR}")
else
  TOP_ERR_CONTENT="(no errors found)"
fi

WARN_SUM_CONTENT=""
if [ -f "${WARN_SUM}" ] && [ -s "${WARN_SUM}" ]; then
  WARN_SUM_CONTENT=$(head -n 100 "${WARN_SUM}")
else
  WARN_SUM_CONTENT="(no warnings found)"
fi

FAILED_TEST_NAMES_CONTENT=""
if [ -f "${FAILED_TEST_NAMES}" ] && [ -s "${FAILED_TEST_NAMES}" ]; then
  FAILED_TEST_NAMES_CONTENT=$(cat "${FAILED_TEST_NAMES}" | head -n 50)
else
  FAILED_TEST_NAMES_CONTENT="(no failed test names extracted)"
fi

LOGCAT_SAMPLE_CONTENT=""
if [ -f "${LOGCAT_SAMPLE}" ] && [ -s "${LOGCAT_SAMPLE}" ]; then
  LOGCAT_SAMPLE_CONTENT=$(head -n 100 "${LOGCAT_SAMPLE}")
else
  LOGCAT_SAMPLE_CONTENT="(no logcat sample available)"
fi

# Get repository name
REPO_NAME=$(basename "$(pwd)")

{
  # Use printf to avoid heredoc issues with backticks
  printf '%s\n' "# Cursor AI prompt: fix Android instrumented tests & CI (auto-generated)" > "${CURSOR_PROMPT}"
  printf 'Repository: %s\n' "${REPO_NAME}" >> "${CURSOR_PROMPT}"
  printf 'Workflow: android-build.yml (emulator-runner)\n' >> "${CURSOR_PROMPT}"
  printf 'Artifacts dir: %s\n' "${ART_DIR}" >> "${CURSOR_PROMPT}"
  printf 'Analysis dir: %s\n' "${OUT_DIR}" >> "${CURSOR_PROMPT}"
  printf 'Generated: %s\n' "$(date -u +"%Y-%m-%d %H:%M:%SZ")" >> "${CURSOR_PROMPT}"
  printf '\n' >> "${CURSOR_PROMPT}"
  printf '## TL;DR\n' >> "${CURSOR_PROMPT}"
  printf 'There are **%s** test failures across flavors. This document contains a comprehensive self-contained analysis with actual content from all summary files. We need Cursor AI to:\n' "${fail_count}" >> "${CURSOR_PROMPT}"
  printf '1. Inspect the failed tests and their stack traces\n' >> "${CURSOR_PROMPT}"
  printf '2. Propose and apply fixes (test stability, coroutine usage, Espresso waits)\n' >> "${CURSOR_PROMPT}"
  printf '3. Ensure CI workflow collects per-flavor reports reliably\n' >> "${CURSOR_PROMPT}"
  printf '\n---\n\n' >> "${CURSOR_PROMPT}"
  printf '## Test Statistics by Flavor\n\n' >> "${CURSOR_PROMPT}"
  printf '%s\n' '```' >> "${CURSOR_PROMPT}"
  printf '%s\n' "${FLAVOR_STATS_CONTENT}" >> "${CURSOR_PROMPT}"
  printf '%s\n' '```' >> "${CURSOR_PROMPT}"
  printf '\n---\n\n' >> "${CURSOR_PROMPT}"
  printf '## Failed Test Names\n\n' >> "${CURSOR_PROMPT}"
  printf '%s\n' '```' >> "${CURSOR_PROMPT}"
  printf '%s\n' "${FAILED_TEST_NAMES_CONTENT}" >> "${CURSOR_PROMPT}"
  printf '%s\n' '```' >> "${CURSOR_PROMPT}"
  printf '\n---\n\n' >> "${CURSOR_PROMPT}"
  printf '## Failures Summary (first 100 lines)\n\n' >> "${CURSOR_PROMPT}"
  printf '%s\n' '```' >> "${CURSOR_PROMPT}"
  printf '%s\n' "${FAIL_SUM_CONTENT}" >> "${CURSOR_PROMPT}"
  printf '%s\n' '```' >> "${CURSOR_PROMPT}"
  printf '\n---\n\n' >> "${CURSOR_PROMPT}"
  printf '## Top Errors (first 150 lines)\n\n' >> "${CURSOR_PROMPT}"
  printf '%s\n' '```' >> "${CURSOR_PROMPT}"
  printf '%s\n' "${TOP_ERR_CONTENT}" >> "${CURSOR_PROMPT}"
  printf '%s\n' '```' >> "${CURSOR_PROMPT}"
  printf '\n---\n\n' >> "${CURSOR_PROMPT}"
  printf '## Warnings Summary (first 100 lines)\n\n' >> "${CURSOR_PROMPT}"
  printf '%s\n' '```' >> "${CURSOR_PROMPT}"
  printf '%s\n' "${WARN_SUM_CONTENT}" >> "${CURSOR_PROMPT}"
  printf '%s\n' '```' >> "${CURSOR_PROMPT}"
  printf '\n---\n\n' >> "${CURSOR_PROMPT}"
  printf '## Logcat Sample (first 100 lines)\n\n' >> "${CURSOR_PROMPT}"
  printf '%s\n' '```' >> "${CURSOR_PROMPT}"
  printf '%s\n' "${LOGCAT_SAMPLE_CONTENT}" >> "${CURSOR_PROMPT}"
  printf '%s\n' '```' >> "${CURSOR_PROMPT}"
  printf '\n---\n\n' >> "${CURSOR_PROMPT}"
  printf '## Files to inspect (priority)\n\n' >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Failed test details: \`${OUT_DIR}/failures-summary.txt\`" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Raw failure dumps: \`${OUT_DIR}/raw-failures.log\`" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Flavor statistics: \`${OUT_DIR}/flavor-statistics.txt\`" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Failed test names: \`${OUT_DIR}/failed-test-names.txt\`" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Logcat sample: \`${OUT_DIR}/sample-logcat.txt.log\`" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Gradle logs: ${GRADLE_LOG_LIST}" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Collected logs: \`${OUT_DIR}/top-errors.txt\`, \`${OUT_DIR}/warnings-summary.txt\`" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Per-flavor report dirs: \`${ART_DIR}/outputs/*\` and \`${ART_DIR}/reports/*\`" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Workflow file: \`.github/workflows/android-build.yml\` (emulator step / script generation)" >> "${CURSOR_PROMPT}"
  printf '\n---\n\n' >> "${CURSOR_PROMPT}"
  printf '## Suggested fixes (high level)\n\n' >> "${CURSOR_PROMPT}"
  printf '### Tests:\n' >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Convert blocking uses to \`runTest\` from \`kotlinx.coroutines.test\` where appropriate." >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Wrap suspend DAO calls in proper coroutine contexts or \`database.runInTransaction\` with \`runBlocking\` only if absolutely necessary; prefer \`runTest\`." >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Add warm-up runs for DB/Room performance tests to avoid first-run variability." >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Add small \`Thread.sleep(500)\` or \`IdlingResource\` waits where Espresso interacts with Activity startup to avoid focus/race flakiness." >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Ensure instrumentation tests use \`ActivityScenario.launch()\` before interactions." >> "${CURSOR_PROMPT}"
  printf '%s\n' "- For performance tests, measure median instead of mean and run multiple warm-ups.\n" >> "${CURSOR_PROMPT}"
  printf '### CI / workflow:\n' >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Ensure gradle output is fully redirected to per-flavor \`gradle-build-<flavor>.log\` files:" >> "${CURSOR_PROMPT}"
  printf '%s\n' "  \`cd truth-android-client && ./gradlew connectedLocalDebugAndroidTest ... > ../gradle-build-local.log 2>&1\`" >> "${CURSOR_PROMPT}"
  printf '%s\n' "  Each flavor (local, mock, remote) and performance tests should have separate log files." >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Enable Gradle build cache for performance: pass \`--build-cache\` to gradle invocations and ensure \`~/.gradle\` is cached with \`actions/cache\`." >> "${CURSOR_PROMPT}"
  printf '%s\n' "- After each flavor run, **move/copy** per-flavor reports into \`\${ART_DIR}\` and **clear** \`truth-android-client/app/build/outputs/androidTest-results\` and \`.../reports/androidTests/connected\` to avoid subsequent flavors overwriting previous reports." >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Keep \`adb logcat -d\` executed **before** emulator kill." >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Use an explicit \`shell: bash\` on steps that use advanced shell features." >> "${CURSOR_PROMPT}"
  printf '%s\n' "- Use a generated script (e.g., \`run_tests.sh\`) as the single entrypoint (already present) and call it via \`script: ./run_tests.sh\` in emulator action.\n" >> "${CURSOR_PROMPT}"
  printf '\n---\n\n' >> "${CURSOR_PROMPT}"
  printf '## Concrete PR tasks for Cursor AI (apply via single PR)\n\n' >> "${CURSOR_PROMPT}"
  printf '1. **Tests**: update identified failing test files (see failed test names above) to use `runTest`, add warm-ups and small waits, and fix coroutine usage.\n' >> "${CURSOR_PROMPT}"
  printf '   - Example change: change `runBlocking { ... }` -> `runTest { ... }`, make DAO operations suspend inside `runTest`.\n\n' >> "${CURSOR_PROMPT}"
  printf '2. **CI**: update `.github/workflows/android-build.yml`:\n' >> "${CURSOR_PROMPT}"
  printf '   - Ensure gradle invocations use `--build-cache` and redirect stdout/stderr to per-flavor `gradle-build-<flavor>.log`.\n' >> "${CURSOR_PROMPT}"
  printf '   - After each flavor run, copy per-flavor reports into `${ART_DIR}/outputs/<flavor>` and then `rm -rf` the build outputs in the project so next flavor does not overwrite.\n' >> "${CURSOR_PROMPT}"
  printf '   - Ensure `adb logcat -d` is executed (and saved) before emulator termination.\n' >> "${CURSOR_PROMPT}"
  printf '   - Set `shell: bash` for the step generating and running scripts.\n\n' >> "${CURSOR_PROMPT}"
  printf '3. **Scripts**: Add the script `./analyze-android-failures.sh` (this file) and a `README` entry describing how to use analysis artifacts.\n\n' >> "${CURSOR_PROMPT}"
  printf '\n---\n\n' >> "${CURSOR_PROMPT}"
  printf '## Priority (1..3)\n\n' >> "${CURSOR_PROMPT}"
  printf '1. Capture full gradle logs per-flavor and persist.\n' >> "${CURSOR_PROMPT}"
  printf '2. Fix tests that fail consistently (see failures-summary above).\n' >> "${CURSOR_PROMPT}"
  printf '3. Improve CI behavior to preserve per-flavor reports and enable build-cache.\n\n' >> "${CURSOR_PROMPT}"
  printf '\n---\n\n' >> "${CURSOR_PROMPT}"
  printf '## Next Steps for Cursor AI\n\n' >> "${CURSOR_PROMPT}"
  printf '1. **Review the failures summary above** to identify patterns (e.g., RootViewWithoutFocusException, UncompletedCoroutinesError, performance threshold violations).\n\n' >> "${CURSOR_PROMPT}"
  printf '2. **Check the failed test names** to locate the specific test methods that need fixes.\n\n' >> "${CURSOR_PROMPT}"
  printf '3. **Examine the top errors** to understand common exception types and their stack traces.\n\n' >> "${CURSOR_PROMPT}"
  printf '4. **Review warnings** for potential performance issues or deprecated API usage.\n\n' >> "${CURSOR_PROMPT}"
  printf '5. **Inspect logcat sample** for runtime errors that might not appear in test XML files.\n\n' >> "${CURSOR_PROMPT}"
  printf '6. **Open the actual test files** (paths can be inferred from class names in failures-summary) and apply fixes:\n' >> "${CURSOR_PROMPT}"
  printf '   - Replace `runBlocking` with `runTest`\n' >> "${CURSOR_PROMPT}"
  printf '   - Add proper waits for Espresso interactions\n' >> "${CURSOR_PROMPT}"
  printf '   - Fix coroutine context issues\n' >> "${CURSOR_PROMPT}"
  printf '   - Adjust performance thresholds if needed\n\n' >> "${CURSOR_PROMPT}"
  printf '7. **Verify fixes** by running tests locally or checking CI results.\n\n' >> "${CURSOR_PROMPT}"
  printf '\n---\n\n' >> "${CURSOR_PROMPT}"
  printf '## Artifacts produced by this analysis\n\n' >> "${CURSOR_PROMPT}"
  printf '%s\n' "- \`${FAIL_SUM}\` - Detailed failure information" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- \`${TOP_ERR}\` - Top error patterns and exceptions" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- \`${WARN_SUM}\` - Warning summary" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- \`${FLAVOR_STATS}\` - Statistics per flavor" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- \`${FAILED_TEST_NAMES}\` - List of failed test names" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- \`${LOGCAT_SAMPLE}\` - Sample logcat output" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- \`${OUT_DIR}/raw-failures.log\` - Raw failure XML dumps" >> "${CURSOR_PROMPT}"
  printf '%s\n' "- \`${CURSOR_PROMPT}\` - This file (self-contained prompt)" >> "${CURSOR_PROMPT}"
  printf '\n---\n\n' >> "${CURSOR_PROMPT}"
}

ech "✅ Analysis complete."
ech "Summary files:"
ech " - ${FAIL_SUM}"
ech " - ${WARN_SUM}"
ech " - ${TOP_ERR}"
ech " - ${FLAVOR_STATS}"
ech " - ${FAILED_TEST_NAMES}"
ech " - ${LOGCAT_SAMPLE}"
ech " - ${CURSOR_PROMPT}"
ech ""
ech "To run locally: ./analyze-android-failures.sh"
ech "To override paths: ART_DIR=\"test-results/\" OUT_DIR=\"analysis/\" ./analyze-android-failures.sh"
ech ""
