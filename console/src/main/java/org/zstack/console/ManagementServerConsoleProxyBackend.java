package org.zstack.console;

import org.apache.commons.io.FileUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleConstant;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.salt.SaltFacade;
import org.zstack.core.salt.SaltRunner;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.console.*;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.Bucket;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ManagementServerConsoleProxyBackend extends AbstractConsoleProxyBackend {
    private static final CLogger logger = Utils.getLogger(ManagementServerConsoleProxyBackend.class);
    private int agentPort = 7758;
    private String agentPackageName = ConsoleGlobalPropery.AGENT_PACKAGE_NAME;
    private boolean connected = false;

    protected ConsoleProxy getConsoleProxy(VmInstanceInventory vm, ConsoleProxyVO vo) {
        return new ConsoleProxyBase(vo, getAgentPort());
    }

    @Override
    protected ConsoleProxy getConsoleProxy(SessionInventory session, VmInstanceInventory vm) {
        String mgmtIp = CoreGlobalProperty.UNIT_TEST_ON ? "127.0.0.1" : Platform.getManagementServerIp();
        ConsoleProxyInventory inv = new ConsoleProxyInventory();
        inv.setScheme("http");
        inv.setProxyHostname(mgmtIp);
        inv.setAgentIp("127.0.0.1");
        inv.setAgentType(getConsoleBackendType());
        inv.setToken(session.getUuid());
        inv.setVmInstanceUuid(vm.getUuid());
        return new ConsoleProxyBase(inv, getAgentPort());
    }

    private void setupPublicKey() throws IOException {
        File pubKeyFile = PathUtil.findFileOnClassPath(AnsibleConstant.RSA_PUBLIC_KEY);
        String script = PathUtil.findFileOnClassPath(AnsibleConstant.IMPORT_PUBLIC_KEY_SCRIPT_PATH, true).getAbsolutePath();

        ShellUtils.run(String.format("sh %s '%s'", script, pubKeyFile.getAbsolutePath()));
    }

    @Override
    @AsyncThread
    protected void connectAgent() {
        try {
            setupPublicKey();
            File privKeyFile = PathUtil.findFileOnClassPath("ansible/rsaKeys/id_rsa");
            String privKey = FileUtils.readFileToString(privKeyFile);

            String srcPath = PathUtil.findFileOnClassPath(String.format("ansible/consoleproxy/%s", agentPackageName), true).getAbsolutePath();
            String destPath = String.format("/var/lib/zstack/console/%s", agentPackageName);
            SshFileMd5Checker checker = new SshFileMd5Checker();
            checker.setTargetIp("127.0.0.1");
            checker.setUsername("root");
            checker.setPrivateKey(privKey);
            checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/console/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
            checker.addSrcDestPair(srcPath, destPath);

            AnsibleRunner runner = new AnsibleRunner();
            runner.setRunOnLocal(true);
            runner.setLocalPublicKey(true);
            runner.installChecker(checker);
            runner.setUsername("root");
            runner.setPrivateKey(privKey);
            runner.setAgentPort(7758);
            runner.setTargetIp(Platform.getManagementServerIp());
            runner.setPlayBookName(ANSIBLE_PLAYBOOK_NAME);
            runner.putArgument("pkg_consoleproxy", agentPackageName);
            runner.run(new Completion() {
                @Override
                public void success() {
                    connected = true;
                    logger.debug("successfully deploy console proxy agent by ansible");
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    connected = false;
                    logger.warn(String.format("failed to deploy console proxy agent by ansible, %s", errorCode));
                }
            });
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    protected boolean isAgentConnected() {
        return connected;
    }

    @Override
    public String getConsoleBackendType() {
        return ConsoleConstants.MANAGEMENT_SERVER_CONSOLE_PROXY_BACKEND;
    }


    public int getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(int agentPort) {
        this.agentPort = agentPort;
    }
}
