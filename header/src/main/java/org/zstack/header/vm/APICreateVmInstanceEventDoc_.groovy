package org.zstack.header.vm

import org.zstack.header.volume.VolumeInventory
import org.zstack.header.vm.VmInstanceInventory

doc {
    title "xxx"

    field {
        name "inventory"
        desc ""
        type ""
    }

    ref {
        name ""
        path "inventory.vmNics"
        desc ""
        type ""
        clz VmNicInventory.class
    }
}
