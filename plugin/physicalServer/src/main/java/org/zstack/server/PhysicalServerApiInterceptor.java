package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.server.*;
import org.zstack.utils.network.NetworkUtils;

import static org.zstack.core.Platform.argerr;

public class PhysicalServerApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateServerPoolMsg) {
            validate((APICreateServerPoolMsg) msg);
        } else if (msg instanceof APICreatePhysicalServerMsg) {
            validate((APICreatePhysicalServerMsg) msg);
        } else if (msg instanceof APIUpdatePhysicalServerMsg) {
            validate((APIUpdatePhysicalServerMsg) msg);
        } else if (msg instanceof APIDeleteServerPoolMsg) {
            validate((APIDeleteServerPoolMsg) msg);
        } else if (msg instanceof APIChangeClusterServerPoolMsg) {
            validate((APIChangeClusterServerPoolMsg) msg);
        } else if (msg instanceof APIAttachProvisionNetworkToClusterMsg) {
            validate((APIAttachProvisionNetworkToClusterMsg) msg);
        } else if (msg instanceof APIDeleteProvisionNetworkMsg) {
            validate((APIDeleteProvisionNetworkMsg) msg);
        } else if (msg instanceof APICreateProvisionNetworkMsg) {
            validate((APICreateProvisionNetworkMsg) msg);
        }
        return msg;
    }

    private void validate(APICreateServerPoolMsg msg) {
        // Zone existence validated by @APIParam(resourceType = ZoneVO.class)
    }

    private void validate(APICreatePhysicalServerMsg msg) {
        if (!NetworkUtils.isIpv4Address(msg.getManagementIp())) {
            throw new ApiMessageInterceptionException(argerr("invalid managementIp[%s]", msg.getManagementIp()));
        }

        // Validate poolUuid belongs to same zone
        if (msg.getPoolUuid() != null && msg.getZoneUuid() != null) {
            ServerPoolVO pool = dbf.findByUuid(msg.getPoolUuid(), ServerPoolVO.class);
            if (pool != null && !pool.getZoneUuid().equals(msg.getZoneUuid())) {
                throw new ApiMessageInterceptionException(argerr(
                    "ServerPool[uuid:%s] belongs to Zone[uuid:%s], but PhysicalServer specifies Zone[uuid:%s]",
                    msg.getPoolUuid(), pool.getZoneUuid(), msg.getZoneUuid()
                ));
            }
        }
    }

    private void validate(APIUpdatePhysicalServerMsg msg) {
        if (msg.getManagementIp() != null && !NetworkUtils.isIpv4Address(msg.getManagementIp())) {
            throw new ApiMessageInterceptionException(argerr("invalid managementIp[%s]", msg.getManagementIp()));
        }
    }

    private void validate(APICreateProvisionNetworkMsg msg) {
        if (msg.getDhcpRangeStartIp() != null && !NetworkUtils.isIpv4Address(msg.getDhcpRangeStartIp())) {
            throw new ApiMessageInterceptionException(argerr("invalid dhcpRangeStartIp[%s]", msg.getDhcpRangeStartIp()));
        }
        if (msg.getDhcpRangeEndIp() != null && !NetworkUtils.isIpv4Address(msg.getDhcpRangeEndIp())) {
            throw new ApiMessageInterceptionException(argerr("invalid dhcpRangeEndIp[%s]", msg.getDhcpRangeEndIp()));
        }
        if (msg.getDhcpRangeNetmask() != null && !NetworkUtils.isIpv4Address(msg.getDhcpRangeNetmask())) {
            throw new ApiMessageInterceptionException(argerr("invalid dhcpRangeNetmask[%s]", msg.getDhcpRangeNetmask()));
        }
        if (msg.getDhcpRangeGateway() != null && !NetworkUtils.isIpv4Address(msg.getDhcpRangeGateway())) {
            throw new ApiMessageInterceptionException(argerr("invalid dhcpRangeGateway[%s]", msg.getDhcpRangeGateway()));
        }
    }

    private void validate(APIDeleteServerPoolMsg msg) {
        long count = Q.New(PhysicalServerVO.class)
            .eq(PhysicalServerAO_.poolUuid, msg.getUuid())
            .count();
        if (count > 0) {
            throw new ApiMessageInterceptionException(argerr(
                "Cannot delete ServerPool[uuid:%s]: %d PhysicalServer(s) still belong to it. " +
                "Please remove or reassign them first.", msg.getUuid(), count
            ));
        }
    }

    private void validate(APIChangeClusterServerPoolMsg msg) {
        // Validate cluster and new pool belong to the same zone
        ServerPoolVO pool = dbf.findByUuid(msg.getServerPoolUuid(), ServerPoolVO.class);
        if (pool == null) {
            throw new ApiMessageInterceptionException(argerr(
                "ServerPool[uuid:%s] not found", msg.getServerPoolUuid()));
        }

        String clusterZoneUuid = Q.New(org.zstack.header.cluster.ClusterVO.class)
            .eq(org.zstack.header.cluster.ClusterAO_.uuid, msg.getClusterUuid())
            .select(org.zstack.header.cluster.ClusterAO_.zoneUuid)
            .findValue();
        if (clusterZoneUuid == null) {
            throw new ApiMessageInterceptionException(argerr(
                "Cluster[uuid:%s] not found", msg.getClusterUuid()));
        }

        if (!clusterZoneUuid.equals(pool.getZoneUuid())) {
            throw new ApiMessageInterceptionException(argerr(
                "Cluster[uuid:%s] belongs to Zone[uuid:%s], but ServerPool[uuid:%s] belongs to Zone[uuid:%s]",
                msg.getClusterUuid(), clusterZoneUuid, msg.getServerPoolUuid(), pool.getZoneUuid()));
        }
    }

    private void validate(APIAttachProvisionNetworkToClusterMsg msg) {
        boolean exists = Q.New(PhysicalServerProvisionNetworkClusterRefVO.class)
            .eq(PhysicalServerProvisionNetworkClusterRefVO_.networkUuid, msg.getNetworkUuid())
            .eq(PhysicalServerProvisionNetworkClusterRefVO_.clusterUuid, msg.getClusterUuid())
            .isExists();
        if (exists) {
            throw new ApiMessageInterceptionException(argerr(
                "ProvisionNetwork[uuid:%s] is already attached to Cluster[uuid:%s]",
                msg.getNetworkUuid(), msg.getClusterUuid()));
        }
    }

    private void validate(APIDeleteProvisionNetworkMsg msg) {
        long count = Q.New(PhysicalServerProvisionNetworkClusterRefVO.class)
            .eq(PhysicalServerProvisionNetworkClusterRefVO_.networkUuid, msg.getUuid())
            .count();
        if (count > 0) {
            throw new ApiMessageInterceptionException(argerr(
                "Cannot delete ProvisionNetwork[uuid:%s]: %d cluster(s) still attached. Detach them first.",
                msg.getUuid(), count));
        }
    }
}
