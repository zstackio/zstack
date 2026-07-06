package org.zstack.storage.addon.primary;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.resourceconfig.BindResourceConfig;

@GlobalConfigDefinition
public class ExternalPrimaryStorageGlobalConfig {
    public static final String CATEGORY = "externalPrimaryStorage";

    @GlobalConfigValidation(validValues = {"iSCSI", "NVMEoF", "CBD"})
    @GlobalConfigDef(defaultValue = "iSCSI", description = "image export protocol of external primary storage")
    @BindResourceConfig({PrimaryStorageVO.class})
    public static GlobalConfig IMAGE_EXPORT_PROTOCOL = new GlobalConfig(CATEGORY, "image.export.protocol");

    @GlobalConfigValidation()
    @GlobalConfigDef(defaultValue = "0.3", type = Double.class, description = "when attaching external primary storage to a cluster, " +
            "if the ratio of hosts that fail to deploy the storage client reaches this threshold, the attach fails; " +
            "otherwise it succeeds and the failed hosts recover through periodic ping self-heal")
    public static GlobalConfig ATTACH_HOST_DEPLOY_FAILURE_RATIO_THRESHOLD = new GlobalConfig(CATEGORY, "attach.hostDeployFailureRatioThreshold");
}