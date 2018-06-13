package org.zstack.header.vo;

import org.zstack.header.core.StaticInit;
import org.zstack.utils.BeanUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResourceTypeMetadata {
    public static Map<Class, Class> concreteBaseTypeMapping = new HashMap<>();

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getTypesAnnotatedWith(BaseResource.class).forEach(bclz -> {
            BaseResource at = bclz.getAnnotation(BaseResource.class);
            if (at == null) {
                return;
            }

            BeanUtils.reflections.getSubTypesOf(bclz).forEach(cclz -> concreteBaseTypeMapping.put(cclz, bclz));
        });
    }

    public static Set<Class> getAllBaseTypes() {
        Set<Class> ret = new HashSet<>();
        ret.addAll(concreteBaseTypeMapping.values());
        return ret;
    }

    public static Class getBaseResourceType(Class clz) {
        Class bclz = concreteBaseTypeMapping.get(clz);
        return bclz == null ? clz : bclz;
    }
}
