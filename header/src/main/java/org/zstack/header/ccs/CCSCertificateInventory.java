package org.zstack.header.ccs;


import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Wenhao.Zhang on 21/10/15
 */
@PythonClassInventory
@Inventory(mappingVOClass = CCSCertificateVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "userCertificateRefs", inventoryClass = CCSCertificateUserRefInventory.class,
                foreignKey = "certificateUuid", expandedInventoryKey = "uuid"),
})
public class CCSCertificateInventory implements Serializable {
    private String uuid;
    private String algorithm;
    private String format;
    private String issuerDN;
    private String subjectDN;
    private String serNumber;
    private Timestamp effectiveTime;
    private Timestamp expirationTime;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    @Queryable(mappingClass = CCSCertificateUserRefInventory.class,
            joinColumn = @JoinColumn(name = "certificateUuid"))
    private List<CCSCertificateUserRefInventory> userCertificateRefs;

    public CCSCertificateInventory() {
    }

    public CCSCertificateInventory(CCSCertificateVO vo) {
        this.uuid = vo.getUuid();
        this.algorithm = vo.getAlgorithm();
        this.format = vo.getFormat();
        this.issuerDN = vo.getIssuerDN();
        this.subjectDN = vo.getSubjectDN();
        this.serNumber = vo.getSerNumber();
        this.effectiveTime = new Timestamp(vo.getEffectiveTime());
        this.expirationTime = new Timestamp(vo.getExpirationTime());
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
        this.userCertificateRefs = CCSCertificateUserRefInventory.valueOf(vo.getUserCertificateRefs());
    }

    public static CCSCertificateInventory valueOf(CCSCertificateVO vo) {
        return new CCSCertificateInventory(vo);
    }

    public static List<CCSCertificateInventory> valueOf(Collection<CCSCertificateVO> vos) {
        return vos.stream().map(CCSCertificateInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getIssuerDN() {
        return issuerDN;
    }

    public void setIssuerDN(String issuerDN) {
        this.issuerDN = issuerDN;
    }

    public String getSubjectDN() {
        return subjectDN;
    }

    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    public String getSerNumber() {
        return serNumber;
    }

    public void setSerNumber(String serNumber) {
        this.serNumber = serNumber;
    }

    public Timestamp getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(Timestamp effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public Timestamp getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Timestamp expirationTime) {
        this.expirationTime = expirationTime;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public List<CCSCertificateUserRefInventory> getUserCertificateRefs() {
        return userCertificateRefs;
    }

    public void setUserCertificateRefs(List<CCSCertificateUserRefInventory> userCertificateRefs) {
        this.userCertificateRefs = userCertificateRefs;
    }
}
