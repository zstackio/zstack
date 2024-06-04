package org.zstack.xinfini.sdk.pool;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

/**
 * @ Author : yh.w
 * @ Date   : Created in 18:23 2024/5/27
 * @example:
 * {
 *    "metadata": {
 *     "id": 7,
 *     "name": "StoragePool",
 *     "created_at": "2024-05-09T14:56:17.986702+08:00",
 *     "creation_finish": "2024-05-09T14:56:34.226868+08:00",
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
 *     "id": 7,
 *     "name": "StoragePool",
 *     "created_at": "2024-05-09T14:56:17.986702+08:00",
 *     "updated_at": "2024-05-09T14:56:34.227127+08:00",
 *     "deleted_at": null,
 *     "creation_finish": "2024-05-09T14:56:34.226868+08:00",
 *     "deletion_begin": null,
 *     "deletion_finish": null,
 *     "etag": "7d33730b-2b04-4d1a-966a-17524906ff71",
 *     "description": "",
 *     "uuid": "b80b3048-625c-46f0-a6b0-fa82a0b83540",
 *     "fault_domain_type": "host",
 *     "removal_stage": null,
 *     "default_bs_policy_id": 7,
 *     "creation_timeout_at": null,
 *     "recovery_reserve_threshold": 0.05
 *    },
 *    "status": {
 *     "id": 7,
 *     "created_at": "2024-05-09T14:56:17.987193+08:00",
 *     "updated_at": "2024-05-09T14:56:23.157244+08:00",
 *     "deleted_at": null,
 *     "etag": "cafb970c-c092-4aad-a275-61e8fa277b80",
 *     "spring_id": 1,
 *     "topo_action_state": null,
 *     "state": "active",
 *     "creation_timeout_at": null,
 *     "total_mb": 0,
 *     "node_num": 3,
 *     "lun_num": 3,
 *     "created_lun_num": 3,
 *     "spring_num": 12,
 *     "created_spring_num": 12,
 *     "lun_error_num": 0,
 *     "bs_policy_num": 1,
 *     "recovery_reserve_threshold": 0.05
 *    }
 *   }
 */
public class PoolModule extends BaseResource {
    private PoolSpec spec;
    private PoolStatus status;

    public PoolSpec getSpec() {
        return spec;
    }

    public void setSpec(PoolSpec spec) {
        this.spec = spec;
    }

    public PoolStatus getStatus() {
        return status;
    }

    public void setStatus(PoolStatus status) {
        this.status = status;
    }

    public PoolModule(Metadata md, PoolSpec spec, PoolStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    public static class PoolStatus extends BaseStatus {
        private int springId;
        private int topoActionState;
        private String state;
        private int totalMb;
        private int nodeNum;
        private int lunNum;
        private int createdLunNum;
        private int springNum;
        private int createdSpringNum;
        private int lunErrorNum;
        private int bsPolicyNum;
        private float recoveryReserveThreshold;

        public int getSpringId() {
            return springId;
        }

        public void setSpringId(int springId) {
            this.springId = springId;
        }

        public int getTopoActionState() {
            return topoActionState;
        }

        public void setTopoActionState(int topoActionState) {
            this.topoActionState = topoActionState;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public int getTotalMb() {
            return totalMb;
        }

        public void setTotalMb(int totalMb) {
            this.totalMb = totalMb;
        }

        public int getNodeNum() {
            return nodeNum;
        }

        public void setNodeNum(int nodeNum) {
            this.nodeNum = nodeNum;
        }

        public int getLunNum() {
            return lunNum;
        }

        public void setLunNum(int lunNum) {
            this.lunNum = lunNum;
        }

        public int getCreatedLunNum() {
            return createdLunNum;
        }

        public void setCreatedLunNum(int createdLunNum) {
            this.createdLunNum = createdLunNum;
        }

        public int getSpringNum() {
            return springNum;
        }

        public void setSpringNum(int springNum) {
            this.springNum = springNum;
        }

        public int getCreatedSpringNum() {
            return createdSpringNum;
        }

        public void setCreatedSpringNum(int createdSpringNum) {
            this.createdSpringNum = createdSpringNum;
        }

        public int getLunErrorNum() {
            return lunErrorNum;
        }

        public void setLunErrorNum(int lunErrorNum) {
            this.lunErrorNum = lunErrorNum;
        }

        public int getBsPolicyNum() {
            return bsPolicyNum;
        }

        public void setBsPolicyNum(int bsPolicyNum) {
            this.bsPolicyNum = bsPolicyNum;
        }

        public float getRecoveryReserveThreshold() {
            return recoveryReserveThreshold;
        }

        public void setRecoveryReserveThreshold(float recoveryReserveThreshold) {
            this.recoveryReserveThreshold = recoveryReserveThreshold;
        }
    }

    public static class PoolSpec extends BaseSpec {
        private String faultDomainType;
        private String removalStage;
        private int defaultBsPolicyId;
        private float recoveryReserveThreshold;

        public String getFaultDomainType() {
            return faultDomainType;
        }

        public void setFaultDomainType(String faultDomainType) {
            this.faultDomainType = faultDomainType;
        }

        public String getRemovalStage() {
            return removalStage;
        }

        public void setRemovalStage(String removalStage) {
            this.removalStage = removalStage;
        }

        public int getDefaultBsPolicyId() {
            return defaultBsPolicyId;
        }

        public void setDefaultBsPolicyId(int defaultBsPolicyId) {
            this.defaultBsPolicyId = defaultBsPolicyId;
        }

        public float getRecoveryReserveThreshold() {
            return recoveryReserveThreshold;
        }

        public void setRecoveryReserveThreshold(float recoveryReserveThreshold) {
            this.recoveryReserveThreshold = recoveryReserveThreshold;
        }
    }
}
