package org.zstack.header.core.external.plugin;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(FeaturePluginVO.class)
public class FeaturePluginVO_ {
    public static volatile SingularAttribute<FeaturePluginVO, String> id;
    public static volatile SingularAttribute<FeaturePluginVO, String> version;
    public static volatile SingularAttribute<FeaturePluginVO, String> sha256;
    public static volatile SingularAttribute<FeaturePluginVO, String> packageName;
    public static volatile SingularAttribute<FeaturePluginVO, String> descriptor;
    public static volatile SingularAttribute<FeaturePluginVO, Timestamp> createDate;
    public static volatile SingularAttribute<FeaturePluginVO, Timestamp> lastOpDate;
}
