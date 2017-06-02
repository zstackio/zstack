package org.zstack.compute.vm;

import org.zstack.core.ScatteredValidator;
import org.zstack.header.vm.VmAttachVolumeValidatorMethod;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
/**
 * Created by MaJin on 2017-05-31.
 */
public class VmAttachVolumeValidator extends ScatteredValidator {
    private static final CLogger logger = Utils.getLogger(VmAttachVolumeValidator.class);
    static {
        // method signature: static void xxx(String vmUuid, String volumeUuid)
        collectValidatorMethods(VmAttachVolumeValidatorMethod.class, String.class, String.class);
    }

    public void validate(String vmUuid, String volumeUuid) {
        logger.debug("invoke validator");
        logger.debug(vmUuid);
        logger.debug(volumeUuid);
        invokeValidatorMethods(vmUuid, volumeUuid);
    }
}
