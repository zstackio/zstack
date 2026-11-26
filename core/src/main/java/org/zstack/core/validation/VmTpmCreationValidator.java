package org.zstack.core.validation;

import org.zstack.core.ScatteredValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;

public class VmTpmCreationValidator extends ScatteredValidator {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface VmTpmCreationValidatorMethod {
    }

    private static final List<Method> methods = collectValidatorMethods(
            VmTpmCreationValidatorMethod.class, String.class);

    public static void validate(String vmUuid) {
        new VmTpmCreationValidator().invokeValidatorMethods(methods, vmUuid);
    }
}
