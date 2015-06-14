#!/bin/bash
pypi_path=${ZSTACK_PYPI_URL-'https://pypi.python.org/simple/'}

usage() {
    echo "usage:$0 [zstack-cli|zstack-ctl|zstack-dashboard]"
    exit 1
}

tool=$1

if [ -z $tool ]; then
  usage
fi

install_virtualenv() {
    which virtualenv &>/dev/null
    if [ $? != 0 ]; then
        pip install -i $pypi_path virtualenv
    fi
}

PWD=`dirname $0`
cd $PWD

if [ $tool = 'zstack-cli' ]; then
    install_virtualenv
    CLI_VIRENV_PATH=/var/lib/zstack/virtualenv/zstackcli
    [ ! -d $CLI_VIRENV_PATH ] && virtualenv $CLI_VIRENV_PATH
    . $CLI_VIRENV_PATH/bin/activate
    pip install -i $pypi_path --ignore-installed zstacklib-*.tar.gz apibinding-*.tar.gz zstackcli-*.tar.gz || exit 1
    chmod +x /usr/bin/zstack-cli
elif [ $tool = 'zstack-ctl' ]; then
    install_virtualenv
    CTL_VIRENV_PATH=/var/lib/zstack/virtualenv/zstackctl
    [ ! -d $CTL_VIRENV_PATH ] && virtualenv $CTL_VIRENV_PATH
    . $CTL_VIRENV_PATH/bin/activate
    pip install -i $pypi_path --ignore-installed zstackctl-*.tar.gz || exit 1
    chmod +x /usr/bin/zstack-ctl
elif [ $tool = 'zstack-dashboard' ]; then
    install_virtualenv
    CTL_VIRENV_PATH=/var/lib/zstack/virtualenv/zstack-dashboard
    [ ! -d $CTL_VIRENV_PATH ] && virtualenv $CTL_VIRENV_PATH
    . $CTL_VIRENV_PATH/bin/activate
    pip install -i $pypi_path --ignore-installed zstack_dashboard-*.tar.gz || exit 1
    chmod +x /etc/init.d/zstack-dashboard
else
    usage
fi

