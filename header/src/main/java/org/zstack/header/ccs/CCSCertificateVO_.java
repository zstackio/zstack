package org.zstack.header.ccs;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by Wenhao.Zhang on 21/10/15
 */
@StaticMetamodel(CCSCertificateVO.class)
public class CCSCertificateVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<CCSCertificateVO, String> algorithm;
    public static volatile SingularAttribute<CCSCertificateVO, String> format;
    public static volatile SingularAttribute<CCSCertificateVO, String> issuerDN;
    public static volatile SingularAttribute<CCSCertificateVO, String> subjectDN;
    public static volatile SingularAttribute<CCSCertificateVO, String> serNumber;
    public static volatile SingularAttribute<CCSCertificateVO, Timestamp> effectiveTime;
    public static volatile SingularAttribute<CCSCertificateVO, Timestamp> expirationTime;
    public static volatile SingularAttribute<CCSCertificateVO, Timestamp> createDate;
    public static volatile SingularAttribute<CCSCertificateVO, Timestamp> lastOpDate;
}
