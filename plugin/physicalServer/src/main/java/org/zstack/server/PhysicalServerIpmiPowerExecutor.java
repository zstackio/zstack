package org.zstack.server;

import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.server.PhysicalServerVO;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.SshCmdHelper;

import static org.zstack.core.Platform.operr;

public class PhysicalServerIpmiPowerExecutor {

    public void powerOn(PhysicalServerVO server, Completion completion) {
        runIpmi(server, "power-on", IPMIToolCaller::powerOn, completion);
    }

    public void powerOff(PhysicalServerVO server, Completion completion) {
        runIpmi(server, "power-off", IPMIToolCaller::powerOff, completion);
    }

    public void powerReset(PhysicalServerVO server, Completion completion) {
        runIpmi(server, "power-reset", IPMIToolCaller::powerReset, completion);
    }

    public void powerOnPxe(PhysicalServerVO server, Completion completion) {
        validate(server);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            completion.success();
            return;
        }

        IPMIToolCaller caller = IPMIToolCaller.fromPhysicalServer(server);
        if (caller.setBootPxe() != 0) {
            completion.fail(operr("failed to set PXE bootdev for PhysicalServer[uuid:%s, oobAddress:%s]",
                    server.getUuid(), server.getOobAddress()));
            return;
        }
        if (caller.powerReset() != 0) {
            completion.fail(operr("failed to power-reset for PXE boot for PhysicalServer[uuid:%s, oobAddress:%s]",
                    server.getUuid(), server.getOobAddress()));
            return;
        }
        completion.success();
    }

    public boolean hasOobCredentials(PhysicalServerVO server) {
        return server != null
                && notEmpty(server.getOobAddress())
                && notEmpty(server.getOobUsername())
                && notEmpty(server.getOobPassword());
    }

    private void runIpmi(PhysicalServerVO server, String op, IpmiAction action, Completion completion) {
        validate(server);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            completion.success();
            return;
        }
        if (action.run(IPMIToolCaller.fromPhysicalServer(server)) == 0) {
            completion.success();
            return;
        }
        completion.fail(operr("IPMI %s failed for PhysicalServer[uuid:%s, oobAddress:%s]",
                op, server.getUuid(), server.getOobAddress()));
    }

    private void validate(PhysicalServerVO server) {
        if (!hasOobCredentials(server)) {
            throw new OperationFailureException(operr(
                    "OOB credentials not configured for PhysicalServer[uuid:%s]",
                    server == null ? null : server.getUuid()));
        }
        if (server.getOobManagementType() != null && !"IPMI".equals(server.getOobManagementType())) {
            throw new OperationFailureException(operr(
                    "unsupported OOB management type[%s] for PhysicalServer[uuid:%s]",
                    server.getOobManagementType(), server.getUuid()));
        }
    }

    private boolean notEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private interface IpmiAction {
        int run(IPMIToolCaller caller);
    }

    private static class IPMIToolCaller {
        private final String interfaceToUse = "lanplus";
        private String hostname;
        private int port;
        private String username;
        private String password;

        private static IPMIToolCaller fromPhysicalServer(PhysicalServerVO server) {
            IPMIToolCaller caller = new IPMIToolCaller();
            caller.hostname = server.getOobAddress();
            caller.port = server.getOobPort() == null ? 623 : server.getOobPort();
            caller.username = server.getOobUsername();
            caller.password = server.getOobPassword();
            return caller;
        }

        private int powerOn() {
            return runWithReturnCode("chassis power on");
        }

        private int powerOff() {
            return runWithReturnCode("chassis power off");
        }

        private int powerReset() {
            return runWithReturnCode("chassis power reset");
        }

        private int setBootPxe() {
            return runWithReturnCode("chassis bootdev pxe options=efiboot");
        }

        private int runWithReturnCode(String command) {
            DebugUtils.Assert(command != null, "command should be set before execution");
            String passFile = PathUtil.createTempFileWithContent(password);
            try {
                String base = String.format("ipmitool -I %s -H %s -p %d -U %s -f %s",
                        interfaceToUse,
                        SshCmdHelper.shellQuote(hostname),
                        port,
                        SshCmdHelper.shellQuote(username),
                        SshCmdHelper.shellQuote(passFile));
                return ShellUtils.runAndReturn(String.format("%s %s", base, command)).getRetCode();
            } finally {
                PathUtil.forceRemoveFile(passFile);
            }
        }
    }
}
