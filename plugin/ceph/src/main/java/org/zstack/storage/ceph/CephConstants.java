package org.zstack.storage.ceph;
/**
 * Created by frank on 7/27/2015.
 */

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface CephConstants {
    @PythonClass
    String CEPH_BACKUP_STORAGE_TYPE = "Ceph";
    @PythonClass
    String CEPH_PRIMARY_STORAGE_TYPE = "Ceph";

    String MON_PARAM_MON_PORT = "monPort";
}
