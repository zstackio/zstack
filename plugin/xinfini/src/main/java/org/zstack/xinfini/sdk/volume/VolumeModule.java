package org.zstack.xinfini.sdk.volume;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

/**
 * @ Author : yh.w
 * @ Date   : Created in 14:16 2024/5/29
 *  {
 *    "metadata": {
 *     "id": 1,
 *     "name": "yh-test",
 *     "created_at": "2024-05-09T16:53:57.941473+08:00",
 *     "creation_finish": "2024-05-09T16:53:58.456849+08:00",
 *     "state": {
 *      "state": "active",
 *      "diff_fields": {
 *       "reconciling": null,
 *       "irreconcilable": null
 *      }
 *     },
 *     "labels": []
 *    },
 *    "spec": {
 *     "id": 1,
 *     "name": "yh-test",
 *     "created_at": "2024-05-09T16:53:57.941473+08:00",
 *     "updated_at": "2024-05-11T16:43:52.26325+08:00",
 *     "deleted_at": null,
 *     "creation_finish": "2024-05-09T16:53:58.456849+08:00",
 *     "deletion_begin": null,
 *     "deletion_finish": null,
 *     "etag": "929566b5-2089-44b8-85f3-9665e63cb829",
 *     "bs_policy_id": 7,
 *     "description": "",
 *     "size_mb": 20480,
 *     "creator": "AFA",
 *     "uuid": "98f6b4e8-ff7e-4abf-a6cd-91f37955c718",
 *     "serial": "1642473b90afc37e",
 *     "pool_id": 7,
 *     "loaded": false
 *    },
 *    "status": {
 *     "id": 1,
 *     "created_at": "2024-05-09T16:53:57.94361+08:00",
 *     "updated_at": "2024-05-11T16:43:52.635937+08:00",
 *     "deleted_at": null,
 *     "etag": "39666197-da41-436f-8c8e-a096b1c1f404",
 *     "size_mb": 20480,
 *     "spring_id": 1,
 *     "loaded": false,
 *     "protocol": "",
 *     "mapping_num": 0
 *    }
 *   }
 */
public class VolumeModule extends BaseResource {
    public VolumeModule(Metadata md, VolumeSpec spec, VolumeStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    private VolumeSpec spec;
    private VolumeStatus status;

    public VolumeSpec getSpec() {
        return spec;
    }

    public void setSpec(VolumeSpec spec) {
        this.spec = spec;
    }

    public VolumeStatus getStatus() {
        return status;
    }

    public void setStatus(VolumeStatus status) {
        this.status = status;
    }

    public static class VolumeSpec extends BaseSpec {
        private int bsPolicyId;
        private int sizeMb;
        private boolean loaded;
        private int poolId;

        public int getBsPolicyId() {
            return bsPolicyId;
        }

        public void setBsPolicyId(int bsPolicyId) {
            this.bsPolicyId = bsPolicyId;
        }

        public int getSizeMb() {
            return sizeMb;
        }

        public void setSizeMb(int sizeMb) {
            this.sizeMb = sizeMb;
        }

        public boolean isLoaded() {
            return loaded;
        }

        public void setLoaded(boolean loaded) {
            this.loaded = loaded;
        }

        public int getPoolId() {
            return poolId;
        }

        public void setPoolId(int poolId) {
            this.poolId = poolId;
        }
    }

    public static class VolumeStatus extends BaseStatus {
        private int sizeMb;
        private boolean loaded;
        private int springId;
        private String protocol;
        private int mappingNum;

        public int getSizeMb() {
            return sizeMb;
        }

        public void setSizeMb(int sizeMb) {
            this.sizeMb = sizeMb;
        }

        public boolean isLoaded() {
            return loaded;
        }

        public void setLoaded(boolean loaded) {
            this.loaded = loaded;
        }

        public int getSpringId() {
            return springId;
        }

        public void setSpringId(int springId) {
            this.springId = springId;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public int getMappingNum() {
            return mappingNum;
        }

        public void setMappingNum(int mappingNum) {
            this.mappingNum = mappingNum;
        }
    }
}
