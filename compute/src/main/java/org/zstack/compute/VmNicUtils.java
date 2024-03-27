package org.zstack.compute;

import org.apache.commons.lang.StringUtils;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.vm.VmInstanceType;
import org.zstack.header.vm.VmNicParam;
import org.zstack.header.vm.VmNicState;
import org.zstack.utils.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

public class VmNicUtils {
    public static void validateVmParams(List<VmNicParam> vmNicParams, List<String> l3Uuids, List<String> supportNicDriverTypes, String vmType) {
        if (CollectionUtils.isEmpty(vmNicParams)) {
            return;
        }

        List<String> l3UuidsInParams = vmNicParams.stream().map(VmNicParam::getL3NetworkUuid).distinct().collect(Collectors.toList());
        if (!VmGlobalConfig.MULTI_VNIC_SUPPORT.value(Boolean.class) && l3UuidsInParams.size() != vmNicParams.size()) {
            throw new ApiMessageInterceptionException(argerr("duplicate nic params"));
        }

        for (VmNicParam nic : vmNicParams) {
            String l3 = nic.getL3NetworkUuid();
            if (StringUtils.isEmpty(l3)) {
                throw new ApiMessageInterceptionException(argerr("l3NetworkUuid of vm nic can not be null"));
            }
            if (!CollectionUtils.isEmpty(l3Uuids) && !l3Uuids.contains(nic.getL3NetworkUuid())) {
                throw new ApiMessageInterceptionException(argerr("l3NetworkUuid of vm nic is not in l3[%s]", l3Uuids));
            }

            if (nic.getOutboundBandwidth() != null) {
                if (nic.getOutboundBandwidth() < 8192 || nic.getOutboundBandwidth() > 32212254720L) {
                    throw new ApiMessageInterceptionException(argerr("outbound bandwidth[%d] of vm nic is out of [8192, 32212254720]", nic.getOutboundBandwidth()));
                }
            }

            if (nic.getInboundBandwidth() != null) {
                if (nic.getInboundBandwidth() < 8192 || nic.getInboundBandwidth() > 32212254720L) {
                    throw new ApiMessageInterceptionException(argerr("inbound bandwidth[%d] of vm nic is out of [8192, 32212254720]", nic.getInboundBandwidth()));
                }
            }

            if (nic.getMultiQueueNum() != null ) {
                if (nic.getMultiQueueNum() < 1 || nic.getMultiQueueNum() > 256) {
                    throw new ApiMessageInterceptionException(argerr("multi queue num[%d] of vm nic is out of [1,256]", nic.getMultiQueueNum()));
                }
            }

            if (nic.getState() != null) {
                if (!asList(VmNicState.enable.toString(), VmNicState.disable.toString()).contains(nic.getState())) {
                    throw new ApiMessageInterceptionException(argerr("vm nic of l3[uuid:%s] state[%s] is not %s or %s ", nic.getL3NetworkUuid(), nic.getState(), VmNicState.enable.toString(), VmNicState.disable.toString()));
                }
            }

            String driverType = nic.getDriverType();
            if (!StringUtils.isEmpty(driverType) && !CollectionUtils.isEmpty(supportNicDriverTypes) && !supportNicDriverTypes.contains(driverType)){
                throw new ApiMessageInterceptionException(argerr("vm nic driver %s not support yet", driverType));
            }

            if (nic.isSriovEnabled() && vmType != null && !VmInstanceType.valueOf(vmType).isSriovSupported()) {
                throw new ApiMessageInterceptionException(argerr("vm type[%s] is not supported SR-IOV", vmType));
            }

            if (!nic.isSriovEnabled() && nic.getVfParentUuid() != null) {
                throw new ApiMessageInterceptionException(argerr("vm nic with vf parent uuid[%s] should be SR-IOV enabled", nic.getVfParentUuid()));
            }
        }
    }
}
