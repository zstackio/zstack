package org.zstack.header.identity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ResourceResponsibleVO.class)
public abstract class ResourceResponsibleVO_ {

	public static volatile SingularAttribute<ResourceResponsibleVO, String> uuid;
	public static volatile SingularAttribute<ResourceResponsibleVO, String> resourceUuid;
	public static volatile SingularAttribute<ResourceResponsibleVO, String> responsibleType;
	public static volatile SingularAttribute<ResourceResponsibleVO, String> responsibleUuid;
	public static volatile SingularAttribute<ResourceResponsibleVO, Timestamp> lastOpDate;
	public static volatile SingularAttribute<ResourceResponsibleVO, Timestamp> createDate;
}
