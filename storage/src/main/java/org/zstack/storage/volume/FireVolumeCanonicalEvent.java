package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.volume.VolumeCanonicalEvents;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;

import java.util.Date;

/**
 * Created by xing5 on 2016/3/12.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FireVolumeCanonicalEvent {
    @Autowired
    private EventFacade evtf;

    public void fireVolumeStatusChangedEvent(VolumeStatus oldStatus, VolumeInventory vol) {
        VolumeCanonicalEvents.VolumeStatusChangedData d = new VolumeCanonicalEvents.VolumeStatusChangedData();
        d.setInventory(vol);
        d.setDate(new Date());
        d.setNewStatus(vol.getStatus());
        d.setOldStatus(oldStatus == null ? null : oldStatus.toString());
        d.setVolumeUuid(vol.getUuid());
        evtf.fire(VolumeCanonicalEvents.VOLUME_STATUS_CHANGED_PATH, d);
    }
}
