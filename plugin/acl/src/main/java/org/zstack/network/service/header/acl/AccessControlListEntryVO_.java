package org.zstack.network.service.header.acl;

import org.apache.commons.net.ntp.TimeStamp;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-05
 **/
@StaticMetamodel(AccessControlListEntryVO.class)
public class AccessControlListEntryVO_ {
    public static volatile SingularAttribute<AccessControlListEntryVO, Long> entryId;
    public static volatile SingularAttribute<AccessControlListEntryVO, String> aclUuid;
    public static volatile SingularAttribute<AccessControlListEntryVO, String> ipEntries;
    public static volatile SingularAttribute<AccessControlListEntryVO, String> description;
    public static volatile SingularAttribute<AccessControlListEntryVO, TimeStamp> createDate;
    public static volatile SingularAttribute<AccessControlListEntryVO, TimeStamp> lastOpDate;

}
