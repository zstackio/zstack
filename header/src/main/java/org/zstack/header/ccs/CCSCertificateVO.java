package org.zstack.header.ccs;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wenhao.Zhang on 21/10/15
 */
@Entity
@Table
@BaseResource
@AutoDeleteTag
public class CCSCertificateVO extends ResourceVO implements ToInventory {
    @Column
    private String algorithm;
    @Column
    private String format;
    @Column
    private String issuerDN;
    @Column
    private String subjectDN;
    @Column
    private String serNumber;
    @Column
    private Long effectiveTime;
    @Column
    private Long expirationTime;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="certificateUuid", insertable=false, updatable=false)
    @NoView
    private List<CCSCertificateUserRefVO> userCertificateRefs = new ArrayList<>();

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public Long getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(Long effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Long expirationTime) {
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

    public List<CCSCertificateUserRefVO> getUserCertificateRefs() {
        return userCertificateRefs;
    }

    public void setUserCertificateRefs(List<CCSCertificateUserRefVO> userCertificateRefs) {
        this.userCertificateRefs = userCertificateRefs;
    }

    @Override
    public String toString() {
        return "CCSCertificateVO{" +
        "uuid='" + uuid + '\'' +
        ", algorithm='" + algorithm + '\'' +
        ", format='" + format + '\'' +
        ", issuerDN='" + issuerDN + '\'' +
        ", subjectDN='" + subjectDN + '\'' +
        ", serNumber=" + serNumber +
        ", effectiveTime=" + effectiveTime +
        ", expirationTime=" + expirationTime +
        ", createDate=" + createDate +
        ", lastOpDate=" + lastOpDate +
        ", userCertificateRefs=" + userCertificateRefs +
        '}';
    }
}
