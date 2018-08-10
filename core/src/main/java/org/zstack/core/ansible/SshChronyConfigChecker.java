package org.zstack.core.ansible;

import org.zstack.core.CoreGlobalProperty;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.*;

public class SshChronyConfigChecker implements AnsibleChecker {
    private static final CLogger logger = Utils.getLogger(SshChronyConfigChecker.class);
    private String username;
    private String password;
    private String privateKey;
    private String targetIp;
    private int sshPort = 22;

    @Override
    public boolean needDeploy() {
        if (!CoreGlobalProperty.SYNC_NODE_TIME) {
            return false;
        }

        Ssh ssh = new Ssh();
        ssh.setUsername(username).setPrivateKey(privateKey)
                .setPassword(password).setPort(sshPort)
                .setHostname(targetIp);
        try {
            ssh.command("chronyc sources | grep '^\\^' | awk '{print $2}'");
            SshResult ret = ssh.run();
            ssh.reset();

            String[] ips = ret.getStdout().split("\n");
            logger.debug(String.format("read chrony server ip %s from configure file", ips));

            Set<String> hostChronyIps = new HashSet<>(Arrays.asList(ips));
            Set<String> configIps = new HashSet<>(CoreGlobalProperty.CHRONY_SERVERS);

            // do not config chrony server on server host
            if (!hostChronyIps.equals(configIps) && !configIps.contains(targetIp)) {
                return true;
            }
        } finally {
            ssh.close();
        }

        return false;
    }

    @Override
    public void deleteDestFile() {

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

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }
}

