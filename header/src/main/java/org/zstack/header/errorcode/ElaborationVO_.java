package org.zstack.header.errorcode;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 2018/12/1.
 */
@StaticMetamodel(ElaborationVO.class)
public class ElaborationVO_ {
    public static volatile SingularAttribute<ElaborationVO, Long> id;
    public static volatile SingularAttribute<ElaborationVO, String> errorInfo;
    public static volatile SingularAttribute<ElaborationVO, String> md5sum;
    public static volatile SingularAttribute<ElaborationVO, Double> distance;
    public static volatile SingularAttribute<ElaborationVO, Long> repeats;
    public static volatile SingularAttribute<ElaborationVO, Boolean> matched;
    public static volatile SingularAttribute<ElaborationVO, Timestamp> createDate;
    public static volatile SingularAttribute<ElaborationVO, Timestamp> lastOpDate;
}
