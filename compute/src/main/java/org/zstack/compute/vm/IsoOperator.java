package org.zstack.compute.vm;

import org.zstack.tag.SystemTagCreator;

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
        SystemTagCreator creator = VmSystemTags.ISO.newSystemTagCreator(vmUuid);
        creator.setTagByTokens(map(e(VmSystemTags.ISO_TOKEN, isoUuid)));
        creator.inherent = true;
        creator.create();
    }

    public void detachIsoFromVm(String vmUuid) {
        VmSystemTags.ISO.deleteInherentTag(vmUuid);
    }

    public boolean isIsoAttachedToVm(String vmUuid) {
        return VmSystemTags.ISO.hasTag(vmUuid);
    }
}
