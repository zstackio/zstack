package org.zstack.kvm;

import org.zstack.core.logging.LogLabel;

/**
 * Created by xing5 on 2016/6/3.
 */
public class KVMHostLabel {
    @LogLabel(messages = {
            "en_US = ping following DNS names[{0}] to make sure the network connection is working",
            "zh_CN = ping下列DNS域名：{0}，以确保物理机网络可以连通外部网络"
    })
    public static final String ADD_HOST_CHECK_DNS = "add.host.kvm.checkDNS";

    @LogLabel(messages = {
            "en_US = check if the host can reach the management node's IP",
            "zh_CN = 检查物理机是否可以访问管理节点IP地址"
    })
    public static final String ADD_HOST_CHECK_PING_MGMT_NODE = "add.host.kvm.pingManagementNode";

    @LogLabel(messages = {
            "en_US = call Ansible to prepare environment on the host, it may take a few minutes ...",
            "zh_CN = 调用Ansible安装物理机环境，这可能需要花费数分钟时间"
    })
    public static final String CALL_ANSIBLE = "add.host.kvm.callAnsible";

    @LogLabel(messages = {
            "en_US = the KVM agent is installed, now wait for it connecting",
            "zh_CN = KVM代理安装完成，正在连接代理"
    })
    public static final String ECHO_AGENT = "add.host.kvm.echoAgent";

    @LogLabel(messages = {
            "en_US = collect libraries(e.g. libvirt, qemu) information from the host",
            "zh_CN = 收集物理机系统中依赖包（例如Libvrit，QEMU）信息"
    })
    public static final String COLLECT_HOST_FACTS = "add.host.kvm.collectFacts";

    @LogLabel(messages = {
            "en_US = configuring iptables on the host",
            "zh_CN = 配置物理机防火墙信息"
    })
    public static final String PREPARE_FIREWALL = "add.host.kvm.prepareFirewall";

    @LogLabel(messages = {
            "en_US = prepare L2 networks on the host",
            "zh_CN = 配置物理机二层网络"
    })
    public static final String PREPARE_L2_NETWORK = "add.host.kvm.prepareL2Network";

    @LogLabel(messages = {
            "en_US = sync host CPU/memory capacity to the database",
            "zh_CN = 同步物理机CPU、内存容量到数据库"
    })
    public static final String SYNC_HOST_CAPACITY = "add.host.kvm.syncCapacity";

    @LogLabel(messages = {
            "en_US = sync states of VMs running on the host",
            "zh_CN = 同步物理机上虚拟机状态到数据库"
    })
    public static final String SYNC_VM_STATE = "add.host.kvm.vmSync";

    @LogLabel(messages = {
            "en_US = can't launch ansible to host [{0}], please check you ssh connection to host",
            "zh_CN = 无法在主机 {0} 上启动ansible, 请检查主机的ssh是否可连接"
    })
    public static final String START_ANSIBLE_FAIL = "ansible.start.error";

    @LogLabel(messages = {
            "en_US = start to install [{0}] ......",
            "zh_CN = 开始安装 {0} ......."
    })
    public static final String START_INSTALL_AGENT = "ansible.install.agent";


    @LogLabel(messages = {
            "en_US = install python module [{0}] successfully",
            "zh_CN = 成功安装python模块 {0}"
    })
    public static final String INSTALL_AGENT_SUCC = "ansible.install.agent.succ";


    @LogLabel(messages = {
            "en_US = only need to upgrade [{0}]",
            "zh_CN = 只需要升级 {0}"
    })
    public static final String UPGRADE_AGENT_SUCC = "ansible.upgrade.agent";


    @LogLabel(messages = {
            "en_US = starting enable yum repo [{0}] ... ",
            "zh_CN = 开始启用yum仓库 {0} ... "
    })
    public static final String ENABLE_YUM_REPO = "ansible.yum.enable.repo";


    @LogLabel(messages = {
            "en_US = yum enable repo [{0}] successfully",
            "zh_CN = 启用yum仓库 {0} 成功"
    })
    public static final String ENABLE_YUM_REPO_SUCC = "ansible.yum.enable.repo.succ";


    @LogLabel(messages = {
            "en_US = enable yum repo [{0}] failed",
            "zh_CN = 启用yum仓库 {0} 失败"
    })
    public static final String ENABLE_YUM_REPO_FAIL= "ansible.yum.enable.repo.fail";


    @LogLabel(messages = {
            "en_US = searching yum package [{0}] ... ",
            "zh_CN = 正在通过yum搜索软件包 {0} ..."
    })
    public static final String YUM_SEARCH_PKG = "ansible.yum.search.pkg";


    @LogLabel(messages = {
            "en_US = the package [{0}] exist in system",
            "zh_CN = 软件包 {0} 在系统中已存在"
    })
    public static final String YUM_SEARCH_PKG_SUCC = "ansible.yum.search.pkg.succ";


    @LogLabel(messages = {
            "en_US = the package [{0}] not exist in system",
            "zh_CN = 软件包 {0} 在系统中不存在"
    })
    public static final String YUM_SEARCH_PKG_FAIL = "ansible.yum.search.pkg.fail";


    @LogLabel(messages = {
            "en_US = running script: [{0}] on host: [{1}]",
            "zh_CN = 正在主机 {1} 上运行脚本: {0}"
    })
    public static final String SCRIPT_RUN = "ansible.script.run";

    @LogLabel(messages = {
            "en_US = running script: [{0}] on host: [{1}] successfully",
            "zh_CN = 主机 {1} 上运行脚本: {0} 成功"
    })
    public static final String SCRIPT_RUN_SUCC = "ansible.script.run.succ";

    @LogLabel(messages = {
            "en_US = running script: [{0}] on host: [{1}] failed",
            "zh_CN = 主机 {1} 上运行脚本: {0} 失败"
    })
    public static final String SCRIPT_RUN_FAIL = "ansible.script.run.fail";

    @LogLabel(messages = {
            "en_US = starting yum install package [{0}]",
            "zh_CN = 开始通过yum安装软件包 {0}"
    })
    public static final String YUM_START_INSTALL_PKG = "ansible.yum.start.install.pkg";

    @LogLabel(messages = {
            "en_US = package [{0}] exist in system",
            "zh_CN = 系统中已经存在软件包 {0} "
    })
    public static final String SKIP_INSTALL_PKG = "ansible.skip.install.pkg";

    @LogLabel(messages = {
            "en_US = yum installing package [{0}] ... ",
            "zh_CN = 正在通过 yum 安装软件包 {0} ... "
    })
    public static final String YUM_INSTALL_PKG = "ansible.yum.install.pkg";

    @LogLabel(messages = {
            "en_US = yum install package [{0}] successfully",
            "zh_CN = 通过 yum 安装软件包 {0} 成功"
    })
    public static final String YUM_INSTALL_PKG_SUCC = "ansible.yum.install.pkg.succ";

    @LogLabel(messages = {
            "en_US = yum install package [{0}] failed",
            "zh_CN = 通过 yum 安装软件包 {0} 失败"
    })
    public static final String YUM_INSTALL_PKG_FAIL = "ansible.yum.install.pkg.fail";


    @LogLabel(messages = {
            "en_US = yum start removing package [{0}] ... ",
            "zh_CN = 通过 yum 开始删除软件包 {0} ... "
    })
    public static final String YUM_START_REMOVE_PKG = "ansible.yum.start.remove.pkg";

    @LogLabel(messages = {
            "en_US = yum removing package [{0}] ... ",
            "zh_CN = 通过 yum 删除软件包 {0} ..."
    })
    public static final String YUM_REMOVE_PKG = "ansible.yum.remove.pkg";

    @LogLabel(messages = {
            "en_US = package [{0}] not exist in system",
            "zh_CN = 软件包 {0} 在系统中不存在"
    })
    public static final String SKIP_REMOVE_PACKAGE = "ansible.skip.remove.pkg";

    @LogLabel(messages = {
            "en_US = yum remove package [{0}] successfully",
            "zh_CN = 通过 yum 删除软件包 {0} 成功"
    })
    public static final String YUM_REMOVE_PACKAGE_SUCC = "ansible.remove.pkg.succ";

    @LogLabel(messages = {
            "en_US = yum remove package [{0}] failed",
            "zh_CN = 通过 yum 删除软件包 {0} 失败"
    })
    public static final String YUM_REMOVE_PACKAGE_FAIL = "ansible.remove.pkg.fail";

    @LogLabel(messages = {
            "en_US = check all packages [{0}] exist in system",
            "zh_CN = 检查这些软件包： {0} 是否在系统中都存在"
    })
    public static final String CHECK_PKGS_EXIST = "ansible.check.pkgs.exist";

    @LogLabel(messages = {
            "en_US = the package [{0}] exist in system",
            "zh_CN = 软件包 {0} 在系统中存在"
    })
    public static final String CHECK_PKG_EXIST_SUCC = "ansible.check.pkg.exist.succ";

    @LogLabel(messages = {
            "en_US = the package [{0}] not exist in system",
            "zh_CN = 软件包 {0} 在系统中不存在"
    })
    public static final String CHECK_PKG_EXIST_FAIL = "ansible.check.pkg.exist.fail";

    @LogLabel(messages = {
            "en_US = starting apt install package [{0}] ... ",
            "zh_CN = 开始通过 apt 安装软件包 {0}"
    })
    public static final String APT_INSTALL_PACKAGE = "ansible.apt.install.pkg.fail";

    @LogLabel(messages = {
            "en_US = apt install package [{0}] failed",
            "zh_CN = apt 安装软件包 {0} 失败"
    })
    public static final String APT_INSTALL_PACKAGE_FAIL= "ansible.apt.install.pkg.succ";

    @LogLabel(messages = {
            "en_US = apt install package [{0}] successfully",
            "zh_CN = apt 安装软件包 {0} 成功"
    })
    public static final String APT_INSTALL_PACKAGE_SUCC = "ansible.apt.install.pkg";

    @LogLabel(messages = {
            "en_US = apt install package [{0}] meet unknown issue, please check the deploy.log",
            "zh_CN = apt 安装软件包 {0} 遇到未知问题，详细内容请查看安装日志"
    })
    public static final String APT_INSTALL_PACKAGE_ISSUE = "ansible.apt.install.pkg.issue";

    @LogLabel(messages = {
            "en_US = pip installing package [{0}] ...",
            "zh_CN = 通过 pip 安装软件包 {0} ..."
    })
    public static final String PIP_INSTALL_PKG = "ansible.pip.install.pkg";

    @LogLabel(messages = {
            "en_US = pip install package [{0}] successfully",
            "zh_CN = 通过 pip 安装软件包 {0} 成功"
    })
    public static final String PIP_INSTALL_PKG_SUCC = "ansible.pip.install.pkg.succ";

    @LogLabel(messages = {
            "en_US = pip installing package [{0}] failed",
            "zh_CN = 通过 pip 安装软件包 {0} 失败"
    })
    public static final String PIP_INSTALL_PKG_FAIL = "ansible.pip.install.pkg.fail";

    @LogLabel(messages = {
            "en_US = starting set cron task [{0}] ...",
            "zh_CN = 开始通过cron设置任务 {0} ..."
    })
    public static final String CRON_SET_TASK = "ansible.cron.set.task";

    @LogLabel(messages = {
            "en_US = set cron task [{0}] successfully",
            "zh_CN = 通过cron设置任务 {0} 成功"
    })
    public static final String CRON_SET_TASK_SUCC = "ansible.cron.set.task.succ";

    @LogLabel(messages = {
            "en_US = starting set cron task [{0}] failed",
            "zh_CN = 通过cron设置任务 {0} 失败"
    })
    public static final String CRON_SET_TASK_FAIL = "ansible.cron.set.task.fail";

    @LogLabel(messages = {
            "en_US = starting copy [{0}] to [{1}] ...",
            "zh_CN = 开始复制 {0} 到 {1} ..."
    })
    public static final String COPY = "ansible.copy.start";

    @LogLabel(messages = {
            "en_US = copy [{0}] to [{1}] failed",
            "zh_CN = 复制 {0} 到 {1} 失败"
    })
    public static final String COPY_FAIL = "ansible.copy.fail";

    @LogLabel(messages = {
            "en_US = copy [{0}] to [{1}] successfully, the change status is [{2}]",
            "zh_CN = 复制 {0} 到 {1} 成功，返回的修改状态为 {2}"
    })
    public static final String COPY_SUCC = "ansible.copy.succ";

    @LogLabel(messages = {
            "en_US = starting fetch [{0}] to [{1}] ...",
            "zh_CN = 开始从 {0} 获取 {1}"
    })
    public static final String FETCH = "ansible.fetch.start";

    @LogLabel(messages = {
            "en_US = fetch from [{0}] to [{1}] failed",
            "zh_CN = 从 {0} 获取 {1} 失败"
    })
    public static final String FETCH_FAIL = "ansible.fetch.fail";

    @LogLabel(messages = {
            "en_US = fetch [{0}] to [{1}], the change status is [{2}]",
            "zh_CN = 从 {0} 获取 {1} 成功，返回的修改状态为 {2}"
    })
    public static final String FETCH_SUCC = "ansible.fetch.succ";

    @LogLabel(messages = {
            "en_US = starting run remote task: [{0}]  ...",
            "zh_CN = 开始执行操作：{0} ..."
    })
    public static final String COMMAND = "ansible.command";

    @LogLabel(messages = {
            "en_US = run remote task: [{0}] successfully ",
            "zh_CN = 执行操作：{0} 成功"
    })
    public static final String COMMAND_SUCC = "ansible.command.succ";

    @LogLabel(messages = {
            "en_US = run remote task: [{0}] failed",
            "zh_CN = 执行操作：{0} 失败"
    })
    public static final String COMMAND_FAIL = "ansible.command.fail";

    @LogLabel(messages = {
            "en_US = check pip version [{0}] exist ...",
            "zh_CN = 检查版本号为 {0} 的 pip 是否存在"
    })
    public static final String CHECK_PIP_VERSION = "ansible.check.pip.version";

    @LogLabel(messages = {
            "en_US = pip-[{0}] exist in system",
            "zh_CN = pip-{0} 在系统中存在"
    })
    public static final String CHECK_PIP_VERSION_SUCC = "ansible.check.pip.version.succ";

    @LogLabel(messages = {
            "en_US = pip-[{0}] not exist in system",
            "zh_CN = pip-{0} 在系统中不存在"
    })
    public static final String CHECK_PIP_VERSION_FAIL = "ansible.check.pip.version.fail";

    @LogLabel(messages = {
            "en_US = starting check file or dir [[{0}]] exist ... ",
            "zh_CN = 开始检查目录或者文件 {0} 是否存在 ... "
    })
    public static final String CHECK_FILE_DIR_EXIST = "ansible.check.file.dir.exist.start";

    @LogLabel(messages = {
            "en_US =  [{0}]  exist ",
            "zh_CN = {0} 在系统中存在"
    })
    public static final String CHECK_FILE_DIR_EXIST_SUCC = "ansible.check.file.dir.exist.succ";

    @LogLabel(messages = {
            "en_US =  [{0}] not exist ",
            "zh_CN = {0} 在系统中不存在"
    })
    public static final String CHECK_FILE_DIR_EXIST_FAIL = "ansible.check.file.dir.exist.fail";

    @LogLabel(messages = {
            "en_US = starting change file attribute [{0}] ... ",
            "zh_CN = 开始改变文件 {0} 的属性"
    })
    public static final String CHANGE_FILE_ATTRIBUTE = "ansible.change.file.attribute.start";

    @LogLabel(messages = {
            "en_US = [{0}] changed successfully",
            "zh_CN = 改变文件 {0} 的属性成功"
    })
    public static final String CHANGE_FILE_ATTRIBUTE_SUCC = "ansible.change.file.attribute.succ";

    @LogLabel(messages = {
            "en_US = [{0}] not be changed",
            "zh_CN = 改变文件 {0} 的属性失败"
    })
    public static final String CHANGE_FILE_ATTRIBUTE_FAIL = "ansible.change.file.attribute.fail";

    @LogLabel(messages = {
            "en_US = starting get remote host [{0}] info ... ",
            "zh_CN = 开始获得远程主机 {0} 的信息 ..."
    })
    public static final String GET_HOST_INFO = "ansible.get.host.info";

    @LogLabel(messages = {
            "en_US = get remote host [{0}] info successfully",
            "zh_CN = 获得远程主机 {0} 的信息成功"
    })
    public static final String GET_HOST_INFO_SUCC = "ansible.get.host.info.succ";

    @LogLabel(messages = {
            "en_US = get remote host [{0}] info failed",
            "zh_CN = 获得远程主机 {0} 的信息失败"
    })
    public static final String GET_HOST_INFO_FAIL = "ansible.get.host.info.fail";

    @LogLabel(messages = {
            "en_US = starting update file [{0}] section [{1}] ... ",
            "zh_CN = 开始更新文件 {0} 的 {1} 章节 ... "
    })
    public static final String SET_INI_FILE = "ansible.set.ini.file";

    @LogLabel(messages = {
            "en_US = update file: [{0}] option: [{1}] value [{2}] successfully",
            "zh_CN = 更新文件 {0} 的 {1} 字段的值为 {2} 成功"
    })
    public static final String SET_INI_FILE_SUCC = "ansible.set.ini.file.succ";

    @LogLabel(messages = {
            "en_US = starting install virtualenv-[{0}] ... ",
            "zh_CN = 开始安装 virtualenv-{0} ... "
    })
    public static final String INSTALL_VIRTUAL_ENV = "ansible.check.install.virtualenv";

    @LogLabel(messages = {
            "en_US = the virtualenv-[{0}] exist in system",
            "zh_CN = virtualenv-{0} 已经在系统中存在"
    })
    public static final String INSTALL_VIRTUAL_ENV_SUCC = "ansible.check.install.virtualenv.succ";

    @LogLabel(messages = {
            "en_US = changing [{0}] service status to [{1}]",
            "zh_CN = 改变服务 {0} 的状态为 {1}"
    })
    public static final String SERVICE_STATUS = "ansible.service.status";

    @LogLabel(messages = {
            "en_US = change service [{0}] status to [{1}] failed",
            "zh_CN = 改变服务 {0} 的状态为 {1} 失败"
    })
    public static final String SERVICE_STATUS_FAIL = "ansible.service.status.fail";

    @LogLabel(messages = {
            "en_US = change service [{0}] status to [{1}] successfully",
            "zh_CN = 改变服务 {0} 的状态为 {1} 成功"
    })
    public static final String SERVICE_STATUS_SUCC = "ansible.service.status.succ";

    @LogLabel(messages = {
            "en_US = updating file [{0}]",
            "zh_CN = 更新文件 {0}"
    })
    public static final String UPDATE_FILE = "ansible.update.file";

    @LogLabel(messages = {
            "en_US = updating file [{0}] failed",
            "zh_CN = 更新文件 {0} 失败"
    })
    public static final String UPDATE_FILE_FAIL = "ansible.update.file.fail";

    @LogLabel(messages = {
            "en_US = updating file [{0}] successfully",
            "zh_CN = 更新文件 {0} 成功"
    })
    public static final String UPDATE_FILE_SUCC = "ansible.update.file.succ";

    @LogLabel(messages = {
            "en_US = set selinux status to [{0}]",
            "zh_CN = 设置 selinux 的状态为 {0}"
    })
    public static final String SET_SELINUX = "ansible.set.selinux";

    @LogLabel(messages = {
            "en_US = set selinux status to [{0}] failed",
            "zh_CN = 设置 selinux 的状态为 {0} 失败"
    })
    public static final String SET_SELINUX_FAIL = "ansible.set.selinux.fail";

    @LogLabel(messages = {
            "en_US = set selinux status to [{0}] successfully",
            "zh_CN = 设置 selinux 的状态为 {0} 成功"
    })
    public static final String SET_SELINUX_SUCC = "ansible.set.selinux.succ";

    @LogLabel(messages = {
            "en_US = updating key [{0}] to host [{1}]",
            "zh_CN = 更新秘钥 {0} 到远程主机 {1}"
    })
    public static final String UPDATE_KEY = "ansible.add.key";

    @LogLabel(messages = {
            "en_US = updat key [{0}] to host [{1}] successfully",
            "zh_CN = 更新秘钥 {0} 到远程主机 {1} 成功"
    })
    public static final String UPDATE_KEY_SUCC = "ansible.add.key.succ";

    @LogLabel(messages = {
            "en_US = updat key [{0}] to host [{1}] failed",
            "zh_CN = 更新秘钥 {0} 到远程主机 {1} 失败"
    })
    public static final String UPDATE_KEY_FAIL = "ansible.add.key.fail";

    @LogLabel(messages = {
            "en_US = starting unarchive [{0}] to [{1}] ...",
            "zh_CN = 开始解压缩 {0} 到 {1} ..."
    })
    public static final String UNARCHIVE = "ansible.unarchive";

    @LogLabel(messages = {
            "en_US = unarchive [{0}] to [{1}] failed ",
            "zh_CN = 解压缩 {0} 到 {1} 失败"
    })
    public static final String UNARCHIVE_FAIL = "ansible.unarchive.fail";

    @LogLabel(messages = {
            "en_US = unarchive [{0}] to [{1}] successfully",
            "zh_CN = 解压缩 {0} 到 {1} 成功"
    })
    public static final String UNARCHIVE_SUCC = "ansible.unarchive.succ";

    @LogLabel(messages = {
            "en_US = starting change kernel module [{0}] status to [{1}]",
            "zh_CN = 开始改变内核模块 {0} 状态为 {1} ... "
    })
    public static final String KERNEL_MODE_PROBE= "ansible.modprobe";

    @LogLabel(messages = {
            "en_US = change kernel module [{0}] status to [{1}] successfully",
            "zh_CN = 改变内核模块 {0} 状态为 {1} 成功 "
    })
    public static final String KERNEL_MODE_PROBE_SUCC = "ansible.modprobe.succ";

    @LogLabel(messages = {
            "en_US = change kernel module [{0}] status to [{1}] failed",
            "zh_CN = 改变内核模块 {0} 状态为 {1} 失败 "
    })
    public static final String KERNEL_MODE_PROBE_FAIL = "ansible.modprobe.fail";

    @LogLabel(messages = {
            "en_US = remove [{0}] ...",
            "zh_CN = 删除 {0} ..."
    })
    public static final String SHELL_REMOVE = "ansible.shell.remove.file";

    @LogLabel(messages = {
            "en_US = remove [{0}] successfully",
            "zh_CN = 删除 {0} 成功"
    })
    public static final String SHELL_REMOVE_SUCC = "ansible.shell.remove.file.succ";

    @LogLabel(messages = {
            "en_US = remove [{0}] failed",
            "zh_CN = 删除 {0} 失败"
    })
    public static final String SHELL_REMOVE_FAIL = "ansible.shell.remove.file.fail";

    @LogLabel(messages = {
            "en_US = deploy [{0}] repo",
            "zh_CN = 部署 {0} 源"
    })
    public static final String SHELL_DEPLOY_REPO = "ansible.shell.deploy.repo";

    @LogLabel(messages = {
            "en_US = deploy [{0}] repo successfully",
            "zh_CN = 部署 {0} 源成功"
    })
    public static final String SHELL_DEPLOY_REPO_SUCC = "ansible.shell.deploy.repo.succ";

    @LogLabel(messages = {
            "en_US = deploy [{0}] repo failed",
            "zh_CN = 部署 {0} 源失败"
    })
    public static final String SHELL_DEPLOY_REPO_FAIL = "ansible.shell.deploy.repo.failed";

    @LogLabel(messages = {
            "en_US = installing package: [{0}] ...",
            "zh_CN = 安装软件包 {0} ..."
    })
    public static final String SHELL_INSTALL_PKG = "ansible.shell.install.pkg";

    @LogLabel(messages = {
            "en_US = install package [{0}] successfully",
            "zh_CN = 安装软件包 {0} 成功"
    })
    public static final String SHELL_INSTALL_PKG_SUCC = "ansible.shell.install.pkg.succ";

    @LogLabel(messages = {
            "en_US = install package [{0}] failed",
            "zh_CN = 安装软件包 {0} 失败"
    })
    public static final String SHELL_INSTALL_PKG_FAIL = "ansible.shell.install.pkg.fail";

    @LogLabel(messages = {
            "en_US = backup file [{0}] ",
            "zh_CN = 备份文件 {0}"
    })
    public static final String SHELL_BACKUP_FILE = "ansible.shell.bakcup.file";

    @LogLabel(messages = {
            "en_US = backup file [{0}] successfully",
            "zh_CN = 备份文件 {0} 成功"
    })
    public static final String SHELL_BACKUP_FILE_SUCC = "ansible.shell.bakcup.file.succ";

    @LogLabel(messages = {
            "en_US = backup file [{0}] failed",
            "zh_CN = 备份文件 {0} 失败"
    })
    public static final String SHELL_BACKUP_FILE_FAIL = "ansible.shell.bakcup.file.fail";

    @LogLabel(messages = {
            "en_US = enable service [{0}] ",
            "zh_CN = 启用服务 {0}"
    })
    public static final String SHELL_ENABLE_SERVICE = "ansible.shell.enable.service";

    @LogLabel(messages = {
            "en_US = enable service [{0}] successfully",
            "zh_CN = 启用服务 {0} 成功"
    })
    public static final String SHELL_ENABLE_SERVICE_SUCC = "ansible.shell.enable.service.succ";

    @LogLabel(messages = {
            "en_US = enable service [{0}] failed",
            "zh_CN = 启用服务 {0} 失败"
    })
    public static final String SHELL_ENABLE_SERVICE_FAIL = "ansible.shell.enable.service.fail";

    @LogLabel(messages = {
            "en_US = enable kernel module [{0}] ",
            "zh_CN = 启用内核模块 {0}"
    })
    public static final String SHELL_ENABLE_MODULE = "ansible.shell.enable.module";

    @LogLabel(messages = {
            "en_US = enable kernel module [{0}] successfully",
            "zh_CN = 启用内核模块 {0} 成功"
    })
    public static final String SHELL_ENABLE_MODULE_SUCC = "ansible.shell.enable.module.succ";

    @LogLabel(messages = {
            "en_US = enable kernel module [{0}] failed",
            "zh_CN = 启用内核模块 {0} 失败"
    })
    public static final String SHELL_ENABLE_MODULE_FAIL = "ansible.shell.enable.module.fail";

    @LogLabel(messages = {
            "en_US = disable service [{0}] ",
            "zh_CN = 停用服务 {0}"
    })
    public static final String SHELL_DISABLE_SERVICE = "ansible.shell.disable.service";

    @LogLabel(messages = {
            "en_US = disable service [{0}] successfully",
            "zh_CN = 停用服务 {0} 成功"
    })
    public static final String SHELL_DISABLE_SERVICE_SUCC = "ansible.shell.disable.service.succ";

    @LogLabel(messages = {
            "en_US = disable service [{0}] failed",
            "zh_CN = 停用服务 {0} 失败"
    })
    public static final String SHELL_DISABLE_SERVICE_FAIL = "ansible.shell.disable.service.fail";

    @LogLabel(messages = {
            "en_US = restart service [{0}] ",
            "zh_CN = 重启服务 {0} "
    })
    public static final String SHELL_RESTART_SERVICE = "ansible.shell.restart.service";

    @LogLabel(messages = {
            "en_US = restart service [{0}] successfully",
            "zh_CN = 重启服务 {0} 成功"
    })
    public static final String SHELL_RESTART_SERVICE_SUCC = "ansible.shell.restart.service.succ";

    @LogLabel(messages = {
            "en_US = restart service [{0}] failed",
            "zh_CN = 重启服务 {0} 失败"
    })
    public static final String SHELL_RESTART_SERVICE_FAIL = "ansible.shell.restart.service.fail";

    @LogLabel(messages = {
            "en_US = create directory [{0}] ",
            "zh_CN = 创建目录 {0}"
    })
    public static final String SHELL_CREATE_DIR = "ansible.shell.mkdir";

    @LogLabel(messages = {
            "en_US = create directory [{0}] successfully",
            "zh_CN = 创建目录 {0} 成功"
    })
    public static final String SHELL_CREATE_DIR_SUCC = "ansible.shell.mkdir.succ";

    @LogLabel(messages = {
            "en_US = create directory [{0}] failed",
            "zh_CN = 创建目录 {0} 失败"
    })
    public static final String SHELL_CREATE_DIR_FAIL = "ansible.shell.mkdir.fail";

    @LogLabel(messages = {
            "en_US = remove libvirt default bridge ",
            "zh_CN = 删除libvirt默认网桥"
    })
    public static final String SHELL_VIRSH_DESTROY_BRIDGE = "ansible.shell.virsh.destroy.bridge";

    @LogLabel(messages = {
            "en_US = remove libvirt default bridge successfully",
            "zh_CN = 删除libvirt默认网桥成功"
    })
    public static final String SHELL_VIRSH_DESTROY_BRIDGE_SUCC = "ansible.shell.virsh.destroy.bridge.succ";

    @LogLabel(messages = {
            "en_US = remove libvirt default bridge failed",
            "zh_CN = 删除libvirt默认网桥失败"
    })
    public static final String SHELL_VIRSH_DESTROY_BRIDGE_FAIL = "ansible.shell.virsh.destroy.bridge.fail";

    @LogLabel(messages = {
            "en_US = verify virtualenv has been setup in system",
            "zh_CN = 确认virtualenv环境是否已经安装"
    })
    public static final String SHELL_CHECK_VIRTUALENV = "ansible.shell.check.virtualenv";

    @LogLabel(messages = {
            "en_US = verify virtualenv has been setup in system successfully",
            "zh_CN = virtualenv环境已安装"
    })
    public static final String SHELL_CHECK_VIRTUALENV_SUCC = "ansible.shell.check.virtualenv.succ";

    @LogLabel(messages = {
            "en_US = verify virtualenv has been setup in system failed",
            "zh_CN = virtualenv环境确认失败"
    })
    public static final String SHELL_CHECK_VIRTUALENV_FAIL = "ansible.shell.check.virtualenv.fail";

}
