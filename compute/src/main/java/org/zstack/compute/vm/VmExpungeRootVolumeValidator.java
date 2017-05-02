package org.zstack.compute.vm;

import org.zstack.core.ScatteredValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by xing5 on 2017/5/2.
 */
public class VmExpungeRootVolumeValidator extends ScatteredValidator {

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface VmExpungeRootVolumeValidatorMethod {
    }

    static {
        // method signature: static void xxx(String vmUuid, String volumeUuid)
        collectValidatorMethods(VmExpungeRootVolumeValidatorMethod.class, String.class, String.class);
    }

    public void validate(String vmUuid, String rootVolumeUuid) {
        invokeValidatorMethods(vmUuid, rootVolumeUuid);
    }
}
