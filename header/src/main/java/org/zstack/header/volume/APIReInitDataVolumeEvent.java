package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIReInitDataVolumeEvent extends APIEvent {
    private VolumeInventory inventory;

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public APIReInitDataVolumeEvent() {
        super();
    }

    public APIReInitDataVolumeEvent(String id) {
        super(id);
    }

    public static APIReInitDataVolumeEvent __example__() {
        APIReInitDataVolumeEvent event = new APIReInitDataVolumeEvent();

        String volumeUuid = uuid();
        VolumeInventory vol = new VolumeInventory();
        vol.setName("test-data-volume");
        vol.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vol.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vol.setType(VolumeType.Data.toString());
        vol.setUuid(volumeUuid);
        vol.setSize(SizeUnit.GIGABYTE.toByte(100));
        vol.setActualSize(SizeUnit.GIGABYTE.toByte(20));
        vol.setDeviceId(1);
        vol.setState(VolumeState.Enabled.toString());
        vol.setFormat("raw");
        vol.setDiskOfferingUuid(uuid());
        vol.setInstallPath(String.format("/zstack_ps/dataVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-%s/%s.qcow2", volumeUuid, volumeUuid));
        vol.setStatus(VolumeStatus.Ready.toString());
        vol.setPrimaryStorageUuid(uuid());
        vol.setVmInstanceUuid(uuid());

        event.setInventory(vol);
        return event;
    }
}
