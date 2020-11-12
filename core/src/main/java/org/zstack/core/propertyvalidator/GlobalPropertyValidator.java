package org.zstack.core.propertyvalidator;

public interface GlobalPropertyValidator {
    boolean validate(String name, String value, Object rule) throws GlobalPropertyValidatorExecption;
}

