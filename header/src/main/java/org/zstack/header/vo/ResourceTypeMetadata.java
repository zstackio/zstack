package org.zstack.header.vo;

import org.zstack.header.core.StaticInit;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.utils.BeanUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResourceTypeMetadata {
    public static Map<Class, Class> concreteBaseTypeMapping = new HashMap<>();
    public static Set<Class> allResourceTypes = new HashSet<>();
    public static Set<Class> allBaseResourceTypes = new HashSet<>();

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getTypesAnnotatedWith(BaseResource.class).forEach(bclz -> {
            BaseResource at = bclz.getAnnotation(BaseResource.class);
            if (at == null) {
                return;
            }

            BeanUtils.reflections.getSubTypesOf(bclz).forEach(cclz -> concreteBaseTypeMapping.put(cclz, bclz));
        });

        allResourceTypes.addAll(BeanUtils.reflections.getSubTypesOf(OwnedByAccount.class));
        allResourceTypes.forEach(clz-> allBaseResourceTypes.add(getBaseResourceTypeFromConcreteType(clz)));
    }

    public static Set<Class> getAllBaseTypes() {
        return allBaseResourceTypes;
    }

    public static Class getBaseResourceTypeFromConcreteType(Class clz) {
        Class bclz = concreteBaseTypeMapping.get(clz);
        return bclz == null ? clz : bclz;
    }
}
