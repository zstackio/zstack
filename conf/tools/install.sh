#!/bin/bash
current_folder=`pwd`
relative_script_parent_path=`dirname $0`
if [ ${relative_script_parent_path:0:1} = "/" ]; then
    cwd=$relative_script_parent_path
else
    cwd=$current_folder/$relative_script_parent_path
fi

pypi_path=file://$cwd/../../../static/pypi/simple
zstack_build_tmpdir=/usr/local/zstack/

usage() {
    echo "usage:$0 [zstack-cli|zstack-ctl|zstack-dashboard|zstack-ui|zstack-sys]"
    exit 1
}

tool=$1
force=$2

if [ -z $tool ]; then
  usage
fi

install_pip() {
    pip3.11 --version | grep 22.3.1 >/dev/null || yum install -y python3.11-pip
}

zstack_local_repo_args() {
    grep -R "^\[zstack-local\]" /etc/yum.repos.d/*.repo >/dev/null 2>&1 && echo "--disablerepo=* --enablerepo=zstack-local"
}

yum_install_packages() {
    local repo_args=`zstack_local_repo_args`
    yum -y $repo_args install $@
}

yum_reinstall_packages() {
    local repo_args=`zstack_local_repo_args`
    yum -y $repo_args reinstall $@ || yum -y $repo_args install $@
}

ensure_rpm_packages_installed() {
    local missing_list=`LANG=en_US.UTF-8 rpm -q $@ 2>/dev/null | grep 'not installed' | awk 'BEGIN{ORS=" "}{ print $2 }'`
    [ -z "$missing_list" ] && return 0

    echo "Installing zstack-ctl build dependencies: $missing_list"
    yum_install_packages $missing_list
}

verify_rpm_packages() {
    local broken_list=""
    local pkg

    for pkg in $@; do
        rpm -q $pkg >/dev/null 2>&1 || continue
        rpm -V $pkg >/dev/null 2>&1 || broken_list="$broken_list $pkg"
    done

    [ -z "$broken_list" ] && return 0

    echo "Reinstalling broken zstack-ctl build dependencies:$broken_list"
    yum_reinstall_packages $broken_list
}

python_cflags_need_annobin() {
    $1 - <<'PY' 2>/dev/null | grep -q 'annobin'
import sysconfig
print(sysconfig.get_config_var("CFLAGS") or "")
PY
}

python_cflags_need_redhat_rpm_config() {
    $1 - <<'PY' 2>/dev/null | grep -qE 'redhat|^-specs=| -specs='
import sysconfig
print(sysconfig.get_config_var("CFLAGS") or "")
PY
}

verify_python_c_extension_build() {
    local python_bin=$1
    local tmp_c=`TMPDIR="$zstack_build_tmpdir" mktemp zstackctl-build-check.XXXXXX.c`
    [ -z "$tmp_c" ] && return 1
    local tmp_o=${tmp_c%.c}.o
    local py_cflags=`$python_bin - <<'PY'
import sysconfig
print(sysconfig.get_config_var("CFLAGS") or "")
PY
`
    local py_include=`$python_bin - <<'PY'
import sysconfig
include_dir = sysconfig.get_config_var("INCLUDEPY") or ""
print("-I%s" % include_dir if include_dir else "")
PY
`

    cat > "$tmp_c" <<'EOF'
#include <Python.h>
#include <ffi.h>
#include <openssl/ssl.h>
int main(void) { return 0; }
EOF

    gcc $py_cflags $py_include -c "$tmp_c" -o "$tmp_o"
    local ret=$?
    rm -f "$tmp_c" "$tmp_o"

    if [ $ret -ne 0 ]; then
        echo "Failed to compile a minimal Python C extension. Please check gcc, python3.11-devel, libffi-devel, openssl-devel, redhat-rpm-config and annobin."
        return 1
    fi

    return 0
}

ensure_zstack_ctl_build_deps() {
    command -v rpm >/dev/null 2>&1 || return 0
    command -v yum >/dev/null 2>&1 || return 0

    local python_bin=python3.11
    local base_deps="$python_bin ${python_bin}-devel ${python_bin}-pip gcc libffi-devel openssl-devel"
    ensure_rpm_packages_installed $base_deps || return 1
    verify_rpm_packages $base_deps || return 1
    local need_redhat_rpm_config=false
    local need_annobin=false

    if python_cflags_need_redhat_rpm_config $python_bin; then
        need_redhat_rpm_config=true
        ensure_rpm_packages_installed redhat-rpm-config || return 1
    fi

    if python_cflags_need_annobin $python_bin; then
        need_annobin=true
        ensure_rpm_packages_installed annobin || return 1
    fi

    if $need_redhat_rpm_config; then
        verify_rpm_packages redhat-rpm-config || return 1
    fi
    if $need_annobin; then
        verify_rpm_packages annobin || return 1
    fi

    verify_python_c_extension_build $python_bin
}

# Ensure the virtualenv at $1 is a Python 3.11 venv.
# If it does not exist or is a legacy Python 2 venv, recreate it.
ensure_python3_venv() {
    local venv_path=$1
    local allowed_prefix="/var/lib/zstack/virtualenv"

    if [[ "$venv_path" != "$allowed_prefix"* || "$venv_path" == *".."* ]]; then
        echo "Error: Path must start with $allowed_prefix. Provided: $venv_path" >&2
        exit 1
    fi

    if [ -d "$venv_path" ] && [ -x "$venv_path/bin/python3.11" ]; then
        return 0
    fi
    # retry once: rm -rf may fail if zstack_service_exporter is regenerating .pyc concurrently
    rm -rf "$venv_path" || rm -rf "$venv_path" || exit 1
    python3.11 -m venv "$venv_path" || exit 1
}


cd $cwd

if [ $tool = 'zstack-ctl' ]; then
    ensure_zstack_ctl_build_deps || exit 1
fi

install_pip
cd /tmp

if [ $tool = 'zstack-cli' ]; then
    CLI_VIRENV_PATH=/var/lib/zstack/virtualenv/zstackcli
    [ ! -z $force ] && rm -rf $CLI_VIRENV_PATH
    ensure_python3_venv "$CLI_VIRENV_PATH"
    . $CLI_VIRENV_PATH/bin/activate
    cd $cwd
    pip install -i $pypi_path --trusted-host localhost --ignore-installed zstackcli-*.tar.gz apibinding-*.tar.gz
    if [ $? -ne 0 ]; then
        rm -rf $CLI_VIRENV_PATH
        exit 1
    fi
    pip show zstacklib
    if [ $? -ne 0 ]; then
        # fresh install zstacklib
        echo "Installing zstacklib..."
        pip install -i $pypi_path --trusted-host localhost --ignore-installed zstacklib-*.tar.gz
        if [ $? -ne 0 ]; then
            rm -rf $CLI_VIRENV_PATH
            exit 1
        fi
    else
        # upgrade zstacklib
        echo "Upgrading zstacklib..."
        pip install -U -i $pypi_path --trusted-host localhost zstacklib-*.tar.gz
        if [ $? -ne 0 ]; then
            rm -rf $CLI_VIRENV_PATH
            exit 1
        fi
    fi
    [ -f $CLI_VIRENV_PATH/bin/zstack-cli ] && cp $CLI_VIRENV_PATH/bin/zstack-cli /usr/bin/zstack-cli
    chmod +x /usr/bin/zstack-cli

elif [ $tool = 'zstack-ctl' ]; then
    CTL_VIRENV_PATH=/var/lib/zstack/virtualenv/zstackctl
    ensure_python3_venv "$CTL_VIRENV_PATH"
    . $CTL_VIRENV_PATH/bin/activate
    cd $cwd
    TMPDIR="$zstack_build_tmpdir" pip install -i $pypi_path --trusted-host localhost --ignore-installed zstackctl-*.tar.gz || exit 1
    TMPDIR="$zstack_build_tmpdir" pip install -i $pypi_path --trusted-host localhost --ignore-installed pycryptodome || exit 1
    [ -f $CTL_VIRENV_PATH/bin/zstack-ctl ] && cp $CTL_VIRENV_PATH/bin/zstack-ctl /usr/bin/zstack-ctl
    chmod +x /usr/bin/zstack-ctl
    python $CTL_VIRENV_PATH/lib/python3.11/site-packages/zstackctl/generate_zstackctl_bash_completion.py

elif [ $tool = 'zstack-sys' ]; then
    SYS_VIRENV_PATH=/var/lib/zstack/virtualenv/zstacksys
    ensure_python3_venv "$SYS_VIRENV_PATH"
    # RE_INSTALL
    if [ ! -x "$SYS_VIRENV_PATH/bin/ansible" ] || ! "$SYS_VIRENV_PATH/bin/ansible" --version 2>/dev/null | grep -q 'core 2.16.14'; then
        rm -rf $SYS_VIRENV_PATH && python3.11 -m venv $SYS_VIRENV_PATH || exit 1
        . $SYS_VIRENV_PATH/bin/activate
        cd $cwd
        #TMPDIR=/usr/local/zstack/ pip install -i $pypi_path --trusted-host localhost --ignore-installed setuptools==65.5.1 || exit 1
        TMPDIR="$zstack_build_tmpdir" pip install -i $pypi_path --trusted-host localhost --ignore-installed ansible==9.13.0 || exit 1

        cat > /usr/bin/ansible << EOF
#! /bin/sh
VIRTUAL_ENV=/var/lib/zstack/virtualenv/zstacksys
if [ ! -d $VIRTUAL_ENV ]; then
    echo "Need to install zstacksys before using it"
    exit 1
fi

LANG=en_US.UTF-8
LC_ALL=en_US.utf8
export LANG LC_ALL
. ${VIRTUAL_ENV}/bin/activate

\${VIRTUAL_ENV}/bin/ansible \$@
EOF
        chmod +x /usr/bin/ansible

        cat > /usr/bin/ansible-playbook << EOF
#! /bin/sh
VIRTUAL_ENV=/var/lib/zstack/virtualenv/zstacksys
if [ ! -d $VIRTUAL_ENV ]; then
    echo "Need to install zstacksys before using it"
    exit 1
fi

LANG=en_US.UTF-8
LC_ALL=en_US.utf8
export LANG LC_ALL
. ${VIRTUAL_ENV}/bin/activate

\${VIRTUAL_ENV}/bin/ansible-playbook \$@
EOF
        chmod +x /usr/bin/ansible-playbook
    fi

elif [ $tool = 'zstack-dashboard' ]; then
    UI_VIRENV_PATH=/var/lib/zstack/virtualenv/zstack-dashboard
    [ ! -z $force ] && rm -rf $UI_VIRENV_PATH
    if [ ! -d "$UI_VIRENV_PATH" ]; then
        python3.11 -m venv $UI_VIRENV_PATH
        if [ $? -ne 0 ]; then
            rm -rf $UI_VIRENV_PATH
            exit 1
        fi
    fi
    . $UI_VIRENV_PATH/bin/activate
    cd $cwd
    pip show versiontools
    if [ $? -ne 0 ]; then
        # fresh install versiontools
        echo "Installing versiontools..."
        pip install -i $pypi_path --trusted-host localhost versiontools
        if [ $? -ne 0 ]; then
            rm -rf $UI_VIRENV_PATH
            exit 1
        fi
    else
        # upgrade versiontools
        echo "Upgrading versiontools..."
        pip install -U -i $pypi_path --trusted-host localhost versiontools
        if [ $? -ne 0 ]; then
            rm -rf $UI_VIRENV_PATH
            exit 1
        fi
    fi
    pip show zstack-dashboard
    if [ $? -ne 0 ]; then
        #fresh install zstack_dashboard
        echo "Installing zstack_dashboard..."
        pip install -i $pypi_path --trusted-host localhost --upgrade zstack_dashboard-*.tar.gz
        if [ $? -ne 0 ]; then
            rm -rf $UI_VIRENV_PATH
            exit 1
        fi
    else
        #upgrae zstack_dashboard
        echo "Upgrading zstack_dashboard..."
        pip install -U -i $pypi_path --trusted-host localhost --upgrade zstack_dashboard-*.tar.gz
        if [ $? -ne 0 ]; then
            rm -rf $UI_VIRENV_PATH
            exit 1
        fi
    fi

    chmod +x /etc/init.d/zstack-dashboard

elif [ x"$tool" = x"zstack-ui" ]; then
    cd "$cwd"
    default_zstack_home='/usr/local/zstack/'
    default_ui_home="$default_zstack_home"/zstack-ui/
    zstack_home=$(echo ~zstack)
    zstack_home=${zstack_home%/}/
    ui_home="$zstack_home"/zstack-ui/
    mkdir -p "$ui_home"

    # Assume:
    # - zstack installed in /usr/local/zstacktest
    # - zstack-ui installed in /usr/local/zstack/zstack-ui
    # After upgrade, zstack-ui will be installed in /usr/local/zstacktest/zstack-ui.
    # We need to copy old ui config and certification files back
    if [ ! -f "$ui_home"/zstack-ui.war -a -f "$default_ui_home"/zstack-ui.war ]; then
      cp -rf "$default_ui_home"/* "$ui_home"
      sed -i "s|$default_zstack_home|$zstack_home|g" "$ui_home"/zstack.ui.properties
    fi

    cp -f zstack-ui.war "$ui_home"
    rm -rf "$ui_home"/tmp
    unzip zstack-ui.war -d "$ui_home"/tmp
    cp -f zstack-ui /etc/init.d/
    chmod a+x /etc/init.d/zstack-ui

    chown -R zstack:zstack "$ui_home"
else
    usage
fi
