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
    private static final String RESOURCE_ASSIGNMENT_DROP_IN = "/etc/systemd/system/zstack-kvmagent.service.d/" +
            "50-zstack-resource-assignment.conf";
    private static final String RESOURCE_ASSIGNMENT_DROP_IN_CONTENT = "[Service]\nSlice=zstack-compute.slice";
    private static final String RESOURCE_ASSIGNMENT_OUTPUT_SEPARATOR = "__ZSTACK_RESOURCE_ASSIGNMENT_CGROUP__";
    private static final String RESOURCE_ASSIGNMENT_CGROUP_V2 = "__ZSTACK_RESOURCE_ASSIGNMENT_CGROUP_V2__";

    private String username;
    private String password;
    private String privateKey;
    private String targetIp;
    private String requireKsmCheck;
    private String requireReservePorts;
    private String requireResourceAssignment;
    private int sshPort = 22;

    @Override
    public boolean needDeploy() {
        return needDeployKsmCheck()
                || needDeployReservePorts()
                || needDeployResourceAssignment();
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
            ssh.sudoCommand("cat /sys/kernel/mm/ksm/run");
            SshResult ret = ssh.setTimeout(60).runAndClose();
            if (ret.getReturnCode() != 0) {
                logger.warn(String.format("exec ssh command failed, return code: %d, stdout: %s, stderr: %s",
                        ret.getReturnCode(), ret.getStdout(), ret.getStderr()));
                return true;
            }

            boolean ksmEnabledOnHost = "1".equals(ret.getStdout());
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

    private boolean needDeployResourceAssignment() {
        if (requireResourceAssignment == null) {
            return false;
        }

        Ssh ssh = new Ssh();
        ssh.setUsername(username).setPrivateKey(privateKey)
                .setPassword(password).setPort(sshPort).setHostname(targetIp);
        try {
            ssh.sudoCommand(String.format(
                    "if [ -f /sys/fs/cgroup/cgroup.controllers ]; " +
                            "then printf '%s\\n'; fi; " +
                            "if [ -f %s ]; then cat %s; else printf '__ABSENT__'; fi; " +
                            "printf '\\n%s'; " +
                            "pid=$(systemctl show --property=MainPID --value " +
                            "zstack-kvmagent.service 2>/dev/null || true); " +
                            "if [ -n \"$pid\" ] && [ \"$pid\" != '0' ] && " +
                            "[ -r \"/proc/$pid/cgroup\" ]; then " +
                            "cat \"/proc/$pid/cgroup\"; fi",
                    RESOURCE_ASSIGNMENT_CGROUP_V2,
                    RESOURCE_ASSIGNMENT_DROP_IN,
                    RESOURCE_ASSIGNMENT_DROP_IN,
                    RESOURCE_ASSIGNMENT_OUTPUT_SEPARATOR));
            SshResult result = ssh.setTimeout(60).runAndClose();
            if (result.getReturnCode() != 0) {
                logger.warn(String.format(
                        "failed to inspect KVM Agent resource assignment, " +
                                "return code: %d, stdout: %s, stderr: %s",
                        result.getReturnCode(), result.getStdout(),
                        result.getStderr()));
                return true;
            }

            String output = result.getStdout();
            int separator = output.indexOf(RESOURCE_ASSIGNMENT_OUTPUT_SEPARATOR);
            if (separator < 0) {
                return true;
            }
            String dropIn = output.substring(0, separator).trim();
            boolean unifiedCgroupV2 = dropIn.startsWith(RESOURCE_ASSIGNMENT_CGROUP_V2);
            if (unifiedCgroupV2) {
                dropIn = dropIn.substring(RESOURCE_ASSIGNMENT_CGROUP_V2.length()).trim();
            }
            String processCgroup = output.substring(separator + RESOURCE_ASSIGNMENT_OUTPUT_SEPARATOR.length()).trim();
            boolean matches = unifiedCgroupV2
                    ? unifiedResourceAssignmentMatches(requireResourceAssignment, dropIn, processCgroup)
                    : legacyResourceAssignmentMatches(requireResourceAssignment, dropIn);
            if (!matches) {
                logger.debug(String.format(
                        "KVM Agent resource assignment does not match, " +
                                "required[%s], unifiedCgroupV2[%s], " +
                                "dropIn[%s], processCgroup[%s]",
                        requireResourceAssignment, unifiedCgroupV2,
                        dropIn, processCgroup));
            }
            return !matches;
        } finally {
            ssh.close();
        }
    }

    static boolean unifiedResourceAssignmentMatches(String required, String dropIn, String processCgroup) {
        boolean enabled = Boolean.parseBoolean(required);
        String normalizedDropIn = dropIn == null ? null : dropIn.replace("\r\n", "\n");
        boolean configured = RESOURCE_ASSIGNMENT_DROP_IN_CONTENT.equals(normalizedDropIn);
        boolean inRoleSlice = processCgroup != null && (
                processCgroup.contains("/zstack-compute.slice/")
                        || processCgroup.endsWith("/zstack-compute.slice"));
        return enabled ? configured && inRoleSlice : "__ABSENT__".equals(dropIn) && !inRoleSlice;
    }

    static boolean legacyResourceAssignmentMatches(String required, String dropIn) {
        return Boolean.parseBoolean(required) || "__ABSENT__".equals(dropIn);
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

    public String getRequireResourceAssignment() {
        return requireResourceAssignment;
    }

    public void setRequireResourceAssignment(String requireResourceAssignment) {
        this.requireResourceAssignment = requireResourceAssignment;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }
}
