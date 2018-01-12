package org.zstack.storage.surfs;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by zhouhaiping 2017-09-01
 */
@GlobalPropertyDefinition
public class SurfsGlobalProperty {
    @GlobalProperty(name="Surfs.backupStorageAgent.port", defaultValue = "6732")
    public static int BACKUP_STORAGE_AGENT_PORT;
    @GlobalProperty(name="Surfs.primaryStorageAgent.port", defaultValue = "6731")
    public static int PRIMARY_STORAGE_AGENT_PORT;
    @GlobalProperty(name="Surfs.backupStorage.agentPackageName", defaultValue = "surfsbackupstorage-2.2.0.tar.gz")
    public static String BACKUP_STORAGE_PACKAGE_NAME;
    @GlobalProperty(name="Surfs.backupStorage.ansiblePlaybook", defaultValue = "surfsb.py")
    public static String BACKUP_STORAGE_PLAYBOOK_NAME;
    @GlobalProperty(name="Surfs.backupStorage.ansibleModulePath", defaultValue = "ansible/surfsb")
    public static String BACKUP_STORAGE_MODULE_PATH;
    @GlobalProperty(name="Surfs.primaryStorage.agentPackageName", defaultValue = "surfsprimarystorage-2.2.0.tar.gz")
    public static String PRIMARY_STORAGE_PACKAGE_NAME;
    @GlobalProperty(name="Surfs.primaryStorage.ansiblePlaybook", defaultValue = "surfsp.py")
    public static String PRIMARY_STORAGE_PLAYBOOK_NAME;
    @GlobalProperty(name="Surfs.primaryStorage.ansibleModulePath", defaultValue = "ansible/surfsp")
    public static String PRIMARY_STORAGE_MODULE_PATH;
    @GlobalProperty(name="Surfs.type", defaultValue = "Surfs")
    public static String SURFS_PRIMARY_STORAGE_TYPE;
}
