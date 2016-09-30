package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 1:45 PM
 * To change this template use File | Settings | File Templates.
 */
public interface NetworkServiceDhcpBackend {
    NetworkServiceProviderType getProviderType();

    void applyDhcpService(List<DhcpStruct> dhcpStructList, VmInstanceSpec spec, Completion completion);

    void releaseDhcpService(List<DhcpStruct> dhcpStructsList, VmInstanceSpec spec, NoErrorCompletion completion);

    void vmDefaultL3NetworkChanged(VmInstanceInventory vm, String previousL3, String nowL3, Completion completion);
}
