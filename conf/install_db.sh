ZSTACK_YUM_REPOS=''
ZSTACK_INSTALL_LOG='/tmp/zstack_installation.log'
MYSQL_NEW_ROOT_PASSWORD='zstack.mysql.password'
MYSQL_ROOT_PASSWORD=''
MANAGEMENT_INTERFACE=`ip route | grep default | head -n 1 | cut -d ' ' -f 5`
MANAGEMENT_IP=''
NEED_KEEP_DB=''
NEED_DROP_DB=''
MYSQL_PORT='3306'
MYSQL_UI_USER_PASSWORD='zstack.ui.password'
MYSQL_USER_PASSWORD='zstack.password'
NEED_DROP_DB='y'

echo '' >$ZSTACK_INSTALL_LOG

get_mn_ip(){
    MANAGEMENT_INTERFACE=`ip route | grep default | head -n 1 | cut -d ' ' -f 5`

    if [ -z $MANAGEMENT_INTERFACE ];then
        MANAGEMENT_IP=`ip -4 addr  | grep inet | tail -1 | awk '{print $2}' | cut -f1  -d'/'`
    else
        MANAGEMENT_IP=`ip -4 addr show ${MANAGEMENT_INTERFACE} | grep inet | head -1 | awk '{print $2}' | cut -f1  -d'/'`
    fi
}

cs_gen_sshkey(){
    echo "Generate Temp SSH Key" >> $ZSTACK_INSTALL_LOG
    [ ! -d /root/.ssh ] && mkdir -p /root/.ssh && chmod 700 /root/.ssh

    rsa_key_file=$1/id_rsa
    rsa_pub_key_file=$1/id_rsa.pub
    authorized_keys_file=/root/.ssh/authorized_keys
    ssh-keygen -t rsa -N '' -f $rsa_key_file >>$ZSTACK_INSTALL_LOG 2>&1
    if [ ! -f $authorized_keys_file ]; then
        cat $rsa_pub_key_file > $authorized_keys_file
        chmod 600 $authorized_keys_file
    else
        ssh_pub_key=`cat $rsa_pub_key_file`
        grep $ssh_pub_key $authorized_keys_file >/dev/null 2>&1
        if [ $? -ne 0 ]; then
            cat $rsa_pub_key_file >> $authorized_keys_file
        fi
    fi
    if [ -x /sbin/restorecon ]; then
        /sbin/restorecon /root/.ssh /root/.ssh/authorized_keys >>$ZSTACK_INSTALL_LOG 2>&1
    fi
}

cs_install_mysql(){
    echo "Install Mysql Server" >> $ZSTACK_INSTALL_LOG
    rsa_key_file=$1/id_rsa
    if [ -z $ZSTACK_YUM_REPOS ];then
        if [ -z $MYSQL_ROOT_PASSWORD ]; then
            zstack-ctl install_db --host=$MANAGEMENT_IP --ssh-key=$rsa_key_file --root-password="$MYSQL_NEW_ROOT_PASSWORD" --debug >>$ZSTACK_INSTALL_LOG 2>&1
        else
            zstack-ctl install_db --host=$MANAGEMENT_IP --login-password="$MYSQL_ROOT_PASSWORD" --root-password="$MYSQL_NEW_ROOT_PASSWORD" --ssh-key=$rsa_key_file --debug >>$ZSTACK_INSTALL_LOG 2>&1
        fi
    else
        if [ -z $MYSQL_ROOT_PASSWORD ]; then
            zstack-ctl install_db --host=$MANAGEMENT_IP --ssh-key=$rsa_key_file --yum=$ZSTACK_YUM_REPOS --root-password="$MYSQL_NEW_ROOT_PASSWORD" >>$ZSTACK_INSTALL_LOG --debug 2>&1
        else
            zstack-ctl install_db --host=$MANAGEMENT_IP --login-password="$MYSQL_ROOT_PASSWORD" --root-password="$MYSQL_NEW_ROOT_PASSWORD" --ssh-key=$rsa_key_file --yum=$ZSTACK_YUM_REPOS --debug >>$ZSTACK_INSTALL_LOG 2>&1
        fi
    fi
    if [ $? -ne 0 ];then
        cs_clean_ssh_tmp_key $1
        exit 1
        echo "failed to install mysql server." >> $ZSTACK_INSTALL_LOG
    fi
}

cs_deploy_db(){
    echo "Initialize ZStack Database"
    if [ -z $NEED_DROP_DB ]; then
        if [ -z $NEED_KEEP_DB ]; then
            zstack-ctl deploydb --root-password="$MYSQL_NEW_ROOT_PASSWORD" --zstack-password="$MYSQL_USER_PASSWORD" --host=$MANAGEMENT_IP >>$ZSTACK_INSTALL_LOG 2>&1
        else
            zstack-ctl deploydb --root-password="$MYSQL_NEW_ROOT_PASSWORD" --zstack-password="$MYSQL_USER_PASSWORD" --host=$MANAGEMENT_IP --keep-db >>$ZSTACK_INSTALL_LOG 2>&1
        fi
    else
        zstack-ctl deploydb --root-password="$MYSQL_NEW_ROOT_PASSWORD" --zstack-password="$MYSQL_USER_PASSWORD" --host=$MANAGEMENT_IP --drop >>$ZSTACK_INSTALL_LOG 2>&1
    fi
    if [ $? -ne 0 ];then
        echo "fail to init ZStack database" >> $ZSTACK_INSTALL_LOG
        exit 1
    fi
}

cs_deploy_ui_db(){
    echo "--------test start--------\n" >> $ZSTACK_INSTALL_LOG
    echo "Initialize ZStack UI Database\n" >> $ZSTACK_INSTALL_LOG
    echo $MYSQL_PORT"\n" >> $ZSTACK_INSTALL_LOG
    echo "--------test end--------\n" >> $ZSTACK_INSTALL_LOG
    if [ -z $NEED_DROP_DB ]; then
        if [ -z $NEED_KEEP_DB ]; then
            zstack-ctl deploy_ui_db --root-password="$MYSQL_NEW_ROOT_PASSWORD" --zstack-ui-password="$MYSQL_UI_USER_PASSWORD" --host=${MANAGEMENT_IP} --port=${MYSQL_PORT} >>$ZSTACK_INSTALL_LOG 2>&1
        else
            zstack-ctl deploy_ui_db --root-password="$MYSQL_NEW_ROOT_PASSWORD" --zstack-ui-password="$MYSQL_UI_USER_PASSWORD" --host=${MANAGEMENT_IP} --port=${MYSQL_PORT} --keep-db >>$ZSTACK_INSTALL_LOG 2>&1
        fi
    else
        zstack-ctl deploy_ui_db --root-password="$MYSQL_NEW_ROOT_PASSWORD" --zstack-ui-password="$MYSQL_UI_USER_PASSWORD" --host=${MANAGEMENT_IP} --port=${MYSQL_PORT} --drop >>$ZSTACK_INSTALL_LOG 2>&1
    fi
    if [ $? -ne 0 ];then
        echo "fail to init ZStack_ui database" >> $ZSTACK_INSTALL_LOG
        exit 1
    fi
}

cs_clean_ssh_tmp_key(){
    #echo_subtitle "Clean up ssh temp key"
    rsa_pub_key_file=$1/id_rsa.pub
    ssh_pub_key=`cat $rsa_pub_key_file`
    authorized_keys_file=/root/.ssh/authorized_keys
    sed -i "\;$ssh_pub_key;d" $authorized_keys_file >>$ZSTACK_INSTALL_LOG 2>&1
    /bin/rm -rf $1
}

install_db(){
    echo "Install Database"
    #generate ssh key for install mysql by ansible remote host
    ssh_tmp_dir=`mktemp`
    /bin/rm -rf $ssh_tmp_dir
    mkdir -p $ssh_tmp_dir
    cs_gen_sshkey $ssh_tmp_dir
    #install mysql db
    cs_install_mysql $ssh_tmp_dir
    #deploy initial database
    cs_deploy_db
    #deploy initial database of zstack_ui
    cs_deploy_ui_db
    #check hostname and ip again
    cs_clean_ssh_tmp_key $ssh_tmp_dir
}

get_mn_ip
install_db
