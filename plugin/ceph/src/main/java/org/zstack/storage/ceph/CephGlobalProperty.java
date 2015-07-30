package org.zstack.storage.ceph;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by frank on 7/29/2015.
 */
@GlobalPropertyDefinition
public class CephGlobalProperty {
    @GlobalProperty(name="Ceph.backupStorageAgent.port", defaultValue = "7761")
    public static int BACKUP_STORAGE_AGENT_PORT;
    @GlobalProperty(name="Ceph.primaryStorageAgent.port", defaultValue = "7762")
    public static int PRIMARY_STORAGE_AGENT_PORT;
}
