package org.zstack.header.core.progress;


import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ActionProgressVO.class)
public class ActionProgressVO_ {
    public static volatile SingularAttribute<ActionProgressVO, Long> id;
    public static volatile SingularAttribute<ActionProgressVO, String> apiId;
    public static volatile SingularAttribute<ActionProgressVO, String> content;
    public static volatile SingularAttribute<ActionProgressVO, String> opaque;
    public static volatile SingularAttribute<ActionProgressVO, Long> createTime;
    public static volatile SingularAttribute<ActionProgressVO, Long> lastOpTime;
    public static volatile SingularAttribute<ActionProgressVO, Long> currentStep;
    public static volatile SingularAttribute<ActionProgressVO, Long> totalStep;
}
