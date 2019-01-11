package org.zstack.compute.vm;

import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/5/26.
 */
public class IsoOperator {
    public static List<String> getIsoUuidByVmUuid2(String vmUuid) {
       List<String> isoUuids = Q.New(VmCdRomVO.class)
               .select(VmCdRomVO_.isoUuid)
               .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
               .notNull(VmCdRomVO_.isoUuid)
               .listValues();
       return isoUuids;
    }

    public static List<String> getVmUuidByIsoUuid2(String isoUuid) {
        List<String> result = Q.New(VmCdRomVO.class)
                .select(VmCdRomVO_.vmInstanceUuid)
                .eq(VmCdRomVO_.isoUuid, isoUuid)
                .listValues();
        return result;
    }

    static void checkAttachIsoToVm2(String vmUuid, String isoUuid) {
        List<String> isoList = getIsoUuidByVmUuid2(vmUuid);

        if (isoList.contains(isoUuid)) {
            throw new OperationFailureException(operr("VM[uuid:%s] has attached ISO[uuid:%s]", vmUuid, isoUuid));
        }

        long emptyCdRomNum = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
                .isNull(VmCdRomVO_.isoUuid)
                .count();
        if (emptyCdRomNum == 0) {
            throw new OperationFailureException(operr("All vm[uuid:%s] CD-ROMs have mounted ISO", vmUuid));
        }
    }

    public static Integer getIsoDeviceId2(String vmUuid, String isoUuid) {
        VmCdRomVO vmCdRomVO = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
                .eq(VmCdRomVO_.isoUuid, isoUuid)
                .find();

        if (vmCdRomVO == null) {
            return null;
        }

        return vmCdRomVO.getDeviceId();
    }

    public static boolean isIsoAttachedToVm2(String vmUuid) {
        boolean exsit = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
                .notNull(VmCdRomVO_.isoUuid)
                .isExists();

        return exsit;
    }

    static VmCdRomVO getEmptyCdRom(String vmUuid) {
        VmCdRomVO cdRomVO = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
                .isNull(VmCdRomVO_.isoUuid)
                .orderBy(VmCdRomVO_.deviceId, SimpleQuery.Od.ASC)
                .limit(1)
                .find();

        return cdRomVO;
    }
}
