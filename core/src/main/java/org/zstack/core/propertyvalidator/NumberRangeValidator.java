package org.zstack.core.propertyvalidator;

import org.zstack.utils.DebugUtils;

@ValidateTarget(target = NumberRange.class)
public class NumberRangeValidator implements GlobalPropertyValidator {
    @Override
    public boolean validate(String name, String value, Object rule) throws GlobalPropertyValidatorExecption {
        long[] numberRange = (long[]) rule;
        if (value != null && numberRange.length > 0) {
            DebugUtils.Assert(numberRange.length == 2, String.format("invalid field [%s], Property.numberRange must have and only have 2 items", name));
            long val = Long.parseLong(value);
            if (val < numberRange[0] || val > numberRange[1]) {
                throw new GlobalPropertyValidatorExecption("value " + val + " of property" + name + " must be in range of [ " + numberRange[0] + " , " + numberRange[1] + " ]");
            }
        }
        return true;
    }
}

