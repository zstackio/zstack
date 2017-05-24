package org.zstack.compute.vm;

import org.zstack.core.ScatteredValidator;
import org.zstack.header.vm.VmAttachVolumeValidatorMethod;

/**
 * Created by MaJin on 2017-05-31.
 */
public class VmAttachVolumeValidator extends ScatteredValidator {
    static {
        // method signature: static void xxx(String vmUuid, String volumeUuid)
        collectValidatorMethods(VmAttachVolumeValidatorMethod.class, String.class, String.class);
    }

    public void validate(String vmUuid, String volumeUuid) {
        invokeValidatorMethods(vmUuid, volumeUuid);
    }
}
