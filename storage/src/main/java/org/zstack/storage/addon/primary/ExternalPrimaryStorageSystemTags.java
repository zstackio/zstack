package org.zstack.storage.addon.primary;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.EphemeralPatternSystemTag;

@TagDefinition
public class ExternalPrimaryStorageSystemTags {
    public static String REQUIRED_INSTALL_URL_TOKEN = "requiredInstallUrl";
    public static EphemeralPatternSystemTag REQUIRED_INSTALL_URL = new EphemeralPatternSystemTag(
            String.format("required::installUrl::{%s}", REQUIRED_INSTALL_URL_TOKEN),
            ClusterVO.class);
}