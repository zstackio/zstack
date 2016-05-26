package org.zstack.compute.vm;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by xing5 on 2016/5/26.
 */
public class IsoOperator {
    public String getIsoUuidByVmUuid(String vmUuid) {
        return VmSystemTags.ISO.getTokenByResourceUuid(vmUuid, VmSystemTags.ISO_TOKEN);
    }

    public void attachIsoToVm(String vmUuid, String isoUuid) {
        VmSystemTags.ISO.createInherentTag(vmUuid, map(e(VmSystemTags.ISO_TOKEN, isoUuid)));
    }

    public void detachIsoFromVm(String vmUuid) {
        VmSystemTags.ISO.deleteInherentTag(vmUuid);
    }

    public boolean isIsoAttachedToVm(String vmUuid) {
        return VmSystemTags.ISO.hasTag(vmUuid);
    }
}
