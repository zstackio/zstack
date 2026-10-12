package org.zstack.kvm;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.ansible.AnsibleChecker;
import org.zstack.utils.RangeSet;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.Objects;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class KvmHostConfigChecker implements AnsibleChecker {
    private static final CLogger logger = Utils.getLogger(KvmHostConfigChecker.class);

    private String username;
    private String password;
    private String privateKey;
    private String targetIp;
    private String requireKsmCheck;
    private String requireReservePorts;
    private int sshPort = 22;

    @Override
    public boolean needDeploy() {
        return needDeployKsmCheck() || needDeployReservePorts();
    }

    private boolean needDeployKsmCheck() {
        if ("none".equals(requireKsmCheck)) {
            return false;
        }

        Ssh ssh = new Ssh();
        ssh.setUsername(username).setPrivateKey(privateKey)
                .setPassword(password).setPort(sshPort)
                .setHostname(targetIp);
        try {
            // A native config already controlled through the memory apply path
            // must not be overwritten by the legacy connect-time run switch.
            // This only routes the check; the Agent still verifies the full
            // config and live sysfs before acknowledging a policy. Even a
            // partial managed block is not permission for a legacy write.
            ssh.sudoCommand("if test -e /etc/ksmtuned.conf; then "
                    + "grep -Eq '^# (BEGIN|END) ZSTACK MEMORY OPTIMIZATION$' /etc/ksmtuned.conf; rc=$?; "
                    + "if test \"$rc\" = 0; then echo managed; "
                    + "elif test \"$rc\" = 1; then cat /sys/kernel/mm/ksm/run; else exit \"$rc\"; fi; "
                    + "else cat /sys/kernel/mm/ksm/run; fi");
            SshResult ret = ssh.setTimeout(60).runAndClose();
            if (ret.getReturnCode() != 0) {
                logger.warn(String.format("exec ssh command failed, return code: %d, stdout: %s, stderr: %s",
                        ret.getReturnCode(), ret.getStdout(), ret.getStderr()));
                return true;
            }

            if ("managed".equals(ret.getStdout().trim())) {
                return false;
            }
            boolean ksmEnabledOnHost = "1".equals(ret.getStdout().trim());
            if (ksmEnabledOnHost && "true".equals(requireKsmCheck)) {
                return false;
            }

            if (!ksmEnabledOnHost && "false".equals(requireKsmCheck)) {
                return false;
            }

            logger.debug(String.format("KSM status is %s (%s), but requireKsmCheck is %s, need to re-deploy",
                    ret.getStdout(),
                    ksmEnabledOnHost ? "enabled" : "disabled",
                    requireKsmCheck)
            );

            ssh.reset();
        } finally {
            ssh.close();
        }

        return true;
    }

    private boolean needDeployReservePorts() {
        if (Strings.isEmpty(requireReservePorts)) {
            return false;
        }

        Ssh ssh = new Ssh();
        ssh.setUsername(username).setPrivateKey(privateKey)
                .setPassword(password).setPort(sshPort)
                .setHostname(targetIp);
        try {
            ssh.sudoCommand("cat /proc/sys/net/ipv4/ip_local_reserved_ports");
            SshResult ret = ssh.setTimeout(60).runAndClose();
            if (ret.getReturnCode() != 0) {
                logger.warn(String.format("exec ssh command failed, return code: %d, stdout: %s, stderr: %s",
                        ret.getReturnCode(), ret.getStdout(), ret.getStderr()));
                return true;
            }
            String reservedPorts = ret.getStdout();
            RangeSet cur = RangeSet.valueOf(reservedPorts.trim());
            RangeSet expect = RangeSet.valueOf(requireReservePorts.trim());

            for (RangeSet.Range range : cur.getRanges()) {
                expect.closed(range.getStart(), range.getEnd());
            }

            cur.mergeAndSort();
            expect.mergeAndSort();

            if (!Objects.equals(cur.getRanges(), expect.getRanges())) {
                logger.debug(String.format("Reserved ports are not the same, need to deploy, current: %s, expect: %s",
                        cur.getRanges(), expect.getRanges()));
                return true;
            }
            return false;
        } finally {
            ssh.close();
        }
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

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public String getRequireKsmCheck() {
        return requireKsmCheck;
    }

    public void setRequireKsmCheck(String requireKsmCheck) {
        this.requireKsmCheck = requireKsmCheck;
    }

    public String getRequireReservePorts() {
        return requireReservePorts;
    }

    public void setRequireReservePorts(String requireReservePorts) {
        this.requireReservePorts = requireReservePorts;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }
}
