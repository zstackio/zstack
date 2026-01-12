package org.zstack.compute;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.vm.VmNicParam;
import org.zstack.header.vm.VmNicState;
import org.zstack.utils.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.header.vm.VmInstanceConstant.VM_NIC_QOS_MAX;
import static org.zstack.header.vm.VmInstanceConstant.VM_NIC_QOS_MIN;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class VmNicUtils {
    public static void validateVmParms(List<VmNicParam> vmNicParms, List<String> l3Uuids, List<String> supportNicDriverTypes) {
        if (CollectionUtils.isEmpty(vmNicParms)) {
            return;
        }

        List<String> l3UuidsInParms = vmNicParms.stream().map(VmNicParam::getL3NetworkUuid).distinct().collect(Collectors.toList());
        if (l3UuidsInParms.size() != vmNicParms.size()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_10000, "duplicate nic params"));
        }

        for (VmNicParam nic : vmNicParms) {
            String l3 = nic.getL3NetworkUuid();
            if (StringUtils.isEmpty(l3)) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_10001, "l3NetworkUuid of vm nic can not be null"));
            }
            if (!CollectionUtils.isEmpty(l3Uuids) && !l3Uuids.contains(nic.getL3NetworkUuid())) {
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_10002, "l3NetworkUuid of vm nic is not in l3[%s]", l3Uuids));
            }

            if (nic.getOutboundBandwidth() != null) {
                if (nic.getOutboundBandwidth() < VM_NIC_QOS_MIN || nic.getOutboundBandwidth() > VM_NIC_QOS_MAX) {
                    throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_10003, "outbound bandwidth[%d] of vm nic is out of [8192, 32212254720]", nic.getOutboundBandwidth()));
                }
            }

            if (nic.getInboundBandwidth() != null) {
                if (nic.getInboundBandwidth() < VM_NIC_QOS_MIN || nic.getInboundBandwidth() > VM_NIC_QOS_MAX) {
                    throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_10004, "inbound bandwidth[%d] of vm nic is out of [8192, 32212254720]", nic.getInboundBandwidth()));
                }
            }

            if (nic.getMultiQueueNum() != null ) {
                if (nic.getMultiQueueNum() < 1 || nic.getMultiQueueNum() > 256) {
                    throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_10005, "multi queue num[%d] of vm nic is out of [1,256]", nic.getMultiQueueNum()));
                }
            }

            if (nic.getState() != null) {
                if (!asList(VmNicState.enable.toString(), VmNicState.disable.toString()).contains(nic.getState())) {
                    throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_10006, "vm nic of l3[uuid:%s] state[%s] is not %s or %s ", nic.getL3NetworkUuid(), nic.getState(), VmNicState.enable.toString(), VmNicState.disable.toString()));
                }
            }

            String driverType = nic.getDriverType();
            if (!StringUtils.isEmpty(driverType) && !CollectionUtils.isEmpty(supportNicDriverTypes) && !supportNicDriverTypes.contains(driverType)){
                throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_COMPUTE_10007, "vm nic driver %s not support yet", driverType));
            }
        }
    }
}
