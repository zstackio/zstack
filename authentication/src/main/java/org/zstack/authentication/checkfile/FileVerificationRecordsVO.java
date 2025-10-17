package org.zstack.authentication.checkfile;
import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class FileVerificationRecordsVO {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String fileVerificationUuid;

    @Column
    private String path;

    @Column
    private String node;

    @Column
    private String currentDigest;

    @Column
    private String targetDigest;

    @Column
    private String reason;

    @Column
    private boolean recoverFlag;

    @Column
    private Timestamp lastOpDate;

    @Column
    private Timestamp createDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFileVerificationUuid() {
        return fileVerificationUuid;
    }

    public void setFileVerificationUuid(String fileVerificationUuid) {
        this.fileVerificationUuid = fileVerificationUuid;
    }

    public String getCurrentDigest() {
        return currentDigest;
    }

    public void setCurrentDigest(String currentDigest) {
        this.currentDigest = currentDigest;
    }

    public String getTargetDigest() {
        return targetDigest;
    }

    public void setTargetDigest(String targetDigest) {
        this.targetDigest = targetDigest;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isRecoverFlag() {
        return recoverFlag;
    }

    public void setRecoverFlag(boolean recoverFlag) {
        this.recoverFlag = recoverFlag;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
