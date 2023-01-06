package org.zstack.header.vm;

import org.zstack.header.vm.cdrom.VmCdRomInventory;

/**
 * Created by LiangHanYu on 2022/9/26 17:44
 */
public class ArchiveVmCdRomBundle {
    VmCdRomInventory cdRomInventory;

    public ArchiveVmCdRomBundle() {
    }

    public ArchiveVmCdRomBundle(VmCdRomInventory cdRomInventory) {
        this.cdRomInventory = cdRomInventory;
    }

    public VmCdRomInventory getCdRomInventory() {
        return cdRomInventory;
    }

    public void setCdRomInventory(VmCdRomInventory cdRomInventory) {
        this.cdRomInventory = cdRomInventory;
    }
}
