package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.server.PhysicalServerAO_;
import org.zstack.header.server.PhysicalServerInventory;
import org.zstack.header.server.PhysicalServerPowerStatus;
import org.zstack.header.server.PhysicalServerState;
import org.zstack.header.server.PhysicalServerVO;
import org.zstack.header.server.ServerPoolVO;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.SshCmdHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

import static org.zstack.core.Platform.operr;

public class PhysicalServerScanner {
    private static final int MAX_SCAN_IPS = 1024;
    private static final int DEFAULT_OOB_PORT = 623;
    private static final int DEFAULT_TIMEOUT_PER_HOST = 3;

    // Test seam (UNIT_TEST_ON only): (ip, username) -> ProbeStatus override
    public static volatile BiFunction<String, String, ProbeStatus> probeOverride;

    // Test seam (UNIT_TEST_ON only): (ip, username) -> simulated PhysicalServerPowerStatus.
    // Consulted only when probeOverride returns SUCCESS; defaults to POWER_UNKNOWN if unset
    // (preserves prior behavior of legacy IT cases that only set probeOverride).
    public static volatile BiFunction<String, String, PhysicalServerPowerStatus> powerOverride;

    @Autowired
    private DatabaseFacade dbf;

    @Autowired(required = false)
    private PhysicalServerPowerTracker powerTracker;

    public ScanResult scan(ScanSpec spec) {
        validateZonePool(spec.getZoneUuid(), spec.getPoolUuid());
        List<String> ips = parseIpRange(spec.getIpRange());
        List<Credential> credentials = parseCredentials(spec.getCredentials());

        ScanResult result = new ScanResult();
        for (String ip : ips) {
            PhysicalServerVO existing = findExisting(spec.getZoneUuid(), spec.getPoolUuid(), ip);
            if (existing != null) {
                result.existingCount++;
                continue;
            }

            ProbeResult probe = probe(ip, spec.getOobPort(), credentials, spec.getTimeoutPerHost());
            if (probe.status == ProbeStatus.SUCCESS) {
                PhysicalServerVO vo = findOrCreatePhysicalServer(spec, ip, probe.credential, probe.initialPower);
                if (vo == null) {
                    result.existingCount++;
                } else {
                    result.discoveredServers.add(PhysicalServerInventory.valueOf(vo));
                    result.discoveredCount++;
                    if (powerTracker != null) {
                        powerTracker.track(vo.getUuid());
                    }
                }
            } else if (probe.status == ProbeStatus.AUTH_FAILED) {
                result.authFailedCount++;
                result.authFailedIps.add(ip);
            } else {
                result.unreachableCount++;
            }
        }

        return result;
    }

    private void validateZonePool(String zoneUuid, String poolUuid) {
        ServerPoolVO pool = dbf.findByUuid(poolUuid, ServerPoolVO.class);
        if (pool == null) {
            throw new OperationFailureException(operrf("ServerPool[uuid:%s] not found", poolUuid));
        }
        if (!pool.getZoneUuid().equals(zoneUuid)) {
            throw new OperationFailureException(operrf(
                    "ServerPool[uuid:%s] belongs to Zone[uuid:%s], but scan specifies Zone[uuid:%s]",
                    poolUuid, pool.getZoneUuid(), zoneUuid));
        }
    }

    private List<String> parseIpRange(String ipRange) {
        if (ipRange == null || ipRange.trim().isEmpty()) {
            throw new OperationFailureException(operrf("ipRange cannot be empty"));
        }

        String[] parts = ipRange.trim().split("-", -1);
        if (parts.length > 2) {
            throw new OperationFailureException(operrf("invalid ipRange[%s], expected start-end", ipRange));
        }

        String startIp = parts[0].trim();
        String endIp = parts.length == 1 ? startIp : parts[1].trim();
        if (!NetworkUtils.isIpv4Address(startIp) || !NetworkUtils.isIpv4Address(endIp)) {
            throw new OperationFailureException(operrf("invalid ipRange[%s], only IPv4 start-end is supported", ipRange));
        }

        long start = NetworkUtils.ipv4StringToLong(startIp);
        long end = NetworkUtils.ipv4StringToLong(endIp);
        if (end < start) {
            throw new OperationFailureException(operrf("invalid ipRange[%s], end IP must be greater than or equal to start IP", ipRange));
        }

        long count = end - start + 1;
        if (count > MAX_SCAN_IPS) {
            throw new OperationFailureException(operrf("ipRange[%s] contains %s IPs, exceeding the limit %s", ipRange, count, MAX_SCAN_IPS));
        }

        List<String> ips = new ArrayList<>((int) count);
        for (long ip = start; ip <= end; ip++) {
            ips.add(NetworkUtils.longToIpv4String(ip));
        }
        return ips;
    }

    private List<Credential> parseCredentials(List<Map<String, String>> rawCredentials) {
        if (rawCredentials == null || rawCredentials.isEmpty()) {
            throw new OperationFailureException(operrf("credentials cannot be empty"));
        }

        List<Credential> credentials = new ArrayList<>();
        for (Map<String, String> raw : rawCredentials) {
            if (raw == null) {
                continue;
            }
            Credential credential = new Credential(raw.get("username"), raw.get("password"));
            if (!credential.isValid()) {
                throw new OperationFailureException(operrf("credential username/password cannot be empty"));
            }
            credentials.add(credential);
        }
        if (credentials.isEmpty()) {
            throw new OperationFailureException(operrf("credentials cannot be empty"));
        }
        return credentials;
    }

    private ErrorCode operrf(String fmt, Object... args) {
        return operr(SysErrors.OPERATION_ERROR.toString(), fmt, args);
    }

    private PhysicalServerVO findExisting(String zoneUuid, String poolUuid, String ip) {
        // BMC IP is zone-globally unique; pool scope is wrong for dedup.
        // Primary key: oobAddress (the scan input is always a BMC/IPMI address).
        PhysicalServerVO byOob = Q.New(PhysicalServerVO.class)
                .eq(PhysicalServerAO_.zoneUuid, zoneUuid)
                .notNull(PhysicalServerAO_.oobAddress)
                .eq(PhysicalServerAO_.oobAddress, ip)
                .find();
        if (byOob != null) {
            return byOob;
        }
        // Legacy fallback: records created before oobAddress was populated use managementIp.
        return Q.New(PhysicalServerVO.class)
                .eq(PhysicalServerAO_.zoneUuid, zoneUuid)
                .isNull(PhysicalServerAO_.oobAddress)
                .eq(PhysicalServerAO_.managementIp, ip)
                .find();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private PhysicalServerVO findOrCreatePhysicalServer(ScanSpec spec, String ip, Credential credential,
                                                       PhysicalServerPowerStatus initialPower) {
        PhysicalServerVO existing = findExisting(spec.getZoneUuid(), spec.getPoolUuid(), ip);
        if (existing != null) {
            return null;
        }
        return createPhysicalServer(spec, ip, credential, initialPower);
    }

    private ProbeResult probe(String ip, Integer oobPort, List<Credential> credentials, Integer timeoutPerHost) {
        boolean sawAuthFailure = false;
        for (Credential credential : credentials) {
            ProbeOutcome outcome = runProbe(ip, oobPort, credential, timeoutPerHost);
            if (outcome.status == ProbeStatus.SUCCESS) {
                return ProbeResult.success(credential, outcome.power);
            }
            if (outcome.status == ProbeStatus.AUTH_FAILED) {
                sawAuthFailure = true;
            }
        }

        return sawAuthFailure ? ProbeResult.authFailed() : ProbeResult.unreachable();
    }

    private ProbeOutcome runProbe(String ip, Integer oobPort, Credential credential, Integer timeoutPerHost) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ProbeStatus status = probeOverride != null
                    ? probeOverride.apply(ip, credential.username)
                    : ProbeStatus.SUCCESS;
            PhysicalServerPowerStatus power = (status == ProbeStatus.SUCCESS && powerOverride != null)
                    ? powerOverride.apply(ip, credential.username)
                    : PhysicalServerPowerStatus.POWER_UNKNOWN;
            return new ProbeOutcome(status, power);
        }

        String passFile = PathUtil.createTempFileWithContent(credential.password);
        try {
            int timeout = timeoutPerHost == null ? DEFAULT_TIMEOUT_PER_HOST : Math.max(1, timeoutPerHost);
            int port = oobPort == null ? DEFAULT_OOB_PORT : oobPort;
            String cmd = String.format(
                    "timeout %d ipmitool -I lanplus -H %s -p %d -U %s -f %s chassis power status",
                    timeout,
                    SshCmdHelper.shellQuote(ip),
                    port,
                    SshCmdHelper.shellQuote(credential.username),
                    SshCmdHelper.shellQuote(passFile));
            ShellResult ret = ShellUtils.runAndReturn(cmd);
            if (ret.getRetCode() == 0) {
                return new ProbeOutcome(ProbeStatus.SUCCESS, PhysicalServerPowerStatusParser.parse(ret.getStdout()));
            }
            ProbeStatus failStatus = isAuthFailure(ret) ? ProbeStatus.AUTH_FAILED : ProbeStatus.UNREACHABLE;
            return new ProbeOutcome(failStatus, PhysicalServerPowerStatus.POWER_UNKNOWN);
        } finally {
            PathUtil.forceRemoveFile(passFile);
        }
    }

    private boolean isAuthFailure(ShellResult ret) {
        String combined = String.format("%s\n%s", ret.getStdout(), ret.getStderr()).toLowerCase(Locale.ROOT);
        return combined.contains("authentication")
                || combined.contains("password")
                || combined.contains("unauthorized")
                || combined.contains("privilege");
    }

    private PhysicalServerVO createPhysicalServer(ScanSpec spec, String ip, Credential credential,
                                                  PhysicalServerPowerStatus initialPower) {
        PhysicalServerVO vo = new PhysicalServerVO();
        vo.setUuid(Platform.getUuid());
        vo.setName(String.format("physical-server-%s", ip.replace('.', '-')));
        vo.setZoneUuid(spec.getZoneUuid());
        vo.setPoolUuid(spec.getPoolUuid());
        vo.setManagementIp(ip);
        vo.setArchitecture("x86_64");
        vo.setState(PhysicalServerState.Enabled);
        vo.setPowerStatus(initialPower);
        vo.setOobManagementType("IPMI");
        vo.setOobAddress(ip);
        vo.setOobPort(spec.getOobPort() == null ? DEFAULT_OOB_PORT : spec.getOobPort());
        vo.setOobUsername(credential.username);
        vo.setOobPassword(credential.password);
        return dbf.persistAndRefresh(vo);
    }

    public static class ScanSpec {
        private String zoneUuid;
        private String poolUuid;
        private String ipRange;
        private Integer oobPort;
        private List<Map<String, String>> credentials;
        private Integer timeoutPerHost;

        public String getZoneUuid() {
            return zoneUuid;
        }

        public ScanSpec setZoneUuid(String zoneUuid) {
            this.zoneUuid = zoneUuid;
            return this;
        }

        public String getPoolUuid() {
            return poolUuid;
        }

        public ScanSpec setPoolUuid(String poolUuid) {
            this.poolUuid = poolUuid;
            return this;
        }

        public String getIpRange() {
            return ipRange;
        }

        public ScanSpec setIpRange(String ipRange) {
            this.ipRange = ipRange;
            return this;
        }

        public Integer getOobPort() {
            return oobPort;
        }

        public ScanSpec setOobPort(Integer oobPort) {
            this.oobPort = oobPort;
            return this;
        }

        public List<Map<String, String>> getCredentials() {
            return credentials;
        }

        public ScanSpec setCredentials(List<Map<String, String>> credentials) {
            this.credentials = credentials;
            return this;
        }

        public Integer getTimeoutPerHost() {
            return timeoutPerHost;
        }

        public ScanSpec setTimeoutPerHost(Integer timeoutPerHost) {
            this.timeoutPerHost = timeoutPerHost;
            return this;
        }
    }

    public static class ScanResult {
        private int discoveredCount;
        private int existingCount;
        private int unreachableCount;
        private int authFailedCount;
        private List<PhysicalServerInventory> discoveredServers = new ArrayList<>();
        private List<String> authFailedIps = new ArrayList<>();

        public int getDiscoveredCount() {
            return discoveredCount;
        }

        public int getExistingCount() {
            return existingCount;
        }

        public int getUnreachableCount() {
            return unreachableCount;
        }

        public int getAuthFailedCount() {
            return authFailedCount;
        }

        public List<PhysicalServerInventory> getDiscoveredServers() {
            return Collections.unmodifiableList(discoveredServers);
        }

        public List<String> getAuthFailedIps() {
            return Collections.unmodifiableList(authFailedIps);
        }
    }

    private static class Credential {
        private final String username;
        private final String password;

        private Credential(String username, String password) {
            this.username = username == null ? null : username.trim();
            this.password = password;
        }

        private boolean isValid() {
            return username != null && !username.isEmpty() && password != null && !password.isEmpty();
        }
    }

    private static class ProbeOutcome {
        private final ProbeStatus status;
        private final PhysicalServerPowerStatus power;

        private ProbeOutcome(ProbeStatus status, PhysicalServerPowerStatus power) {
            this.status = status;
            this.power = power;
        }
    }

    private static class ProbeResult {
        private final ProbeStatus status;
        private final Credential credential;
        private final PhysicalServerPowerStatus initialPower;

        private ProbeResult(ProbeStatus status, Credential credential, PhysicalServerPowerStatus initialPower) {
            this.status = status;
            this.credential = credential;
            this.initialPower = initialPower;
        }

        private static ProbeResult success(Credential credential, PhysicalServerPowerStatus initialPower) {
            return new ProbeResult(ProbeStatus.SUCCESS, credential, initialPower);
        }

        private static ProbeResult authFailed() {
            return new ProbeResult(ProbeStatus.AUTH_FAILED, null, PhysicalServerPowerStatus.POWER_UNKNOWN);
        }

        private static ProbeResult unreachable() {
            return new ProbeResult(ProbeStatus.UNREACHABLE, null, PhysicalServerPowerStatus.POWER_UNKNOWN);
        }
    }

    public enum ProbeStatus {
        SUCCESS,
        AUTH_FAILED,
        UNREACHABLE
    }
}
