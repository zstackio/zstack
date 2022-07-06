package org.zstack.header.vm.devices;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

/**
 * Created by LiangHanYu on 2022/6/17 17:31
 */
@RestResponse(allTo = "inventories")
public class APIQueryVmInstanceDeviceAddressArchiveReply extends APIQueryReply {
    private List<VmInstanceDeviceAddressArchiveInventory> inventories;

    public List<VmInstanceDeviceAddressArchiveInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmInstanceDeviceAddressArchiveInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmInstanceDeviceAddressArchiveReply __example__() {
        VmInstanceDeviceAddressArchiveInventory inv = new VmInstanceDeviceAddressArchiveInventory();
        inv.setId(1);
        inv.setResourceUuid(uuid());

        DeviceAddress address = new DeviceAddress();
        address.bus = "00";
        address.domain = "0000";
        address.slot = "0d";
        address.function = "0";

        inv.setDeviceAddress(address.toString());
        inv.setAddressGroupUuid(uuid());
        inv.setMetadata("Metadata");
        inv.setMetadataClass(VmInstanceDeviceAddressArchiveInventory.class.getCanonicalName());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        APIQueryVmInstanceDeviceAddressArchiveReply result = new APIQueryVmInstanceDeviceAddressArchiveReply();
        result.inventories = Collections.singletonList(inv);
        return result;
    }
}
