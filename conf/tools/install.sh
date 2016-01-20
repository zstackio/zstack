#!/bin/bash
current_folder=`pwd`
relative_script_parent_path=`dirname $0`
if [ ${relative_script_parent_path:0:1} = "/" ]; then
    cwd=$relative_script_parent_path
else
    cwd=$current_folder/$relative_script_parent_path
fi

pypi_path=file://$cwd/../../../static/pypi/simple

usage() {
    echo "usage:$0 [zstack-cli|zstack-ctl|zstack-dashboard]"
    exit 1
}

tool=$1

if [ -z $tool ]; then
  usage
fi

install_pip() {
    pip --version | grep 7.0.3 >/dev/null || easy_install -i $pypi_path --upgrade pip
}

install_virtualenv() {
    virtualenv --version | grep 12.1.1 >/dev/null || pip install -i $pypi_path --ignore-installed virtualenv==12.1.1
}

cd $cwd

install_pip
install_virtualenv
cd /tmp

if [ $tool = 'zstack-cli' ]; then
    CLI_VIRENV_PATH=/var/lib/zstack/virtualenv/zstackcli
    rm -rf $CLI_VIRENV_PATH && virtualenv $CLI_VIRENV_PATH
    . $CLI_VIRENV_PATH/bin/activate
    cd $cwd
    pip install -i $pypi_path --trusted-host localhost --ignore-installed zstacklib-*.tar.gz apibinding-*.tar.gz zstackcli-*.tar.gz || exit 1
    chmod +x /usr/bin/zstack-cli
elif [ $tool = 'zstack-ctl' ]; then
    CTL_VIRENV_PATH=/var/lib/zstack/virtualenv/zstackctl
    rm -rf $CTL_VIRENV_PATH && virtualenv $CTL_VIRENV_PATH
    . $CTL_VIRENV_PATH/bin/activate
    cd $cwd
    pip install -i $pypi_path --trusted-host localhost --ignore-installed zstackctl-*.tar.gz || exit 1
    chmod +x /usr/bin/zstack-ctl
elif [ $tool = 'zstack-dashboard' ]; then
    UI_VIRENV_PATH=/var/lib/zstack/virtualenv/zstack-dashboard
    rm -rf $UI_VIRENV_PATH && virtualenv $UI_VIRENV_PATH
    . $UI_VIRENV_PATH/bin/activate
    cd $cwd
    pip install -i $pypi_path --trusted-host localhost versiontools|| exit 1
    pip install -i $pypi_path --trusted-host localhost zstack_dashboard-*.tar.gz || exit 1
    chmod +x /etc/init.d/zstack-dashboard
else
    usage
fi

