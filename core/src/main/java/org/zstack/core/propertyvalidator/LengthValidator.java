package org.zstack.core.propertyvalidator;

import org.zstack.utils.DebugUtils;

@ValidateTarget(target = Length.class)
public class LengthValidator implements GlobalPropertyValidator {
    @Override
    public boolean validate(String name, String value, Object rule) throws GlobalPropertyValidatorExecption {
        long[] lengthRange = (long[]) rule;
        if (value != null && lengthRange.length > 0) {
            DebugUtils.Assert(lengthRange.length == 2, String.format("invalid field [%s], Property.numberRange must have and only have 2 items", name));
            if (value.length() > lengthRange[1] || value.length() < lengthRange[0]) {
                throw new GlobalPropertyValidatorExecption("value " + value + " of property " + name + " must be in range of [ " + lengthRange[0] + " , " + lengthRange[1] + " ]");
            }
        }
        return true;
    }
}

