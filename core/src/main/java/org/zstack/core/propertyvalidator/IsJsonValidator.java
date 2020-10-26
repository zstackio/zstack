package org.zstack.core.propertyvalidator;

import java.util.ArrayList;

import org.zstack.header.storage.backup.PrimaryStoragePriorityGetter;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 *  * Created by LiangHanYu on 2020/10/24 12:53
 *   */

@ValidateTarget(target = IsJson.class)
public class IsJsonValidator implements GlobalPropertyValidator {
    @Override
    public boolean validate(String name, String value, Object rule) throws GlobalPropertyValidatorExecption {
        if (value == null || value.isEmpty()) {
            return true;
        }
        try {
            JSONObjectUtil.toCollection(value, ArrayList.class, PrimaryStoragePriorityGetter.PriorityMap.class);
        } catch (RuntimeException e) {
            throw new GlobalPropertyValidatorExecption(String.format("invalid json value %s for %s", value, name));
        }
        return true;
    }
}
