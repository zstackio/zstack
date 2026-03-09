package org.zstack.header.rest;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(RestAPIVO.class)
public class RestAPIVO_ {
    public static volatile SingularAttribute<RestAPIVO, String> uuid;
    public static volatile SingularAttribute<RestAPIVO, String> result;
    public static volatile SingularAttribute<RestAPIVO, String> apiMessageName;
    public static volatile SingularAttribute<RestAPIVO, Timestamp> createDate;
    public static volatile SingularAttribute<RestAPIVO, Timestamp> lastOpDate;
}
