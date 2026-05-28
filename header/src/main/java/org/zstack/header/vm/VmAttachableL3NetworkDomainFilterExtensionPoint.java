package org.zstack.header.vm;

import org.zstack.header.network.l3.L3NetworkInventory;

import java.util.List;

public interface VmAttachableL3NetworkDomainFilterExtensionPoint {
    /**
     * Returns the SDN controller vendor this domain filter handles.
     *
     * @return vendor type for SDN-backed L3 networks, or null for non-SDN/default L3 networks
     */
    String getSdnControllerVendorType();

    /**
     * Keeps the L3 networks that are attachable to the VM within this filter's network domain.
     *
     * @param vm non-null VM inventory used as the attach target
     * @param l3s non-null candidate L3 list; implementations must not modify the input list
     * @return non-null filtered candidate list containing attachable L3 networks in this domain
     */
    List<L3NetworkInventory> filterAttachableL3NetworkInDomain(VmInstanceInventory vm, List<L3NetworkInventory> l3s);
}
