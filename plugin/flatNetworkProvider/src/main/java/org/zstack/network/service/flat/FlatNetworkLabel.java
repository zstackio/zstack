package org.zstack.network.service.flat;

import org.zstack.core.logging.LogLabel;

/**
 * Created by xing5 on 2016/6/3.
 */
public class FlatNetworkLabel {
    @LogLabel(messages = {
            "en_US = sync VMs' DHCP information to the host",
            "zh_CN = 同步虚拟机的DHCP信息到物理机"
    })
    public static final String SYNC_DHCP = "flatNetwork.syncDHCP";

    @LogLabel(messages = {
            "en_US = sync VMs' DNS information to the host",
            "zh_CN = 同步虚拟机的DNS信息到物理机"
    })
    public static final String SYNC_DNS = "flatNetwork.syncDNS";

    @LogLabel(messages = {
            "en_US = sync VMs' EIP information to the host",
            "zh_CN = 同步虚拟机的EIP信息到物理机"
    })
    public static final String SYNC_EIP = "flatNetwork.syncEIP";

    @LogLabel(messages = {
            "en_US = sync VMs' userdata information to the host",
            "zh_CN = 同步虚拟机的userdata信息到物理机"
    })
    public static final String SYNC_USERDATA = "flatNetwork.syncUserdata";
}
