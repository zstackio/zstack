#!/usr/bin/env bash
# Trip-wire: reject NEW production-code files that introduce
# `if (CoreGlobalProperty.UNIT_TEST_ON) { ... }` short-circuit branches.
#
# Existing files (legacy debt) are grandfathered in
# `.harness/ut-shortcut-baseline.txt`. Any production file (under
# `src/main/java`) outside the baseline that matches the pattern fails the
# check. Removing files from the baseline (cleanup work) is allowed.
#
# Why: short-circuiting production code paths in test mode hides regressions
# (case in point — premium/baremetal2/.../BareMetal2DpuChassisFactory.java
# UNIT_TEST_ON early-return skipped the chassis hostUuid wire-up, breaking
# BareMetal2ChassisCase silently for months). New code should expose a
# real seam (extension point, hijackable bean, fixture helper) instead.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASELINE="$REPO_ROOT/.harness/ut-shortcut-baseline.txt"

if [[ ! -f "$BASELINE" ]]; then
    echo "[ut-shortcut] baseline not found: $BASELINE" >&2
    exit 2
fi

CURRENT="$(mktemp)"
trap 'rm -f "$CURRENT"' EXIT

cd "$REPO_ROOT"
grep -rlE 'if\s*\(\s*CoreGlobalProperty\.UNIT_TEST_ON\s*\)' \
    --include='*.java' . 2>/dev/null \
    | grep -E '/src/main/java/' \
    | grep -v '/target/' \
    | sort > "$CURRENT"

NEW_OFFENDERS="$(comm -23 "$CURRENT" "$BASELINE" || true)"

if [[ -n "$NEW_OFFENDERS" ]]; then
    echo "[ut-shortcut] FAIL: new files added 'if (CoreGlobalProperty.UNIT_TEST_ON)' shortcuts:" >&2
    echo "$NEW_OFFENDERS" | sed 's/^/  - /' >&2
    echo >&2
    echo "Production code must not branch on UNIT_TEST_ON. Use a real seam:" >&2
    echo "  - PluginRegistry extension point + a test-only @Component" >&2
    echo "  - Spring bean replacement via testlib hijackSimulator" >&2
    echo "  - Fixture helper that mocks via the existing SDK API" >&2
    echo >&2
    echo "If the file genuinely belongs in baseline (legacy migration only)," >&2
    echo "add it to .harness/ut-shortcut-baseline.txt with explicit human review." >&2
    exit 1
fi

REMOVED="$(comm -13 "$CURRENT" "$BASELINE" || true)"
if [[ -n "$REMOVED" ]]; then
    echo "[ut-shortcut] OK; baseline can be tightened — these files no longer match:" >&2
    echo "$REMOVED" | sed 's/^/  - /' >&2
    echo "Run: scripts/check-no-new-ut-shortcuts.sh --refresh-baseline" >&2
fi

if [[ "${1:-}" == "--refresh-baseline" ]]; then
    cp "$CURRENT" "$BASELINE"
    echo "[ut-shortcut] baseline refreshed: $BASELINE" >&2
fi

echo "[ut-shortcut] PASS"
