package org.zstack.header.cluster;

/**
 * Created by GuoYi on 3/16/18
 */
public interface ClusterUpdateOSExtensionPoint {
    String preUpdateClusterOS(UpdateClusterOSStruct updateClusterOSStruct);
    void beforeUpdateClusterOS(ClusterVO cls);
    void afterUpdateClusterOS(ClusterVO cls);
}
