package org.zstack.storage.primary.iscsi;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by frank on 4/19/2015.
 */
@PythonClass
public class IscsiPrimaryStorageConstants {
    @PythonClass
    public static final String ISCSI_FILE_SYSTEM_BACKEND_PRIMARY_STORAGE_TYPE = "IscsiFileSystemBackendPrimaryStorage";

    public static final String ANSIBLE_MODULE_PATH = "ansible/iscsi";
}
