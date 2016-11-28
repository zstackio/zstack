package org.zstack.header.host;

public interface HypervisorFactory {
    HostVO createHost(HostVO vo, AddHostMessage msg);

    Host getHost(HostVO vo);

    HypervisorType getHypervisorType();

    HostInventory getHostInventory(HostVO vo);

    HostInventory getHostInventory(String uuid);
}
