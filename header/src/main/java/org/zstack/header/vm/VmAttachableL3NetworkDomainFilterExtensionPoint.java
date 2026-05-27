package org.zstack.header.vm;

import org.zstack.header.network.l3.L3NetworkInventory;

import java.util.List;

public interface VmAttachableL3NetworkDomainFilterExtensionPoint {
    String getSdnControllerVendorType();

    List<L3NetworkInventory> filterAttachableL3NetworkInDomain(VmInstanceInventory vm, List<L3NetworkInventory> l3s);
}
