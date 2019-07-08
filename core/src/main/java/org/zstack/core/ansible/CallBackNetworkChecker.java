package org.zstack.core.ansible;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;
import org.zstack.utils.ssh.SshResult;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.StringDSL.ln;

/**
 * Created by mingjian.deng on 2019/6/12.
 */
public class CallBackNetworkChecker implements AnsibleChecker {
    private static final CLogger logger = Utils.getLogger(CallBackNetworkChecker.class);

    private String username;
    private String password;
    private String targetIp;
    private String privateKey;
    private int port = 22;

    private String callbackIp = Platform.getManagementServerIp();
    private int callBackPort = 8080;

    private static StringDSL.StringWrapper script = ln(
            "sudo nmap -sS -P0 -n -p {0} {1}|grep \"1 host up\""
    );

    @Override
    public boolean needDeploy() {
        return false;
    }

    @Override
    public void deleteDestFile() {

    }

    @Override
    public ErrorCode stopAnsible() {
        if (CoreGlobalProperty.UNIT_TEST_ON || !AnsibleGlobalConfig.CHECK_MANAGEMENT_CALLBACK.value(Boolean.class)) {
            return null;
        }
        Ssh ssh = new Ssh();
        ssh.setUsername(username).setPrivateKey(privateKey)
                .setPassword(password).setPort(port)
                .setHostname(targetIp);
        String srcScript = script.format(callBackPort, callbackIp);
        try {
            SshResult ret = ssh.shell(srcScript).setTimeout(5).runAndClose();
            ret.raiseExceptionIfFailed();

            if (StringUtils.isEmpty(ret.getStdout())) {
                return operr("cannot nmap from agent: %s to callback address: %s:%s", targetIp, callbackIp, callBackPort);
            }

            logger.debug(ret.getStdout());
            return null;
        } catch (SshException e) {
            return operr(e.getMessage());
        }
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public String getCallbackIp() {
        return callbackIp;
    }

    public void setCallbackIp(String callbackIp) {
        this.callbackIp = callbackIp;
    }

    public int getCallBackPort() {
        return callBackPort;
    }

    public void setCallBackPort(int callBackPort) {
        this.callBackPort = callBackPort;
    }

}
