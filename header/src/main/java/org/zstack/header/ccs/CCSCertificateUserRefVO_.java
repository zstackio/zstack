package org.zstack.header.ccs;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by Wenhao.Zhang on 21/10/15
 */
@StaticMetamodel(CCSCertificateUserRefVO.class)
public class CCSCertificateUserRefVO_ {
    public static volatile SingularAttribute<CCSCertificateUserRefVO, Integer> id;
    public static volatile SingularAttribute<CCSCertificateUserRefVO, String> userUuid;
    public static volatile SingularAttribute<CCSCertificateUserRefVO, String> certificateUuid;
    public static volatile SingularAttribute<CCSCertificateUserRefVO, CCSCertificateUserState> state;
    public static volatile SingularAttribute<CCSCertificateUserRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<CCSCertificateUserRefVO, Timestamp> lastOpDate;
}
