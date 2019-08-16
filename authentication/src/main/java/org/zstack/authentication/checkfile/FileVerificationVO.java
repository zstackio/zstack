package org.zstack.authentication.checkfile;
import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class FileVerificationVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    private String path;

    @Column
    private String node;

    @Column
    private String type;

    @Column
    private String digest;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public FileVerification toFile() {
        FileVerification f = new FileVerification();
        f.setPath(this.getPath());
        f.setNode(this.getNode());
        f.setType(this.getType());
        f.setDigest(this.getDigest());
        return f;
    }
}
