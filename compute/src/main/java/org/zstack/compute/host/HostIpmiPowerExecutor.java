package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostIpmiVO;
import org.zstack.header.host.HostPowerStatus;
import org.zstack.header.host.HostVO;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import static org.zstack.core.Platform.operr;

/**
 * @Author : jingwang
 * @create 2023/5/5 6:56 PM
 */
public abstract class HostIpmiPowerExecutor implements HostPowerExecutor {
    @Autowired
    DatabaseFacade dbf;

    private static final CLogger logger = Utils.getLogger(HostIpmiPowerExecutor.class);

    @Override
    public void powerOff(HostVO host, Boolean force, Completion completion) {
        ErrorCode err = powerOff(host, force);
        if (err == null) {
            completion.success();
        } else {
            completion.fail(err);
        }
    }

    private ErrorCode powerOff(HostVO host, Boolean force) {
        if (HostPowerStatus.POWER_OFF.equals(refreshHostPowerStatus(host).getIpmiPowerStatus())) {
            logger.debug(String.format("host[%s:%d] is already powered off",
                    host.getIpmi().getIpmiAddress(), host.getIpmi().getIpmiPort()));
            return null;
        }

        final IPMIToolCaller caller = IPMIToolCaller.fromHostVo(host);
        int ret;
        if (force) {
            ret = caller.powerOff();
        } else {
            ret = caller.powerSoft();
        }

        if (0 == ret) {
            return null;
        } else {
            return operr("power off host[%s] by ipmi failed.", host.getUuid());
        }
    }

    public HostIpmiVO refreshHostPowerStatus(HostVO host) {
        String status = getPowerStatus(host);
        HostIpmiVO ipmi = host.getIpmi();
        ipmi.setIpmiPowerStatus(status);
        return dbf.updateAndRefresh(ipmi);
    }

    @Override
    public void powerOn(HostVO host, Completion completion) {
        ErrorCode err = powerOn(host);
        if (err == null) {
            completion.success();
        } else {
            completion.fail(err);
        }
    }

    private ErrorCode powerOn(HostVO host) {
        if (HostPowerStatus.POWER_ON.equals(refreshHostPowerStatus(host).getIpmiPowerStatus())) {
            logger.debug(String.format("host[%s:%d] is already powered on",
                    host.getIpmi().getIpmiAddress(), host.getIpmi().getIpmiPort()));
            return null;
        }

        final IPMIToolCaller caller = IPMIToolCaller.fromHostVo(host);
        int ret = caller.powerOn();
        if (0 == ret) {
            return null;
        } else {
            return operr("power off host[%s] by ipmi failed.", host.getUuid());
        }
    }

    @Override
    public void powerReset(HostVO host, Completion completion) {
        ErrorCode err = powerReset(host);
        if (err == null) {
            completion.success();
        } else {
            completion.fail(err);
        }
    }

    private ErrorCode powerReset(HostVO host) {
        if (HostPowerStatus.POWER_OFF.equals(refreshHostPowerStatus(host).getIpmiPowerStatus())) {
            return operr(String.format("power reset host[%s:%d] failed. because host is already powered off",
                    host.getIpmi().getIpmiAddress(), host.getIpmi().getIpmiPort()));
        }

        final IPMIToolCaller caller = IPMIToolCaller.fromHostVo(host);
        int ret = caller.powerReset();
        if (0 == ret) {
            return null;
        } else {
            return operr("power off host[%s] by ipmi failed.", host.getUuid());
        }
    }

    @Override
    public String getPowerStatus(HostVO host) {
        HostIpmiVO ipmi = host.getIpmi();
        return getPowerStatus(ipmi);
    }

    public static String getPowerStatus(HostIpmiVO ipmi) {
        if (null == ipmi || null == ipmi.getIpmiAddress()) {
            return HostPowerStatus.POWER_UNKNOWN;
        }

        if (null == ipmi.getIpmiUsername() || null == ipmi.getIpmiPassword()) {
            return HostPowerStatus.UN_CONFIGURED;
        }

        ShellResult rst = IPMIToolCaller.fromHostIpmiVo(ipmi).status();
        if (rst.getRetCode() == 0) {
            if (rst.getStdout().trim().equals("Chassis Power is on")) {
                return HostPowerStatus.POWER_ON;
            } else if (rst.getStdout().trim().equals("Chassis Power is off")) {
                return HostPowerStatus.POWER_OFF;
            } else {
                return HostPowerStatus.POWER_UNKNOWN;
            }
        } else {
            return HostPowerStatus.POWER_UNKNOWN;
        }
    }

    protected static class IPMIToolCaller {
        private final String interfaceToUse = "lanplus";
        public String hostname;
        public int port;
        public String username;
        public String password;
        public String passFile;

        public static IPMIToolCaller fromHostIpmiVo(HostIpmiVO ipmiVo) {
            IPMIToolCaller caller = new IPMIToolCaller();
            caller.hostname = ipmiVo.getIpmiAddress();
            caller.port = ipmiVo.getIpmiPort();
            caller.username = ipmiVo.getIpmiUsername();
            caller.password = ipmiVo.getIpmiPassword();
            return caller;
        }

        public static IPMIToolCaller fromHostVo(HostVO hostVO) {
            return fromHostIpmiVo(hostVO.getIpmi());
        }

        private String buildBaseCmd() {
            passFile = PathUtil.createTempFileWithContent(password);
            return String.format("ipmitool -I %s -H '%s' -p '%d' -U '%s' -f '%s'",
                    interfaceToUse, hostname, port, username, passFile);
        }

        private ShellResult status() {
            return runWithResult("chassis power status");
        }

        public int powerOff() {
            return runWithReturnCode("chassis power off");
        }

        public int powerSoft() {
            return runWithReturnCode("chassis power soft");
        }

        public int powerOn() {
            return runWithReturnCode("chassis power on");
        }

        public int powerReset() {
            return runWithReturnCode("chassis power reset");
        }

        @Deferred
        public ShellResult runWithResult(String command) {
            DebugUtils.Assert(command != null, "command should be set before execution");
            Defer.defer(() -> PathUtil.forceRemoveFile(passFile));
            return ShellUtils.runAndReturn(String.format("%s %s", buildBaseCmd(), command));
        }

        @Deferred
        public int runWithReturnCode(String command) {
            DebugUtils.Assert(command != null, "command should be set before execution");
            Defer.defer(() -> PathUtil.forceRemoveFile(passFile));
            return ShellUtils.runAndReturn(String.format("%s %s", buildBaseCmd(), command)).getRetCode();
        }
    }
}
