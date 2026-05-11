#!/usr/bin/env bash
# mvn-safe-install.sh — auto-detects header/compute staleness and forces clean install.
#
# Usage:
#   ./scripts/mvn-safe-install.sh -pl plugin/physicalServer,plugin/kvm -am [other mvn args]
#
# Behavior:
#   - Always uses local .m2 (project's .m2/repository).
#   - Always passes -P premium and -DskipTests (matches our test workflow).
#   - Detects if any header/abstraction Java OR shared *VO.java is newer than
#     the compute jar. If yes, forces 'mvn clean install' instead of bare
#     'mvn install' to prevent the AspectJ-woven VerifyError pattern.
#   - On success, records HEAD SHA to .m2/LAST_BUILD_COMMIT for later diffs.

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

LOCAL_REPO="$REPO_ROOT/.m2/repository"
COMPUTE_JAR="$LOCAL_REPO/org/zstack/compute/5.5.0/compute-5.5.0.jar"

is_stale() {
    [ ! -f "$COMPUTE_JAR" ] && return 0
    local jar_mtime newest_src_mtime
    jar_mtime=$(stat -c '%Y' "$COMPUTE_JAR")
    newest_src_mtime=$(find header/src/main/java abstraction/src/main/java -name '*.java' \
        -printf '%T@\n' 2>/dev/null | cut -d. -f1 | sort -n | tail -1)
    [ -z "$newest_src_mtime" ] && return 1
    [ "$newest_src_mtime" -gt "$jar_mtime" ]
}

if is_stale; then
    echo "[mvn-safe-install] STALE GUARD: header/abstraction sources newer than compute jar"
    echo "[mvn-safe-install] forcing 'clean install' to avoid VerifyError on AspectJ lambdas"
    MVN_GOAL="clean install"
else
    MVN_GOAL="install"
fi

mvn $MVN_GOAL -DskipTests -o -P premium \
    -Dmaven.repo.local="$LOCAL_REPO" \
    "$@"

# Record successful build SHA — later runs can use this for finer-grained diff
git rev-parse HEAD > "$LOCAL_REPO/../LAST_BUILD_COMMIT"

echo "[mvn-safe-install] done: $MVN_GOAL completed for HEAD=$(git rev-parse --short HEAD)"
