package org.zstack.header.cluster;

public interface ClusterFactory {
    ClusterType getType();

    ClusterVO createCluster(ClusterVO vo, APICreateClusterMsg msg);

    Cluster getCluster(ClusterVO vo);
}
