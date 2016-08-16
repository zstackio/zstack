package org.zstack.storage.primary.iscsi;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by frank on 4/19/2015.
 */
@GlobalPropertyDefinition
public class IscsiFileSystemBackendPrimaryStorageGlobalProperty {
    @GlobalProperty(name = "IscsiFileSystemBackendPrimaryStorage.agentPort", defaultValue = "7760")
    public static int AGENT_PORT;
    @GlobalProperty(name = "IscsiFileSystemBackendPrimaryStorage.agentPackageName", defaultValue = "iscsifilesystemagent-1.6.tar.gz")
    public static String AGENT_PACKAGE_NAME;
    @GlobalProperty(name = "IscsiFileSystemBackendPrimaryStorage.ansiblePlaybook", defaultValue = "iscsi.py")
    public static String ANSIBLE_PLAYBOOK_NAME;
    @GlobalProperty(name = "IscsiFileSystemBackendPrimaryStorage.ansibleModulePath", defaultValue = "ansible/iscsi")
    public static String ANSIBLE_MODULE_PATH;
}
