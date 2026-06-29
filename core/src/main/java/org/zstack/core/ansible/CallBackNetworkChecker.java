package org.zstack.core.ansible;

import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshCmdHelper;
import org.zstack.utils.ssh.SshException;
import org.zstack.utils.ssh.SshResult;
import org.zstack.utils.network.IPv6NetworkUtils;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.StringDSL.ln;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

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
    private int callBackPort = Platform.getManagementNodeServicePort();

    private static final String EMPTY_COMMAND_OPTION = "";
    private static final String IPV6_COMMAND_OPTION = "-6 ";
    private static final String HOST_UP_PATTERN = "1 host up";
    private static final StringDSL.StringWrapper CALLBACK_CHECK_SCRIPT = ln(
            "cat /dev/null | nc {3}{2} {1} || echo {0} | sudo -S nmap {4}-sS -P0 -n -p {1} {2} 2>/dev/null | grep \"{5}\""
    );

    @Override
    public boolean needDeploy() {
        return false;
    }

    @Override
    public void deleteDestFile() {

    }

    /*
     * use nc to test connection between agent and callback,
     * if failed, use nmap to try again.
     */
    private ErrorCode useNcatAndNmapToTestConnection(Ssh ssh) {
        String srcScript = buildCallbackCheckScript(SshCmdHelper.shellQuote(password), callBackPort, callbackIp);

        ssh.sudoCommand(srcScript);
        SshResult ret = ssh.run();
        ret.raiseExceptionIfFailed();

        return null;
    }

    public static String buildCallbackCheckScript(String password, int port, String callbackIp) {
        String callbackHost = IPv6NetworkUtils.stripHostUrlBrackets(callbackIp);
        String ipVersionOption = IPv6NetworkUtils.isIpv6Address(callbackHost) ? IPV6_COMMAND_OPTION : EMPTY_COMMAND_OPTION;

        return CALLBACK_CHECK_SCRIPT.format(password, port, callbackHost, ipVersionOption, ipVersionOption, HOST_UP_PATTERN);
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

        try {
            return useNcatAndNmapToTestConnection(ssh);
        } catch (SshException e) {
            return operr(ORG_ZSTACK_CORE_ANSIBLE_10004, e.getMessage());
        } finally {
            ssh.close();
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
