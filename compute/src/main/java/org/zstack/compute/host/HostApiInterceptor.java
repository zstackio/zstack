package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.zone.ManagementNetworkIpVersionManager;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class HostApiInterceptor implements ApiMessageInterceptor {
    private static final String INVALID_MANAGEMENT_IP_ERROR =
            "managementIp[%s] is not a valid IPv4 address, IPv6 address, or hostname";
    private static final String RESERVED_MANAGEMENT_IPV6_ERROR =
            "managementIp[%s] is an IPv6 address that cannot be used as a management address";

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ManagementNetworkIpVersionManager managementNetworkIpVersionManager;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof HostMessage) {
            HostMessage hmsg = (HostMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hmsg.getHostUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);

        if (msg instanceof APIAddHostMsg) {
            validate((APIAddHostMsg) msg);
        } else if (msg instanceof APIUpdateHostMsg) {
            validate((APIUpdateHostMsg) msg);
        } else if (msg instanceof APIDeleteHostMsg) {
            validate((APIDeleteHostMsg) msg);
        } else if (msg instanceof APIChangeHostStateMsg){
            validate((APIChangeHostStateMsg) msg);
        } else if (msg instanceof APIGetHostWebSshUrlMsg) {
            validate((APIGetHostWebSshUrlMsg) msg);
        } else if (msg instanceof APICreateHostNetworkServiceTypeMsg) {
            validate((APICreateHostNetworkServiceTypeMsg) msg);
        } else if (msg instanceof APIDeleteHostNetworkServiceTypeMsg) {
            validate((APIDeleteHostNetworkServiceTypeMsg) msg);
        } else if (msg instanceof APIUpdateHostNetworkServiceTypeMsg) {
            validate((APIUpdateHostNetworkServiceTypeMsg) msg);
        }

        return msg;
    }

    private void validate(APIDeleteHostNetworkServiceTypeMsg msg) {
        if (Q.New(HostNetworkLabelVO.class).eq(HostNetworkLabelVO_.uuid, msg.getUuid())
                .eq(HostNetworkLabelVO_.system, Boolean.TRUE).isExists()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_HOST_10108, "system host network service type[%s] cannot be deleted", msg.getUuid()));
        }
    }

    private void validate(APIUpdateHostNetworkServiceTypeMsg msg) {
        if (Q.New(HostNetworkLabelVO.class).eq(HostNetworkLabelVO_.uuid, msg.getUuid())
                .eq(HostNetworkLabelVO_.system, Boolean.TRUE).isExists()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_HOST_10109, "system host network service type[%s] cannot be updated", msg.getUuid()));
        }
    }

    private void validate(APICreateHostNetworkServiceTypeMsg msg) {
        if (Q.New(HostNetworkLabelVO.class).eq(HostNetworkLabelVO_.serviceType, msg.getServiceType()).isExists()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_HOST_10110, "there has been a host network service type[%s]", msg.getServiceType()));
        }
    }

    private void validate(APIGetHostWebSshUrlMsg msg) {
        String ZOPS_CONTAINER_NAME = "zops-controller";
        ShellResult ret;
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            ret = ShellUtils.runAndReturn(String.format("docker exec %s systemctl is-active webssh", ZOPS_CONTAINER_NAME));
        } else {
            ret = new ShellResult();
            ret.setCommand(String.format("docker exec %s systemctl is-active webssh", ZOPS_CONTAINER_NAME));
            ret.setRetCode(0);
        }
        if (!ret.isReturnCode(0)) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_HOST_10111, "webssh server is not running."));
        }
    }

    private void validate(APIDeleteHostMsg msg) {
        if (!dbf.isExist(msg.getUuid(), HostVO.class)) {
            APIDeleteHostEvent evt = new APIDeleteHostEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIUpdateHostMsg msg) {
        if (msg.getManagementIp() != null) {
            msg.setManagementIp(validateManagementEndpoint(msg.getManagementIp()));

            SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
            q.add(HostVO_.managementIp, Op.EQ, msg.getManagementIp());
            if (q.isExists()) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_HOST_10112, "there has been a host having managementIp[%s]", msg.getManagementIp()));
            }

            String zoneUuid = Q.New(HostVO.class)
                    .select(HostVO_.zoneUuid)
                    .eq(HostVO_.uuid, msg.getUuid())
                    .findValue();
            managementNetworkIpVersionManager.validateEndpointInZone(zoneUuid, msg.getManagementIp(),
                    "host", msg.getUuid(), ORG_ZSTACK_COMPUTE_HOST_10130);
        }
    }

    private void validate(APIAddHostMsg msg) {
        validateManagementEndpoint(msg);
        String zoneUuid = Q.New(ClusterVO.class)
                .select(ClusterVO_.zoneUuid)
                .eq(ClusterVO_.uuid, msg.getClusterUuid())
                .findValue();
        managementNetworkIpVersionManager.validateEndpointInZone(zoneUuid, msg.getManagementIp(),
                "host", msg.getName(), ORG_ZSTACK_COMPUTE_HOST_10130);
    }

    static void validateManagementEndpoint(APIAddHostMsg msg) {
        String managementIp = msg.getManagementIp();
        msg.setManagementIp(validateManagementEndpoint(managementIp));
    }

    static String validateManagementEndpoint(String managementIp) {
        if (IPv6NetworkUtils.isIpv6Address(managementIp)) {
            if (!IPv6NetworkUtils.isValidManagementIpv6Address(managementIp)) {
                throw new ApiMessageInterceptionException(argerr(
                        ORG_ZSTACK_COMPUTE_HOST_10129,
                        RESERVED_MANAGEMENT_IPV6_ERROR,
                        managementIp));
            }
        } else if (!isValidManagementEndpoint(managementIp)) {
            throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_COMPUTE_HOST_10128,
                    INVALID_MANAGEMENT_IP_ERROR,
                    managementIp));
        }

        if (IPv6NetworkUtils.isIpv6Address(managementIp)) {
            return IPv6NetworkUtils.getIpv6AddressCanonicalString(managementIp);
        }

        return managementIp;
    }

    static String getManagementEndpointValidationErrorCode(String managementIp) {
        if (IPv6NetworkUtils.isIpv6Address(managementIp)) {
            return IPv6NetworkUtils.isValidManagementIpv6Address(managementIp) ? null : ORG_ZSTACK_COMPUTE_HOST_10129;
        }

        return isValidManagementEndpoint(managementIp) ? null : ORG_ZSTACK_COMPUTE_HOST_10128;
    }

    private static boolean isValidManagementEndpoint(String endpoint) {
        return IPv6NetworkUtils.isValidManagementEndpoint(endpoint);
    }

    private void validate(APIChangeHostStateMsg msg){
        HostStatus hostStatus = Q.New(HostVO.class)
                .select(HostVO_.status)
                .eq(HostVO_.uuid,msg.getHostUuid())
                .findValue();
        if (hostStatus != HostStatus.Connected && msg.getStateEvent().equals(HostStateEvent.maintain.toString())){
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_COMPUTE_HOST_10114, "can not maintain host[uuid:%s, status:%s]which is not Connected", msg.getHostUuid(), hostStatus));
        }
    }
}
