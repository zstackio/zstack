package org.zstack.compute;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.vm.VmNicParm;
import org.zstack.utils.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

public class VmNicUtils {
    public static void validateVmParms(List<VmNicParm> vmNicParms, List<String> l3Uuids, List<String> supportNicDriverTypes) {
        if (CollectionUtils.isEmpty(vmNicParms)) {
            return;
        }

        List<String> l3UuidsInParms = vmNicParms.stream().map(VmNicParm::getL3NetworkUuid).distinct().collect(Collectors.toList());
        if (l3UuidsInParms.size() != vmNicParms.size()) {
            throw new ApiMessageInterceptionException(argerr("duplicate nic params"));
        }

        for (VmNicParm nic : vmNicParms) {
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
                if (nic.getMultiQueueNum() < 0) {
                    throw new ApiMessageInterceptionException(argerr("multi queue num[%d] of vm nic is less than 0", nic.getMultiQueueNum()));
                }
            }

            String driverType = nic.getDriverType();
            if (!StringUtils.isEmpty(driverType) && !CollectionUtils.isEmpty(supportNicDriverTypes) && !supportNicDriverTypes.contains(driverType)){
                throw new ApiMessageInterceptionException(argerr("vm nic driver %s not support yet", driverType));
            }
        }
    }
}
