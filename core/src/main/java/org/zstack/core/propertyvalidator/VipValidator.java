package org.zstack.core.propertyvalidator;

import org.zstack.core.CoreGlobalProperty;

@ValidateTarget(target = Vip.class)
public class VipValidator implements GlobalPropertyValidator {

    @Override
    public boolean validate(String name, String value, Object rule) throws GlobalPropertyValidatorExecption {
        boolean notVip = (boolean) rule;
        if (value != null && !notVip) {
            String MN_VIP = CoreGlobalProperty.MN_VIP;
            if (CoreGlobalProperty.MN_VIP != null && value.equals(MN_VIP)) {
                throw new GlobalPropertyValidatorExecption("invalid ip: " + value + " for " + name);
            }
        }
        return true;
    }
}

