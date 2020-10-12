package org.zstack.core.propertyvalidator;

import java.util.regex.Pattern;

@ValidateTarget(target = RegexValues.class)
public class RegexValuesValidator implements GlobalPropertyValidator {
    @Override
    public boolean validate(String name, String value, Object rule) throws GlobalPropertyValidatorExecption {
        String regexRule = (String) rule;
        if (value != null && !regexRule.trim().equals("")) {
            String pattern = regexRule.trim();
            boolean isMatch = Pattern.matches(pattern, value);
            if (!isMatch) {
                throw new GlobalPropertyValidatorExecption(String.format("invalid value [%s] for %s", value, name));
            }
        }
        return true;
    }
}
