package org.zstack.xinfini.sdk.pool;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

/**
 * @ Author : yh.w
 * @ Date   : Created in 18:23 2024/5/27
 * @example:
 * {
 *  "metadata": {
 *   "id": 7,
 *   "name": "7c37c3c0-a1e3-470c-a2c5-02698358e1de",
 *   "created_at": "2024-05-09T14:56:18.013405+08:00",
 *   "creation_finish": "2024-05-09T14:56:23.438683+08:00",
 *   "state": {
 *    "state": "active",
 *    "diff_fields": {
 *     "reconciling": null,
 *     "irreconcilable": null
 *    }
 *   },
 *   "labels": []
 *  },
 *  "spec": {
 *   "id": 7,
 *   "name": "7c37c3c0-a1e3-470c-a2c5-02698358e1de",
 *   "created_at": "2024-05-09T14:56:18.013405+08:00",
 *   "updated_at": "2024-05-09T14:56:23.438925+08:00",
 *   "deleted_at": null,
 *   "creation_finish": "2024-05-09T14:56:23.438683+08:00",
 *   "deletion_begin": null,
 *   "deletion_finish": null,
 *   "etag": "ac00aee3-4685-4515-a765-159bb7ed1145",
 *   "description": "",
 *   "uuid": "3a1fde5e-13a0-447a-afa8-a49540792a5c",
 *   "cache_enabled": true,
 *   "cache_replica_num": 3,
 *   "data_replica_type": "replica",
 *   "data_replica_num": 3,
 *   "data_data_chunk_num": 0,
 *   "data_coding_chunk_num": 0,
 *   "data_out_failure_domain_num": 0,
 *   "cache_placement_policy_id": 13,
 *   "data_placement_policy_id": 14,
 *   "data_stripe_width": 0,
 *   "pool_id": 7,
 *   "creation_timeout_at": null
 *  },
 *  "status": {
 *   "id": 7,
 *   "created_at": "2024-05-09T14:56:18.013744+08:00",
 *   "updated_at": "2024-05-09T14:56:23.431072+08:00",
 *   "deleted_at": null,
 *   "etag": "1327a072-b194-4b7e-9539-ed7b36bf4ec3",
 *   "cache_placement_policy_id": 13,
 *   "data_placement_policy_id": 14,
 *   "cache_replica_num": 3,
 *   "data_replica_num": 3,
 *   "spring_id": 7,
 *   "creation_timeout_at": null
 *  }
 * }
 */
public class BsPolicyModule extends BaseResource {
    private BsPolicySpec spec;
    private BsPolicyStatus status;

    public BsPolicySpec getSpec() {
        return spec;
    }

    public void setSpec(BsPolicySpec spec) {
        this.spec = spec;
    }

    public BsPolicyStatus getStatus() {
        return status;
    }

    public void setStatus(BsPolicyStatus status) {
        this.status = status;
    }

    public BsPolicyModule(Metadata md, BsPolicySpec spec, BsPolicyStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    public static class BsPolicyStatus extends BaseStatus {
        // not used yet
    }

    public static class BsPolicySpec extends BaseSpec {
        private boolean cacheEnabled;
        private int cacheReplicaNum;
        private String dataReplicaType;
        private int dataReplicaNum;

        public boolean isCacheEnabled() {
            return cacheEnabled;
        }

        public void setCacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
        }

        public int getCacheReplicaNum() {
            return cacheReplicaNum;
        }

        public void setCacheReplicaNum(int cacheReplicaNum) {
            this.cacheReplicaNum = cacheReplicaNum;
        }

        public String getDataReplicaType() {
            return dataReplicaType;
        }

        public void setDataReplicaType(String dataReplicaType) {
            this.dataReplicaType = dataReplicaType;
        }

        public int getDataReplicaNum() {
            return dataReplicaNum;
        }

        public void setDataReplicaNum(int dataReplicaNum) {
            this.dataReplicaNum = dataReplicaNum;
        }
    }
}
