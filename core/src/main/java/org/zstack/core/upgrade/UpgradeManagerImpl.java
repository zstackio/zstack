package org.zstack.core.upgrade;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.SQL;
import org.zstack.header.agent.versioncontrol.AgentVersionVO;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class UpgradeManagerImpl implements ManagementNodeReadyExtensionPoint {

    private static final CLogger logger = Utils.getLogger(UpgradeManagerImpl.class);

    public void installUpdateExtension() {
        UpgradeGlobalConfig.GRAYSCALE_UPGRADE.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                if (!newConfig.value(Boolean.class)) {
                    changeGlobalConfig();
                }
            }
        });
    }

    public void changeGlobalConfig() {
        SQL.New(AgentVersionVO.class).hardDelete();
    }

    @Override
    public void managementNodeReady() {
        installUpdateExtension();
    }
}
