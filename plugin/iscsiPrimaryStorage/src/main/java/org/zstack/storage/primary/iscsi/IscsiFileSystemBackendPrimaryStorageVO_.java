package org.zstack.storage.primary.iscsi;

import org.zstack.header.storage.primary.PrimaryStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by frank on 4/20/2015.
 */
@StaticMetamodel(IscsiFileSystemBackendPrimaryStorageVO.class)
public class IscsiFileSystemBackendPrimaryStorageVO_ extends PrimaryStorageVO_ {
    public static volatile SingularAttribute<IscsiFileSystemBackendPrimaryStorageVO, String> chapUsername;
    public static volatile SingularAttribute<IscsiFileSystemBackendPrimaryStorageVO, String> chapPassword;
    public static volatile SingularAttribute<IscsiFileSystemBackendPrimaryStorageVO, String> hostname;
    public static volatile SingularAttribute<IscsiFileSystemBackendPrimaryStorageVO, String> sshUsername;
    public static volatile SingularAttribute<IscsiFileSystemBackendPrimaryStorageVO, String> sshPassword;
    public static volatile SingularAttribute<IscsiFileSystemBackendPrimaryStorageVO, String> filesystemType;
}
