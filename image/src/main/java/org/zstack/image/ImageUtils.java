package org.zstack.image;

import org.zstack.core.db.Q;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

public class ImageUtils {
    public static String getArchitectureFromRootVolume(String rootVolumeUuid, String architecture) {
        if (architecture == null) {
            String vmUuid = Q.New(VolumeVO.class).select(VolumeVO_.vmInstanceUuid).eq(VolumeVO_.uuid, rootVolumeUuid).findValue();
            String clusterUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.clusterUuid).eq(VmInstanceVO_.uuid, vmUuid).findValue();
            return Q.New(ClusterVO.class).select(ClusterVO_.architecture).eq(ClusterVO_.uuid, clusterUuid).findValue();
        }
        return architecture;
    }
}
