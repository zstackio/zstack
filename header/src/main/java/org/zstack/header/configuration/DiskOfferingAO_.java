package org.zstack.header.configuration;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(DiskOfferingAO.class)
public class DiskOfferingAO_ {
	public static volatile SingularAttribute<DiskOfferingAO, String> uuid;
	public static volatile SingularAttribute<DiskOfferingAO, Long> diskSize;
	public static volatile SingularAttribute<DiskOfferingAO, String> name;
	public static volatile SingularAttribute<DiskOfferingAO, String> description;
    public static volatile SingularAttribute<DiskOfferingAO, String> type;
    public static volatile SingularAttribute<DiskOfferingAO, DiskOfferingState> state;
	public static volatile SingularAttribute<DiskOfferingAO, Integer> sortKey;
	public static volatile SingularAttribute<DiskOfferingAO, Timestamp> createDate;
	public static volatile SingularAttribute<DiskOfferingAO, Timestamp> lastOpDate;
	public static volatile SingularAttribute<DiskOfferingAO, String> allocatorStrategy;
}
