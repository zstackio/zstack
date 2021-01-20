package org.zstack.image;

import org.zstack.core.db.Q;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

public class ImageUtils {
    public static String getArchitectureFromRootVolume(String rootVolumeUuid, String architecture) {
        if (architecture != null) {
            return architecture;
        }

        String vmUuid = Q.New(VolumeVO.class).select(VolumeVO_.vmInstanceUuid).eq(VolumeVO_.uuid, rootVolumeUuid).findValue();
        VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
        String clusterUuid = vm.getClusterUuid();
        if (clusterUuid == null) {
            clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, vm.getLastHostUuid()).findValue();
        }
        return Q.New(ClusterVO.class).select(ClusterVO_.architecture).eq(ClusterVO_.uuid, clusterUuid).findValue();
    }
}
