package org.zstack.xinfini.sdk.volume;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

/**
 * @ Author : yh.w
 * @ Date   : Created in 14:16 2024/5/29
 *  {
 * "metadata": {
 * "created_at": "2024-05-31T16:00:38.109455088+08:00",
 * "id": 1,
 * "labels": [],
 * "name": "snap-1"
 * },
 * "spec": {
 * "bs_policy_id": 3,
 * "bs_volume_id": 1,
 * "created_at": "2024-05-31T16:00:38.109455088+08:00",
 * "creation_finish": null,
 * "creation_timeout_at": "2024-05-31T16:01:08.109217231+08:00",
 * "creator": "AFA",
 * "deleted_at": null,
 * "deletion_begin": null,
 * "deletion_finish": null,
 * "description": "",
 * "etag": "4bb20229-a8c5-401f-b70e-9abd57b9ad5d",
 * "id": 1,
 * "name": "snap-1",
 * "pool_id": 3,
 * "size_mb": 1024,
 * "updated_at": "2024-05-31T16:00:38.109455088+08:00",
 * "uuid": "98cb2f2c-f72b-4da0-87fa-8adc5110b193"
 * },
 * "status": {
 * "cloned_volume_num": 0,
 * "created_at": "2024-05-31T16:00:38.109913582+08:00",
 * "creation_timeout_at": null,
 * "deleted_at": null,
 * "etag": "63b43ee6-d7dc-4a5a-a8ff-471f4556e031",
 * "id": 1,
 * "spring_id": null,
 * "updated_at": "2024-05-31T16:00:38.109913582+08:00"
 * }
 */
public class VolumeSnapshotModule extends BaseResource {
    public VolumeSnapshotModule(Metadata md, VolumeSnapshotSpec spec, VolumeSnapshotStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    private VolumeSnapshotSpec spec;
    private VolumeSnapshotStatus status;

    public VolumeSnapshotStatus getStatus() {
        return status;
    }

    public void setStatus(VolumeSnapshotStatus status) {
        this.status = status;
    }

    public VolumeSnapshotSpec getSpec() {
        return spec;
    }

    public void setSpec(VolumeSnapshotSpec spec) {
        this.spec = spec;
    }

    public static class VolumeSnapshotSpec extends BaseSpec {
        private int bsPolicyId;
        private int bsVolumeId;
        private int poolId;
        private long sizeMb;

        public int getBsPolicyId() {
            return bsPolicyId;
        }

        public void setBsPolicyId(int bsPolicyId) {
            this.bsPolicyId = bsPolicyId;
        }

        public int getBsVolumeId() {
            return bsVolumeId;
        }

        public void setBsVolumeId(int bsVolumeId) {
            this.bsVolumeId = bsVolumeId;
        }

        public int getPoolId() {
            return poolId;
        }

        public void setPoolId(int poolId) {
            this.poolId = poolId;
        }

        public long getSizeMb() {
            return sizeMb;
        }

        public void setSizeMb(long sizeMb) {
            this.sizeMb = sizeMb;
        }
    }

    public static class VolumeSnapshotStatus extends BaseStatus {
        private int clonedVolumeNum;
        private String springId;

        public int getClonedVolumeNum() {
            return clonedVolumeNum;
        }

        public void setClonedVolumeNum(int clonedVolumeNum) {
            this.clonedVolumeNum = clonedVolumeNum;
        }

        public String getSpringId() {
            return springId;
        }

        public void setSpringId(String springId) {
            this.springId = springId;
        }
    }
}
