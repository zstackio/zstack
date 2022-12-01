package org.zstack.sdk;



public class CCSCertificateInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String algorithm;
    public void setAlgorithm(java.lang.String algorithm) {
        this.algorithm = algorithm;
    }
    public java.lang.String getAlgorithm() {
        return this.algorithm;
    }

    public java.lang.String format;
    public void setFormat(java.lang.String format) {
        this.format = format;
    }
    public java.lang.String getFormat() {
        return this.format;
    }

    public java.lang.String issuerDN;
    public void setIssuerDN(java.lang.String issuerDN) {
        this.issuerDN = issuerDN;
    }
    public java.lang.String getIssuerDN() {
        return this.issuerDN;
    }

    public java.lang.String subjectDN;
    public void setSubjectDN(java.lang.String subjectDN) {
        this.subjectDN = subjectDN;
    }
    public java.lang.String getSubjectDN() {
        return this.subjectDN;
    }

    public java.lang.String serNumber;
    public void setSerNumber(java.lang.String serNumber) {
        this.serNumber = serNumber;
    }
    public java.lang.String getSerNumber() {
        return this.serNumber;
    }

    public java.sql.Timestamp effectiveTime;
    public void setEffectiveTime(java.sql.Timestamp effectiveTime) {
        this.effectiveTime = effectiveTime;
    }
    public java.sql.Timestamp getEffectiveTime() {
        return this.effectiveTime;
    }

    public java.sql.Timestamp expirationTime;
    public void setExpirationTime(java.sql.Timestamp expirationTime) {
        this.expirationTime = expirationTime;
    }
    public java.sql.Timestamp getExpirationTime() {
        return this.expirationTime;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

    public java.util.List userCertificateRefs;
    public void setUserCertificateRefs(java.util.List userCertificateRefs) {
        this.userCertificateRefs = userCertificateRefs;
    }
    public java.util.List getUserCertificateRefs() {
        return this.userCertificateRefs;
    }

}
