package org.zstack.xinfini;

import org.zstack.xinfini.sdk.node.NodeModule;
import org.zstack.xinfini.sdk.pool.BsPolicyModule;
import org.zstack.xinfini.sdk.pool.PoolCapacity;
import org.zstack.xinfini.sdk.pool.PoolModule;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class XInfiniAddonInfo {
    private List<Node> nodes;

    private List<Pool> pools = new ArrayList<>();

    private String currentIscsiTargetId;
    private int currentIscsiTargetIndex;

    private String currentImageIscsiTargetId;
    private int currentImageIscsiTargetIndex;

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public List<Pool> getPools() {
        return pools;
    }

    public void setPools(List<Pool> pools) {
        this.pools = pools;
    }

    public String getCurrentIscsiTargetId() {
        return currentIscsiTargetId;
    }

    public void setCurrentIscsiTargetId(String currentIscsiTargetId) {
        this.currentIscsiTargetId = currentIscsiTargetId;
    }

    public int getCurrentIscsiTargetIndex() {
        return currentIscsiTargetIndex;
    }

    public void setCurrentIscsiTargetIndex(int currentIscsiTargetIndex) {
        this.currentIscsiTargetIndex = currentIscsiTargetIndex;
    }

    public String getCurrentImageIscsiTargetId() {
        return currentImageIscsiTargetId;
    }

    public void setCurrentImageIscsiTargetId(String currentImageIscsiTargetId) {
        this.currentImageIscsiTargetId = currentImageIscsiTargetId;
    }

    public int getCurrentImageIscsiTargetIndex() {
        return currentImageIscsiTargetIndex;
    }

    public void setCurrentImageIscsiTargetIndex(int currentImageIscsiTargetIndex) {
        this.currentImageIscsiTargetIndex = currentImageIscsiTargetIndex;
    }

    public static class Node {
        private int id;
        private String ip;
        private String state;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public static Node valueOf(NodeModule nodeModule) {
            Node node = new Node();
            node.setIp(nodeModule.getSpec().getAdminIp());
            node.setId(nodeModule.getSpec().getId());
            node.setState(nodeModule.getMetadata().getState().getState());
            return node;
        }
    }

    public static class Pool {
        private int id;
        private String name;
        private long availableCapacity;
        private long totalCapacity;
        private String healthStatus;
        private String redundancyPolicy;
        private int replicatedSize;
        private Timestamp createDate;
        private Timestamp lastOpDate;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getAvailableCapacity() {
            return availableCapacity;
        }

        public void setAvailableCapacity(long availableCapacity) {
            this.availableCapacity = availableCapacity;
        }

        public long getTotalCapacity() {
            return totalCapacity;
        }

        public void setTotalCapacity(long totalCapacity) {
            this.totalCapacity = totalCapacity;
        }

        public Timestamp getCreateDate() {
            return createDate;
        }

        public void setCreateDate(Timestamp createDate) {
            this.createDate = createDate;
        }

        public String getHealthStatus() {
            return healthStatus;
        }

        public void setHealthStatus(String healthStatus) {
            this.healthStatus = healthStatus;
        }

        public static Pool valueOf(PoolModule poolModule, BsPolicyModule policy, PoolCapacity poolCapacity) {
            PoolModule.PoolSpec poolSpec = poolModule.getSpec();
            Pool pool = new Pool();
            pool.setId(poolSpec.getId());
            pool.setName(poolSpec.getName());
            pool.setAvailableCapacity(poolCapacity.getAvailableCapacity());
            pool.setTotalCapacity(poolCapacity.getTotalCapacity());
            pool.setHealthStatus(poolModule.getMetadata().getState().getState());
            pool.setRedundancyPolicy(policy.getSpec().getDataReplicaType());
            pool.setReplicatedSize(policy.getSpec().getDataReplicaNum());
            pool.setCreateDate(XInfiniTimeUtil.translateToTimeStamp(poolSpec.getCreatedAt()));
            pool.setLastOpDate(XInfiniTimeUtil.translateToTimeStamp(poolSpec.getUpdatedAt()));
            return pool;
        }

        public Timestamp getLastOpDate() {
            return lastOpDate;
        }

        public void setLastOpDate(Timestamp lastOpDate) {
            this.lastOpDate = lastOpDate;
        }

        public String getRedundancyPolicy() {
            return redundancyPolicy;
        }

        public void setRedundancyPolicy(String redundancyPolicy) {
            this.redundancyPolicy = redundancyPolicy;
        }

        public int getReplicatedSize() {
            return replicatedSize;
        }

        public void setReplicatedSize(int replicatedSize) {
            this.replicatedSize = replicatedSize;
        }
    }
}
