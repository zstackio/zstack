package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 16/10/27.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmQcowFileFind {
    private static final CLogger logger = Utils.getLogger(VmQcowFileFind.class);
    @Autowired
    private static DatabaseFacade dbf;

    public static String generateQcowFilePath(final FlowTrigger trigger, VmInstanceSpec spec){
        VmInstanceVO viVo = dbf.findByUuid(spec.getAccountPerference().getVmUuid(),
                VmInstanceVO.class);
        String qcowFilePath = getRootVolumeQcowPath(viVo);
        if(qcowFilePath == null)
            trigger.fail(new ErrorCode("NO_ROOTVOLUME_FOUND", "not dest root volume" +
                    "found in VolumeVO by vmUuid", String.format("not dest root volume found" +
                    " in VolumeVO by vmUuid: %s", spec.getAccountPerference().getVmUuid())));
        return qcowFilePath;
    }

    private static String getRootVolumeQcowPath(final VmInstanceVO viVo){
        logger.debug(String.format("get RootVolumePath begin, viVo is: %s", viVo.toString()));
        String volumeUuid = viVo.getRootVolumeUuid();
        if(volumeUuid == null)
            return null;
        VolumeVO vo = dbf.findByUuid(volumeUuid, VolumeVO.class);
        if(vo == null)
            return null;
        VolumeInventory vol = VolumeInventory.valueOf(vo);
        logger.debug(String.format("get RootVolumePath %s from VolumeVo by volumeUuid: %s", vol.getInstallPath(), volumeUuid));
        return vol.getInstallPath();
    }
}
