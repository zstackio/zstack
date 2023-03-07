package org.zstack.authentication.checkfile;
import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class FileVerificationVO {
    @Id
    @Column
    private String uuid;

    @Column
    private String path;

    @Column
    private String category;

    @Column
    private String node;

    @Column
    private String hexType;

    @Column
    private String digest;

    @Column
    private String state;

    @Column
    private Timestamp lastOpDate;

    @Column
    private Timestamp createDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getHexType() {
        return hexType;
    }

    public void setHexType(String hexType) {
        this.hexType = hexType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public FileVerification toFile() {
        FileVerification f = new FileVerification();
        f.setUuid(this.getUuid());
        f.setPath(this.getPath());
        f.setNode(this.getNode());
        f.setHexType(this.getHexType());
        f.setCategory(this.getCategory());
        f.setDigest(this.getDigest());
        f.setState(this.getState());
        return f;
    }
}
