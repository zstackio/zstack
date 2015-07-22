package org.zstack.core.ansible;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AnsibleRunner {
    private static final CLogger logger = Utils.getLogger(AnsibleRunner.class);

    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private CloudBus bus;

    private static ReentrantLock lock = new ReentrantLock();
    private static String privKeyFile;
    private List<AnsibleChecker> checkers = new ArrayList<AnsibleChecker>();

    private static List<String> hostIPs = new ArrayList<String>();
    private static File hostsFile = new File(AnsibleConstant.INVENTORY_FILE);

    static {
        privKeyFile = PathUtil.findFileOnClassPath(AnsibleConstant.RSA_PRIVATE_KEY).getAbsolutePath();
    }

    {
        fullDeploy = AnsibleGlobalProperty.FULL_DEPLOY;
        try {
            if (!hostsFile.exists()) {
                hostsFile.createNewFile();
            }

            if (AnsibleGlobalProperty.KEEP_HOSTS_FILE_IN_MEMORY) {
                String ipStr = FileUtils.readFileToString(hostsFile);
                for (String ip : ipStr.split("\n")) {
                    ip = ip.trim();
                    ip = StringUtils.strip(ip, "\n\t\r");
                    if (ip.equals("")) {
                        continue;
                    }
                    hostIPs.add(ip);
                }
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private String targetIp;
    private String username;
    private String password;
    private String privateKey;
    private int sshPort = 22;
    private String playBookName;
    private Map<String, Object> arguments = new HashMap<String, Object>();
    private int agentPort;
    private boolean fullDeploy;
    private boolean localPublicKey;
    private boolean runOnLocal;

    public boolean isRunOnLocal() {
        return runOnLocal;
    }

    public void setRunOnLocal(boolean runOnLocal) {
        this.runOnLocal = runOnLocal;
    }

    public boolean isLocalPublicKey() {
        return localPublicKey;
    }

    public void setLocalPublicKey(boolean localPublicKey) {
        this.localPublicKey = localPublicKey;
    }

    public void putArgument(String key, Object value) {
        arguments.put(key, value);
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public String getPlayBookName() {
        return playBookName;
    }

    public void setPlayBookName(String playBookName) {
        this.playBookName = playBookName;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public int getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(int agentPort) {
        this.agentPort = agentPort;
    }

    public boolean isFullDeploy() {
        return fullDeploy;
    }

    public void setFullDeploy(boolean fullDeploy) {
        this.fullDeploy = fullDeploy;
    }

    private boolean findIpInHostFile(String ip) throws IOException {
        BufferedReader bf = new BufferedReader(new FileReader(AnsibleConstant.INVENTORY_FILE));
        String line;

        try {
            while ((line = bf.readLine()) != null) {
                line = StringUtils.strip(line.trim(), "\t\r\n");
                if (line.equals(ip.trim())) {
                    return true;
                }
            }

            return false;
        } finally {
            bf.close();
        }
    }

    private void setupHostsFile() throws IOException {
        lock.lock();
        try {
            if (AnsibleGlobalProperty.KEEP_HOSTS_FILE_IN_MEMORY) {
                if (!hostIPs.contains(targetIp)) {
                    hostIPs.add(targetIp);
                    FileUtils.writeStringToFile(hostsFile, StringUtils.join(hostIPs, "\n"), false);
                    logger.debug(String.format("add target ip[%s] to %s", targetIp, AnsibleConstant.INVENTORY_FILE));
                }
            } else {
                if (!findIpInHostFile(targetIp)) {
                    FileUtils.writeStringToFile(hostsFile, String.format("%s\n", targetIp), true);
                    logger.debug(String.format("add target ip[%s] to %s", targetIp, AnsibleConstant.INVENTORY_FILE));
                } else {
                    logger.debug(String.format("found target ip[%s] in %s", targetIp, AnsibleConstant.INVENTORY_FILE));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void setupPublicKey() throws IOException {
        File pubKeyFile = PathUtil.findFileOnClassPath(AnsibleConstant.RSA_PUBLIC_KEY);
        String script = PathUtil.findFileOnClassPath(AnsibleConstant.IMPORT_PUBLIC_KEY_SCRIPT_PATH, true).getAbsolutePath();
        if (localPublicKey) {
            ShellUtils.run(String.format("sh %s %s", script, pubKeyFile.getAbsolutePath()));
        } else {
            Ssh ssh = new Ssh();
            ssh.setHostname(targetIp).setPassword(password).setPort(sshPort).setPrivateKey(privateKey).setUsername(username);
            String destName = String.format("/tmp/zstackimportpublickey%s.sh", Platform.getUuid());
            String destKeyName = String.format("/tmp/zstackpublickey%s", Platform.getUuid());
            ssh.scp(script, destName)
                    .scp(pubKeyFile.getAbsolutePath(), destKeyName)
                    .command(String.format("sh %s %s; rm -f %s ; rm -f %s", destName, destKeyName, destKeyName, destName));
            ssh.runErrorByExceptionAndClose();
        }
    }

    private void callAnsible(final Completion completion) {
        RunAnsibleMsg msg = new RunAnsibleMsg();
        msg.setTargetIp(targetIp);
        msg.setPrivateKeyFile(privKeyFile);
        msg.setArguments(arguments);
        msg.setPlayBookName(playBookName);
        if (runOnLocal) {
            bus.makeLocalServiceId(msg, AnsibleConstant.SERVICE_ID);
        } else {
            bus.makeTargetServiceIdByResourceUuid(msg, AnsibleConstant.SERVICE_ID, targetIp);
        }
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    cleanup();
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private boolean runChecker() {
        for (AnsibleChecker checker : checkers) {
            if (checker.needDeploy()) {
                logger.debug(String.format("checker[%s] reports deploy is needed", checker.getClass()));
                return true;
            }
        }

        return false;
    }

    private boolean isNeedRun() {
        if (isFullDeploy()) {
            logger.debug(String.format("Ansible.fullDeploy is set, run ansible anyway"));
            return true;
        }

        boolean changed = asf.isModuleChanged(playBookName);
        if (changed) {
            logger.debug(String.format("ansible module[%s] changed, run ansible", playBookName));
            return true;
        }

        if (agentPort != 0) {
            boolean opened = NetworkUtils.isRemotePortOpen(targetIp, agentPort, (int) TimeUnit.SECONDS.toMillis(5));
            if (!opened) {
                logger.debug(String.format("agent port[%s] on target ip[%s] is not opened, run ansible[%s]", agentPort, targetIp, playBookName));
                return true;
        }

            if (runChecker()) {
                return true;
            }

            logger.debug(String.format("agent port[%s] on target ip[%s] is opened, ansible module[%s] is not changed, skip to run ansible", agentPort, targetIp, playBookName));
            return false;
        }

        logger.debug("agent port is not set, run ansible anyway");
        return true;
    }

    private void cleanup() {
        // deleting source files. Then next time ansible is called, AnsibleChecker returns false that lets ansible run
        for (AnsibleChecker checker : checkers) {
            checker.deleteDestFile();
        }
    }

    public void run(Completion completion) {
        try {
            if (!isNeedRun()) {
                completion.success();
                return;
            }

            putArgument("pip_url", String.format("http://%s:8080/zstack/static/pypi/simple", Platform.getManagementServerIp()));
            putArgument("trusted_host", Platform.getManagementServerIp());

            logger.debug(String.format("starts to run ansbile[%s]", playBookName));
            setupHostsFile();
            setupPublicKey();
            callAnsible(completion);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public List<AnsibleChecker> getCheckers() {
        return checkers;
    }

    public void installChecker(AnsibleChecker checker) {
        checkers.add(checker);
    }
}
