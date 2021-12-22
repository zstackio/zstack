package org.zstack.compute.vm;

import org.zstack.header.vm.VmNicInventory;

import java.util.List;

public interface VmNicManager {

    List<String> getSupportNicDriverTypes();

    String getDefaultPVNicDriver();

    String getDefaultNicDriver();

    void setNicDriverType(VmNicInventory nic, boolean isImageSupportVirtIo, boolean isParaVirtualization);
}
