package org.zstack.core.propertyvalidator;

import java.util.Arrays;

@ValidateTarget(target = AvailableValues.class)
public class AvailableValuesValidator implements GlobalPropertyValidator {
    @Override
    public boolean validate(String name, String value, Object rule) throws GlobalPropertyValidatorExecption {
        String[] validValues = (String[]) rule;
        if (value != null && validValues.length > 0) {
            if (Arrays.stream(validValues).noneMatch(it -> it.equals(value))) {
                throw new GlobalPropertyValidatorExecption(String.format("valid value of property [%s] are %s , actually found '%s'",
                        name, Arrays.toString(validValues), value));
            }
        }
        return true;
    }
}
