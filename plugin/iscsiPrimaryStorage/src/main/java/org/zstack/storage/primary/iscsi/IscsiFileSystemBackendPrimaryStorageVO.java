package org.zstack.storage.primary.iscsi;

import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * Created by frank on 4/19/2015.
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = PrimaryStorageEO.class, needView = false)
@AutoDeleteTag
public class IscsiFileSystemBackendPrimaryStorageVO extends PrimaryStorageVO {
    @Column
    private String chapUsername;
    @Column
    private String chapPassword;
    @Column
    private String hostname;
    @Column
    private String sshUsername;
    @Column
    private String sshPassword;
    @Column
    private String filesystemType;

    public IscsiFileSystemBackendPrimaryStorageVO(PrimaryStorageVO other) {
        super(other);
    }

    public IscsiFileSystemBackendPrimaryStorageVO(IscsiFileSystemBackendPrimaryStorageVO other) {
        super(other);
        this.chapUsername = other.chapUsername;
        this.chapPassword = other.chapPassword;
        this.hostname = other.hostname;
        this.sshUsername = other.sshUsername;
        this.sshPassword = other.sshPassword;
        this.filesystemType = other.filesystemType;
    }

    public IscsiFileSystemBackendPrimaryStorageVO() {
    }

    public String getChapUsername() {
        return chapUsername;
    }

    public void setChapUsername(String chapUsername) {
        this.chapUsername = chapUsername;
    }

    public String getChapPassword() {
        return chapPassword;
    }

    public void setChapPassword(String chapPassword) {
        this.chapPassword = chapPassword;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public String getFilesystemType() {
        return filesystemType;
    }

    public void setFilesystemType(String filesystemType) {
        this.filesystemType = filesystemType;
    }
}
