package org.zstack.portal.apimediator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.ApiMessageValidator;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by Wenhao.Zhang on 21/01/13
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LicenseApiValidator implements ApiMessageValidator {
    
    @Autowired(required = false)
    private List<AddonModuleResourceApiValidator> validators;
    
    @Autowired
    private DatabaseFacade dbf;
    
    @Override
    public void validate(APIMessage msg, Field field, Object value, APIParam param) {
        if (validators == null) {
            return;
        }
    
        Class<?> type = param.resourceType();
        for (AddonModuleResourceApiValidator validator : validators) {
            if (validator.resourceTypeMatched(type) || validator.apiFieldMatched(msg.getClass(), field)) {
                validator.validate(msg, field, value, param);
            }
        }
    }
}
