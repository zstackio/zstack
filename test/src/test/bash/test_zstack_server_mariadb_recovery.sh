#!/bin/sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_dir=$(CDPATH= cd -- "$script_dir/../../../.." && pwd)
script="$repo_dir/conf/install/zstack-server"
tmp_dir=$(mktemp -d)

cleanup() {
    rm -rf "$tmp_dir"
}

trap cleanup EXIT

bin_dir="$tmp_dir/bin"
mkdir -p "$bin_dir"

cat > "$bin_dir/zstack-ctl" <<'EOF'
#!/bin/sh
echo "zstack-ctl $*" >> "$ZSTUB_CALLS"
exit 0
EOF

cat > "$bin_dir/systemctl" <<'EOF'
#!/bin/sh
case "$1 $2" in
    "list-unit-files mariadb.service")
        echo "mariadb.service enabled"
        exit 0
        ;;
    "is-active --quiet")
        [ "${ZSTUB_MARIADB_ACTIVE:-false}" = "true" ] && exit 0
        exit 3
        ;;
    "reset-failed mariadb")
        echo "systemctl reset-failed mariadb" >> "$ZSTUB_CALLS"
        exit 0
        ;;
    "start mariadb")
        echo "systemctl start mariadb" >> "$ZSTUB_CALLS"
        exit 0
        ;;
esac
exit 1
EOF

cat > "$bin_dir/lsof" <<'EOF'
#!/bin/sh
exit 1
EOF

cat > "$bin_dir/fuser" <<'EOF'
#!/bin/sh
exit 1
EOF

chmod +x "$bin_dir/zstack-ctl" "$bin_dir/systemctl" "$bin_dir/lsof" "$bin_dir/fuser"

assert_contains() {
    grep -F "$1" "$2" >/dev/null || {
        echo "expected '$1' in $2" >&2
        exit 1
    }
}

calls="$tmp_dir/calls"
socket="$tmp_dir/mysql.sock"
log_dir="$tmp_dir/log"
touch "$socket"

PATH="$bin_dir:$PATH" \
ZSTUB_CALLS="$calls" \
ZSTACK_MARIADB_SOCKET="$socket" \
ZSTACK_SERVER_LOG_DIR="$log_dir" \
sh "$script" start >/dev/null

[ ! -e "$socket" ] || {
    echo "expected stale socket to be removed" >&2
    exit 1
}
assert_contains "systemctl reset-failed mariadb" "$calls"
assert_contains "systemctl start mariadb" "$calls"
assert_contains "zstack-ctl start" "$calls"

sysv_bin_dir="$tmp_dir/sysv-bin"
mkdir -p "$sysv_bin_dir"
cp "$bin_dir/zstack-ctl" "$bin_dir/lsof" "$bin_dir/fuser" "$sysv_bin_dir/"
ln -s /usr/bin/grep "$sysv_bin_dir/grep"
cat > "$sysv_bin_dir/which" <<'EOF'
#!/bin/sh
command -v "$1"
EOF
cat > "$sysv_bin_dir/service" <<'EOF'
#!/bin/sh
if [ "$1 $2" = "mariadb status" ]; then
    echo "mariadb is running"
    exit 0
fi
exit 1
EOF
chmod +x "$sysv_bin_dir/which" "$sysv_bin_dir/service"

calls="$tmp_dir/calls-sysv-active"
socket="$tmp_dir/mysql-sysv-active.sock"
touch "$socket"

PATH="$sysv_bin_dir" \
ZSTUB_CALLS="$calls" \
ZSTACK_MARIADB_SOCKET="$socket" \
ZSTACK_SERVER_LOG_DIR="$log_dir" \
/bin/sh "$script" start >/dev/null

[ -e "$socket" ] || {
    echo "expected active SysV MariaDB socket to be kept" >&2
    exit 1
}
if grep -F "systemctl start mariadb" "$calls" >/dev/null 2>&1; then
    echo "unexpected call: systemctl start mariadb" >&2
    exit 1
fi
assert_contains "zstack-ctl start" "$calls"

no_probe_bin_dir="$tmp_dir/no-probe-bin"
mkdir -p "$no_probe_bin_dir"
cp "$bin_dir/zstack-ctl" "$bin_dir/systemctl" "$no_probe_bin_dir/"
ln -s /usr/bin/grep "$no_probe_bin_dir/grep"
cat > "$no_probe_bin_dir/which" <<'EOF'
#!/bin/sh
command -v "$1"
EOF
chmod +x "$no_probe_bin_dir/which"

calls="$tmp_dir/calls-no-probe"
socket="$tmp_dir/mysql-no-probe.sock"
touch "$socket"

PATH="$no_probe_bin_dir" \
ZSTUB_CALLS="$calls" \
ZSTACK_MARIADB_SOCKET="$socket" \
ZSTACK_SERVER_LOG_DIR="$log_dir" \
/bin/sh "$script" start >/dev/null

[ -e "$socket" ] || {
    echo "expected socket to be kept when lsof and fuser are unavailable" >&2
    exit 1
}
if grep -F "systemctl start mariadb" "$calls" >/dev/null 2>&1; then
    echo "unexpected call: systemctl start mariadb" >&2
    exit 1
fi
assert_contains "zstack-ctl start" "$calls"

calls="$tmp_dir/calls-active"
socket="$tmp_dir/mysql-active.sock"
touch "$socket"

PATH="$bin_dir:$PATH" \
ZSTUB_CALLS="$calls" \
ZSTUB_MARIADB_ACTIVE=true \
ZSTACK_MARIADB_SOCKET="$socket" \
ZSTACK_SERVER_LOG_DIR="$log_dir" \
sh "$script" start >/dev/null

[ -e "$socket" ] || {
    echo "expected active MariaDB socket to be kept" >&2
    exit 1
}
if grep -F "systemctl start mariadb" "$calls" >/dev/null 2>&1; then
    echo "unexpected call: systemctl start mariadb" >&2
    exit 1
fi
assert_contains "zstack-ctl start" "$calls"
