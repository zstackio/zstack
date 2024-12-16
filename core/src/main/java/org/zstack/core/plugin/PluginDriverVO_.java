package org.zstack.core.plugin;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(PluginDriverVO.class)
public class PluginDriverVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PluginDriverVO, String> uuid;
    public static volatile SingularAttribute<PluginDriverVO, String> name;
}
