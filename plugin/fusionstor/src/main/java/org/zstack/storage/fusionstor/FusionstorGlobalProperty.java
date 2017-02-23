package org.zstack.storage.fusionstor;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by frank on 7/29/2015.
 */
@GlobalPropertyDefinition
public class FusionstorGlobalProperty {
    @GlobalProperty(name="Fusionstor.backupStorageAgent.port", defaultValue = "7763")
    public static int BACKUP_STORAGE_AGENT_PORT;
    @GlobalProperty(name="Fusionstor.primaryStorageAgent.port", defaultValue = "7764")
    public static int PRIMARY_STORAGE_AGENT_PORT;
    @GlobalProperty(name="Fusionstor.backupStorage.agentPackageName", defaultValue = "fusionstorbackupstorage-1.2.tar.gz")
    public static String BACKUP_STORAGE_PACKAGE_NAME;
    @GlobalProperty(name="Fusionstor.backupStorage.ansiblePlaybook", defaultValue = "fusionstorb.py")
    public static String BACKUP_STORAGE_PLAYBOOK_NAME;
    @GlobalProperty(name="Fusionstor.backupStorage.ansibleModulePath", defaultValue = "ansible/fusionstorb")
    public static String BACKUP_STORAGE_MODULE_PATH;
    @GlobalProperty(name="Fusionstor.primaryStorage.agentPackageName", defaultValue = "fusionstorprimarystorage-1.2.tar.gz")
    public static String PRIMARY_STORAGE_PACKAGE_NAME;
    @GlobalProperty(name="Fusionstor.primaryStorage.ansiblePlaybook", defaultValue = "fusionstorp.py")
    public static String PRIMARY_STORAGE_PLAYBOOK_NAME;
    @GlobalProperty(name="Fusionstor.primaryStorage.ansibleModulePath", defaultValue = "ansible/fusionstorp")
    public static String PRIMARY_STORAGE_MODULE_PATH;
}
