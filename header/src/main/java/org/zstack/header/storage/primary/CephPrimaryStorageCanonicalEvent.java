package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedJsonSchema;

public class CephPrimaryStorageCanonicalEvent {
    public static final String POOL_CAPACITY_INSUFFICIENT = "/ceph/primaryStorage/capacityInsufficient";

    @NeedJsonSchema
    public static class CephPoolCapacityInsufficientData {
        private String poolUuid;

        public String getPoolUuid() {
            return poolUuid;
        }

        public void setPoolUuid(String poolUuid) {
            this.poolUuid = poolUuid;
        }
    }
}
