#!/usr/bin/env bash
# run-case.sh — single-source IT case runner with locked .m2 + freshness guard.
#
# Usage:
#   ./scripts/run-case.sh <CaseName> [--rebuild] [--pre-clean]
#
# Behavior:
#   - Auto-resolves the case's Maven module by locating <CaseName>.groovy under
#     a *Test/groovy/ tree (test/ for OSS, premium/test-premium/ for premium).
#   - Pins -Dmaven.repo.local to the worktree's .m2/repository (CLAUDE.md §13);
#     refuses to fall back to ~/.m2 even if the worktree m2 is empty.
#   - --rebuild   : run ./runMavenProfile premium (full clean install) first.
#   - --pre-clean : rm -rf the worktree .m2 before --rebuild (extreme isolation).
#   - Tees output to /tmp/run-case-<case>-<ts>.log; parses outcome with two
#     conditions (BUILD SUCCESS && Tests run: >= 1) so a "0 tests run" build
#     does NOT silently count as pass.
#   - On failure prints the first Caused-by + Tests-in-error lines.
#
# Exit codes:
#   0  PASS  (BUILD SUCCESS + at least one test executed)
#   1  FAIL  (BUILD FAILURE or Tests run: 0)
#   2  case file not found in repo
#   3  worktree .m2 missing and --rebuild not requested
#   4  bad arguments

set -euo pipefail

usage() {
    echo "Usage: $0 <CaseName> [--rebuild] [--pre-clean]" >&2
    exit 4
}

[ $# -lt 1 ] && usage
CASE="$1"; shift
REBUILD=0
PRE_CLEAN=0
for arg in "$@"; do
    case "$arg" in
        --rebuild)   REBUILD=1 ;;
        --pre-clean) PRE_CLEAN=1; REBUILD=1 ;;
        *) echo "[run-case] unknown arg: $arg" >&2; usage ;;
    esac
done

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

# Trip-wire: reject new production-code UNIT_TEST_ON shortcuts.
# Existing files grandfathered in .harness/ut-shortcut-baseline.txt.
if [ -x "$REPO_ROOT/scripts/check-no-new-ut-shortcuts.sh" ]; then
    "$REPO_ROOT/scripts/check-no-new-ut-shortcuts.sh"
fi

LOCAL_REPO="$REPO_ROOT/.m2/repository"

if [ "$PRE_CLEAN" -eq 1 ]; then
    echo "[run-case] --pre-clean: rm -rf $LOCAL_REPO"
    rm -rf "$LOCAL_REPO"
fi

if [ "$REBUILD" -eq 1 ]; then
    echo "[run-case] --rebuild: ./runMavenProfile premium (clean install full reactor)"
    ./runMavenProfile premium
fi

if [ ! -d "$LOCAL_REPO" ]; then
    echo "[run-case] ERROR: $LOCAL_REPO does not exist."             >&2
    echo "[run-case] Re-run with --rebuild to populate the worktree m2." >&2
    exit 3
fi

# Auto-resolve case module: search src/test/groovy for <CaseName>.groovy
CASE_PATH=$(find "$REPO_ROOT" \
    \( -path "$REPO_ROOT/.git" -o -path "$REPO_ROOT/.m2" -o -path "$REPO_ROOT/worktrees" \) -prune \
    -o -type f -name "${CASE}.groovy" -path '*/src/test/groovy/*' -print 2>/dev/null \
    | head -1)

if [ -z "$CASE_PATH" ]; then
    echo "[run-case] ERROR: ${CASE}.groovy not found under any src/test/groovy tree." >&2
    exit 2
fi

# MOD_PATH = directory containing pom.xml that owns this src/test/groovy
MOD_PATH=$(echo "$CASE_PATH" | sed -E 's|/src/test/groovy/.*||')
[ ! -f "$MOD_PATH/pom.xml" ] && {
    echo "[run-case] ERROR: no pom.xml at $MOD_PATH (resolved from $CASE_PATH)" >&2
    exit 2
}

TS=$(date +%Y%m%d-%H%M%S)
LOG="/tmp/run-case-${CASE}-${TS}.log"
HEAD_SHA=$(git rev-parse --short HEAD)

echo "[run-case] case      = $CASE"
echo "[run-case] module    = $MOD_PATH"
echo "[run-case] m2        = $LOCAL_REPO"
echo "[run-case] HEAD      = $HEAD_SHA"
echo "[run-case] log       = $LOG"
echo "[run-case] starting mvn test ..."

cd "$MOD_PATH"
# Concurrent mvn / surefire fork JVMs racing on the same .m2 jars cause
# libzip.so SIGBUS (BUS_ADRERR) when one process mmaps a jar that another is
# overwriting. Symptom is "forked VM terminated" with hs_err_pid log showing
# a libzip frame. Kill leftovers from prior runs before starting a fresh fork.
echo "[run-case] killing leftover surefire fork JVMs for user $USER ..."
# Only target surefirebooter (the JVM that mmaps test jars).
# Do NOT pkill plexus.classworlds.launcher.Launcher — that would also kill any
# mvn process about to start, including this script's own mvn invocation.
pkill -u "$USER" -f "[s]urefirebooter" 2>/dev/null || true
sleep 1
# pgrep + pipefail trap: pgrep exit=1 when no match propagates through wc to abort
# the whole pipe under `set -euo pipefail`. Wrap in `set +e` block to tolerate it.
set +e
LEFTOVER_PIDS=$(pgrep -u "$USER" -f "surefirebooter" 2>/dev/null)
set -e
if [ -n "$LEFTOVER_PIDS" ]; then
    LEFTOVER_COUNT=$(printf '%s\n' "$LEFTOVER_PIDS" | wc -l)
    echo "[run-case] WARNING: $LEFTOVER_COUNT surefire fork(s) still alive after pkill"
    pgrep -u "$USER" -af "surefirebooter" 2>/dev/null | sed 's/^/[run-case]   /' || true
fi

# IMPORTANT: -Dmaven.repo.local is the only knob that prevents ~/.m2 fallback.
# Do NOT remove. Do NOT let surefire fork inherit a different repo path.
# -B (batch mode) suppresses ANSI color so grep / sed downstream stays clean.
set +e
mvn -B test \
    -Dtest="$CASE" \
    -DfailIfNoTests=false \
    -Dmaven.repo.local="$LOCAL_REPO" \
    2>&1 | tee "$LOG"
MVN_EXIT=${PIPESTATUS[0]}
set -e

# Outcome parse — two conditions to avoid "0 tests run" silent-pass.
BUILD_OK=0
TESTS_RAN=0
grep -q "BUILD SUCCESS" "$LOG" && BUILD_OK=1
if grep -qE "Tests run: [1-9][0-9]*, Failures: 0, Errors: 0" "$LOG"; then
    TESTS_RAN=1
fi

echo
echo "[run-case] -------------------------------------------------"
echo "[run-case] case      : $CASE"
echo "[run-case] HEAD      : $HEAD_SHA"
echo "[run-case] mvn exit  : $MVN_EXIT"
echo "[run-case] BUILD OK  : $BUILD_OK"
echo "[run-case] tests ran : $TESTS_RAN"

if [ "$BUILD_OK" -eq 1 ] && [ "$TESTS_RAN" -eq 1 ]; then
    echo "[run-case] PASS  log=$LOG"
    exit 0
else
    echo "[run-case] FAIL  log=$LOG"
    echo "[run-case] root cause(s):"
    grep -E "Caused by:|Tests in error:|Tests in failure:|forked VM terminated" "$LOG" \
        | head -5 | sed 's/^/[run-case]   /'
    exit 1
fi
