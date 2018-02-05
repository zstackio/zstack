package org.zstack.storage.ceph.backup;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by mingjian.deng on 2017/10/31.
 */
@GlobalPropertyDefinition
public class CephBackupStorageGlobalProperty {
    // format like [{"PS":"Ceph", "priority":"5"},{"PS":"LocalStorage", "priority":"10"}], default priority value is 10
    @GlobalProperty(name="ceph.backupstorage.primary.storage.priority", defaultValue = "[{\"PS\":\"Ceph\", \"priority\":\"2\"}]")
    public static String CEPH_BACKUPSTORAGE_PRIORITY;
}
