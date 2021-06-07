package org.zstack.header.encryption;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 11/12/19.
 */
@Entity
@Table
public class LuksEncryptedImageVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    private String encryptUuid;

    @Column
    private String hashValue;

    @Column
    private String bindingVmUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEncryptUuid() {
        return encryptUuid;
    }

    public void setEncryptUuid(String encryptUuid) {
        this.encryptUuid = encryptUuid;
    }

    public String getHashValue() {
        return hashValue;
    }

    public void setHashValue(String hashValue) {
        this.hashValue = hashValue;
    }

    public String getBindingVmUuid() {
        return bindingVmUuid;
    }

    public void setBindingVmUuid(String bindingVmUuid) {
        this.bindingVmUuid = bindingVmUuid;
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
}
