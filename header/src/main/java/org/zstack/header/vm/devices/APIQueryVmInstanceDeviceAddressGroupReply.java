package org.zstack.header.vm.devices;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by LiangHanYu on 2022/6/20 18:03
 */
@RestResponse(allTo = "inventories")
public class APIQueryVmInstanceDeviceAddressGroupReply extends APIQueryReply {
    private List<VmInstanceDeviceAddressGroupInventory> inventories;

    public List<VmInstanceDeviceAddressGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmInstanceDeviceAddressGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmInstanceDeviceAddressGroupReply __example__() {
        VmInstanceDeviceAddressGroupInventory inv = new VmInstanceDeviceAddressGroupInventory();
        inv.setUuid(uuid());
        inv.setResourceUuid(uuid());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        VmInstanceDeviceAddressArchiveInventory archiveInventory = new VmInstanceDeviceAddressArchiveInventory();
        archiveInventory.setId(1);
        archiveInventory.setResourceUuid(uuid());
        archiveInventory.setPciAddress("pciAddress");
        archiveInventory.setAddressGroupUuid(inv.getUuid());
        archiveInventory.setVmInstanceUuid(uuid());
        archiveInventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        archiveInventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        archiveInventory.setMetadata("metadata");
        archiveInventory.setMetadataClass(VmInstanceDeviceAddressGroupInventory.class.getCanonicalName());
        inv.setAddressList(Arrays.asList(archiveInventory));
        APIQueryVmInstanceDeviceAddressGroupReply result = new APIQueryVmInstanceDeviceAddressGroupReply();
        result.inventories = Collections.singletonList(inv);
        return result;
    }
}
