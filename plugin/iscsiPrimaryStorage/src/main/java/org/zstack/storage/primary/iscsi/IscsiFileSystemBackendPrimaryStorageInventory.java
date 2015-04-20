package org.zstack.storage.primary.iscsi;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 4/19/2015.
 */
@Inventory(mappingVOClass = PrimaryStorageVO.class, collectionValueOfMethod = "valueOf1")
@PythonClassInventory
public class IscsiFileSystemBackendPrimaryStorageInventory extends PrimaryStorageInventory {
    private String chapUsername;
    private String chapPassword;
    private String hostname;
    private String sshUsername;
    private String sshPassword;
    private String filesystemType;

    protected IscsiFileSystemBackendPrimaryStorageInventory(IscsiFileSystemBackendPrimaryStorageVO vo) {
        super(vo);
        setChapPassword(vo.getChapPassword());
        setChapUsername(vo.getChapUsername());
        setHostname(vo.getHostname());
        setSshUsername(vo.getSshUsername());
        setSshPassword(vo.getSshPassword());
        setFilesystemType(vo.getFilesystemType());
    }

    public static IscsiFileSystemBackendPrimaryStorageInventory valueOf(IscsiFileSystemBackendPrimaryStorageVO vo) {
        return new IscsiFileSystemBackendPrimaryStorageInventory(vo);
    }

    public static List<IscsiFileSystemBackendPrimaryStorageInventory> valueOf1(Collection<IscsiFileSystemBackendPrimaryStorageVO> vos) {
        List<IscsiFileSystemBackendPrimaryStorageInventory> invs = new ArrayList<IscsiFileSystemBackendPrimaryStorageInventory>();
        for (IscsiFileSystemBackendPrimaryStorageVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
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
