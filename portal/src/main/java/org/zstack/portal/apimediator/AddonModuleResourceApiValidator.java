package org.zstack.portal.apimediator;

import org.zstack.header.message.ApiMessageValidator;

import java.lang.reflect.Field;

public interface AddonModuleResourceApiValidator extends ApiMessageValidator {
    
    boolean resourceTypeMatched(Class<?> resourceType);
    
    default boolean apiFieldMatched(Class<?> apiMsgType, Field field) { return false; }
    
}
