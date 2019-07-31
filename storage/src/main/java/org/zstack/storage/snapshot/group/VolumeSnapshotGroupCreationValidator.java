package org.zstack.storage.snapshot.group;

import org.zstack.core.ScatteredValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by MaJin on 2019/7/31.
 */
public class VolumeSnapshotGroupCreationValidator extends ScatteredValidator {
    private static List<Method> methods;

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface VolumeSnapshotGroupCreationValidatorMethod {
    }

    static {
        // method signature: static void xxx(String vmUuid, String volumeUuid)
        methods = collectValidatorMethods(VolumeSnapshotGroupCreationValidatorMethod.class, String.class);
    }

    public static void validate(String vmUuid) {
        new VolumeSnapshotGroupCreationValidator().invokeValidatorMethods(methods, vmUuid);
    }
}
