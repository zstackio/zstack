package org.zstack.storage.primary.iscsi;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by frank on 4/19/2015.
 */
@GlobalPropertyDefinition
public class IscsiFileSystemBackendPrimaryStorageGlobalProperty {
    @GlobalProperty(name = "IscsiFileSystemBackendPrimaryStorage.agentPort", defaultValue = "7759")
    public static int AGENT_PORT;
}
