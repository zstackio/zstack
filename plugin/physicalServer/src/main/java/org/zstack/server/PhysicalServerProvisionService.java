package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.server.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.zstack.core.Platform.operr;

public class PhysicalServerProvisionService {
    private static final CLogger logger = Utils.getLogger(PhysicalServerProvisionService.class);
    private static final Pattern MAC_PATTERN = Pattern.compile("(?i)([0-9a-f]{2}(:[0-9a-f]{2}){5})");

    @Autowired
    private DatabaseFacade dbf;

    @Autowired(required = false)
    private List<ProvisionProvider> providerList = Collections.emptyList();

    public void startProvisioning(APIProvisionPhysicalServerMsg msg,
                                  String accountUuid,
                                  String jobUuid,
                                  ProvisionPhase startPhase,
                                  ReturnValueCompletion<ProvisionResult> completion) {
        PhysicalServerVO server = dbf.findByUuid(msg.getServerUuid(), PhysicalServerVO.class);
        if (server == null) {
            completion.fail(operrf("PhysicalServer[uuid:%s] not found", msg.getServerUuid()));
            return;
        }

        PhysicalServerProvisionNetworkVO network = dbf.findByUuid(
                msg.getNetworkUuid(), PhysicalServerProvisionNetworkVO.class);
        if (network == null) {
            completion.fail(operrf("ProvisionNetwork[uuid:%s] not found", msg.getNetworkUuid()));
            return;
        }

        if (network.getState() != ProvisionNetworkState.Enabled) {
            completion.fail(operrf("ProvisionNetwork[uuid:%s] is not Enabled", network.getUuid()));
            return;
        }

        if (!network.getZoneUuid().equals(server.getZoneUuid())) {
            completion.fail(operrf(
                    "ProvisionNetwork[uuid:%s] belongs to Zone[uuid:%s], but PhysicalServer[uuid:%s] belongs to Zone[uuid:%s]",
                    network.getUuid(), network.getZoneUuid(), server.getUuid(), server.getZoneUuid()));
            return;
        }

        if (server.getPoolUuid() == null) {
            completion.fail(operrf("PhysicalServer[uuid:%s] is not assigned to any ServerPool", server.getUuid()));
            return;
        }

        boolean attached = Q.New(PhysicalServerProvisionNetworkPoolRefVO.class)
                .eq(PhysicalServerProvisionNetworkPoolRefVO_.networkUuid, network.getUuid())
                .eq(PhysicalServerProvisionNetworkPoolRefVO_.poolUuid, server.getPoolUuid())
                .isExists();
        if (!attached) {
            completion.fail(operrf(
                    "ProvisionNetwork[uuid:%s] is not attached to PhysicalServer[uuid:%s]'s ServerPool[uuid:%s]",
                    network.getUuid(), server.getUuid(), server.getPoolUuid()));
            return;
        }

        if (isPxe(network.getType()) && hasNoOobCredentials(server)) {
            completion.fail(operrf("PhysicalServer[uuid:%s] has no OOB/IPMI credentials for PXE provision",
                    server.getUuid()));
            return;
        }

        String provisionNicMac = resolveProvisionNicMac(server.getUuid(), msg.getProvisionNicMac());
        if (!isBlank(msg.getProvisionNicMac()) && isBlank(provisionNicMac)) {
            completion.fail(operrf("PhysicalServer[uuid:%s] provision NIC[mac:%s] was not found in discovered hardware",
                    server.getUuid(), msg.getProvisionNicMac()));
            return;
        }

        ProvisionProvider provider = providers().get(network.getType());
        if (provider == null) {
            completion.fail(operrf("no ProvisionProvider registered for ProvisionNetworkType[%s]", network.getType().toString()));
            return;
        }

        ProvisionRequest request = new ProvisionRequest()
                .setServerUuid(msg.getServerUuid())
                .setNetworkUuid(msg.getNetworkUuid())
                .setOsImageUuid(msg.getOsImageUuid())
                .setOsDistribution(msg.getOsDistribution())
                .setKickstartTemplate(msg.getKickstartTemplate())
                .setProvisionNicMac(msg.getProvisionNicMac())
                .setCustomParams(msg.getCustomParams())
                .setAccountUuid(accountUuid)
                .setStartPhase(startPhase)
                .setTarget(buildTarget(server, network, msg, provisionNicMac, jobUuid));

        logger.debug(String.format("start provisioning PhysicalServer[uuid:%s] with ProvisionNetwork[uuid:%s, type:%s]",
                server.getUuid(), network.getUuid(), network.getType()));
        provider.startProvisioning(request, completion);
    }

    private boolean isPxe(ProvisionNetworkType type) {
        return type == ProvisionNetworkType.GATEWAY_PXE || type == ProvisionNetworkType.STANDALONE_PXE;
    }

    private boolean hasNoOobCredentials(PhysicalServerVO server) {
        return isBlank(server.getOobManagementType())
                || isBlank(server.getOobAddress())
                || isBlank(server.getOobUsername())
                || isBlank(server.getOobPassword());
    }

    private boolean provisionNicExists(String serverUuid, String mac) {
        return Q.New(PhysicalServerHardwareDetailVO.class)
                .eq(PhysicalServerHardwareDetailVO_.serverUuid, serverUuid)
                .eq(PhysicalServerHardwareDetailVO_.type, "NIC")
                .like(PhysicalServerHardwareDetailVO_.extraInfo, "%" + mac + "%")
                .isExists();
    }

    private String resolveProvisionNicMac(String serverUuid, String requestedMac) {
        if (!isBlank(requestedMac)) {
            return provisionNicExists(serverUuid, requestedMac) ? requestedMac : null;
        }

        List<PhysicalServerHardwareDetailVO> nics = Q.New(PhysicalServerHardwareDetailVO.class)
                .eq(PhysicalServerHardwareDetailVO_.serverUuid, serverUuid)
                .eq(PhysicalServerHardwareDetailVO_.type, "NIC")
                .list();
        for (PhysicalServerHardwareDetailVO nic : nics) {
            String extraInfo = nic.getExtraInfo();
            if (isPrimaryProvisionNic(extraInfo)) {
                String mac = findMac(extraInfo);
                if (!isBlank(mac)) {
                    return mac;
                }
            }
        }
        for (PhysicalServerHardwareDetailVO nic : nics) {
            String mac = findMac(nic.getExtraInfo());
            if (!isBlank(mac)) {
                return mac;
            }
        }
        return null;
    }

    private boolean isPrimaryProvisionNic(String extraInfo) {
        if (isBlank(extraInfo)) {
            return false;
        }
        String normalized = extraInfo.toLowerCase();
        return normalized.contains("primary") && normalized.contains("true")
                || normalized.contains("provision") && normalized.contains("true");
    }

    private String findMac(String value) {
        if (isBlank(value)) {
            return null;
        }
        Matcher matcher = MAC_PATTERN.matcher(value);
        return matcher.find() ? matcher.group(1).toLowerCase() : null;
    }

    private PhysicalServerProvisionTarget buildTarget(PhysicalServerVO server,
                                                      PhysicalServerProvisionNetworkVO network,
                                                      APIProvisionPhysicalServerMsg msg,
                                                      String provisionNicMac,
                                                      String jobUuid) {
        return new PhysicalServerProvisionTarget()
                .setServerUuid(server.getUuid())
                .setNetworkUuid(network.getUuid())
                .setManagementIp(server.getManagementIp())
                .setOobAddress(server.getOobAddress())
                .setOobPort(server.getOobPort())
                .setOobUsername(server.getOobUsername())
                .setOobPassword(server.getOobPassword())
                .setProvisionNicMac(provisionNicMac)
                .setDhcpInterface(network.getDhcpInterface())
                .setDhcpRangeStartIp(network.getDhcpRangeStartIp())
                .setDhcpRangeEndIp(network.getDhcpRangeEndIp())
                .setDhcpRangeNetmask(network.getDhcpRangeNetmask())
                .setDhcpRangeGateway(network.getDhcpRangeGateway())
                .setOsImageUuid(msg.getOsImageUuid())
                .setOsDistribution(msg.getOsDistribution())
                .setKickstartTemplate(msg.getKickstartTemplate())
                .setCustomParams(msg.getCustomParams())
                .setJobUuid(jobUuid);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Map<ProvisionNetworkType, ProvisionProvider> providers() {
        Map<ProvisionNetworkType, ProvisionProvider> providers = new HashMap<>();
        for (ProvisionProvider provider : providerList) {
            ProvisionProvider old = providers.put(provider.getType(), provider);
            if (old != null) {
                throw new OperationFailureException(operrf(
                        "duplicate ProvisionProvider for ProvisionNetworkType[%s]: %s and %s",
                        provider.getType().toString(), old.getClass().getName(), provider.getClass().getName()));
            }
        }
        return providers;
    }

    private ErrorCode operrf(String fmt, Object... args) {
        return operr(SysErrors.OPERATION_ERROR.toString(), fmt, args);
    }
}
