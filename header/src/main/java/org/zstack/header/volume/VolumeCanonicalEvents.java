package org.zstack.header.volume;

import org.zstack.header.message.NeedJsonSchema;

import java.util.Date;

/**
 * Created by xing5 on 2016/3/12.
 */
public class VolumeCanonicalEvents {
    public static final String VOLUME_STATUS_CHANGED_PATH = "/volume/status/change";

    @NeedJsonSchema
    public static class VolumeStatusChangedData {
        private String volumeUuid;
        private String oldStatus;
        private String newStatus;
        private VolumeInventory inventory;
        private Date date = new Date();

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public String getOldStatus() {
            return oldStatus;
        }

        public void setOldStatus(String oldStatus) {
            this.oldStatus = oldStatus;
        }

        public String getNewStatus() {
            return newStatus;
        }

        public void setNewStatus(String newStatus) {
            this.newStatus = newStatus;
        }

        public VolumeInventory getInventory() {
            return inventory;
        }

        public void setInventory(VolumeInventory inventory) {
            this.inventory = inventory;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }
}
