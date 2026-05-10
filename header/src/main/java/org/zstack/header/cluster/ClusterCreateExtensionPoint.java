package org.zstack.header.cluster;

public interface ClusterCreateExtensionPoint {
    void afterCreateCluster(ClusterVO cluster);
}
