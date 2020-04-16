package org.zstack.compute.vm;

import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;

public class VmHostNameHelper {
    public String getHostName(VmInstanceInventory vm) {
        String hostName = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.HOSTNAME_TOKEN);

        if (hostName == null) {
            for (VmNicInventory nic : vm.getVmNics()) {
                if (nic.getL3NetworkUuid().equals(vm.getDefaultL3NetworkUuid())) {
                    if (nic.getIpVersion() == IPv6Constants.IPv4) {
                        hostName = nic.getIp().replaceAll("\\.", "-");
                    } else {
                        hostName = IPv6NetworkUtils.ipv6AddessToHostname(nic.getIp());
                    }
                }
            }
        }

        return hostName;
    }

    public String getHostName(VmInstanceVO vm) {
        String hostName = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.HOSTNAME_TOKEN);

        if (hostName == null) {
            for (VmNicVO nic : vm.getVmNics()) {
                if (nic.getL3NetworkUuid().equals(vm.getDefaultL3NetworkUuid())) {
                    if (nic.getIpVersion() == IPv6Constants.IPv4) {
                        hostName = nic.getIp().replaceAll("\\.", "-");
                    } else {
                        hostName = IPv6NetworkUtils.ipv6AddessToHostname(nic.getIp());
                    }
                }
            }
        }

        return hostName;
    }
}
