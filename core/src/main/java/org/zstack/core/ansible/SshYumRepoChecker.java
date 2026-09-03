package org.zstack.core.ansible;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.ManagedComponentEndpoint;
import org.zstack.core.Platform;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.List;

/**
 * Created by GuoYi on 2018-12-24.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SshYumRepoChecker implements AnsibleChecker {
    @Autowired
    protected RESTFacade restf;

    private static final CLogger logger = Utils.getLogger(SshYumRepoChecker.class);
    private String username;
    private String password;
    private String privateKey;
    private String targetIp;
    private String managementNodeIp;
    private int sshPort = 22;

    @Override
    public boolean needDeploy() {
        if (StringUtils.isEmpty(password)) {
            return true;
        }

        Ssh ssh = new Ssh();
        ssh.setUsername(username).setPrivateKey(privateKey)
                .setPassword(password).setPort(sshPort)
                .setHostname(targetIp);
        try {
            String managementNodeEndpoint = managementNodeIp;
            if (managementNodeEndpoint == null) {
                ErrorableValue<List<ManagedComponentEndpoint>> endpoints =
                        Platform.resolveManagedComponentEndpointCandidates(targetIp);
                if (!endpoints.isSuccess()) {
                    throw new OperationFailureException(endpoints.error);
                }
                ManagedComponentEndpoint endpoint = endpoints.result.get(0);
                managementNodeEndpoint = endpoint.getCurrentManagementNodeAddress();
                targetIp = endpoint.getRemoteAddress();
                ssh.setHostname(targetIp);
            }
            ssh.sudoCommand(buildYumRepoEndpointRewriteCommand(managementNodeEndpoint, restf.getPort()));
            SshResult ret = ssh.setTimeout(60).runAndClose();
            if (ret.getReturnCode() != 0) {
                logger.warn(String.format("exec ssh command failed, return code: %d, stdout: %s, stderr: %s",
                        ret.getReturnCode(), ret.getStdout(), ret.getStderr()));
                return true;
            }

            ssh.reset();
        } finally {
            ssh.close();
        }

        logger.debug("successfully configured zstack-mn.repo and qemu-kvm-ev-mn.repo in " + targetIp);
        return false;
    }

    static String buildYumRepoEndpointRewriteCommand(String managementNodeEndpoint, int restPort) {
        return String.format("sed -i '/baseurl/s#\\(\\[[^]]*\\]\\|\\([0-9]\\{1,3\\}\\.\\)\\{3\\}[0-9]\\{1,3\\}\\):\\([0-9]\\+\\)#%s#g' /etc/yum.repos.d/{zstack,qemu-kvm-ev}-mn.repo",
                IPv6NetworkUtils.formatHostPort(managementNodeEndpoint, restPort));
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

    public void setManagementNodeIp(String managementNodeIp) {
        this.managementNodeIp = managementNodeIp;
    }
}
