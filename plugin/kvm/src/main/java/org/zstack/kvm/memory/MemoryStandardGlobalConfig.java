package org.zstack.kvm.memory;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.resourceconfig.BindResourceConfig;
import org.zstack.header.host.HostVO;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.kvm.KVMGlobalConfig;

/** Standard ResourceConfig identities for scalar memory settings. */
@GlobalConfigDefinition
public class MemoryStandardGlobalConfig {
    private static final String CATEGORY = KVMGlobalConfig.CATEGORY;
    private static final String EMPTY = "";

    @GlobalConfigDef(defaultValue = EMPTY, description = "KSM zero-page scanning")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig KSM_ZERO_PAGES = new GlobalConfig(CATEGORY, "memory.ksm.zeroPagesEnabled");

    @GlobalConfigDef(defaultValue = "false", description = "Enable ZRAM")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_ENABLED = new GlobalConfig(CATEGORY, "memory.zram.enabled");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM logical capacity in bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_LOGICAL_CAPACITY = new GlobalConfig(CATEGORY, "memory.zram.logicalCapacityBytes");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM physical RAM limit in bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_RAM_LIMIT = new GlobalConfig(CATEGORY, "memory.zram.ramLimitBytes");

    @GlobalConfigDef(defaultValue = "false", description = "Enable writeback")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig WRITEBACK_ENABLED = new GlobalConfig(CATEGORY, "memory.writeback.enabled");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Writeback backend capacity bytes; host scope only")
    @BindResourceConfig({HostVO.class})
    public static GlobalConfig WRITEBACK_BACKEND_CAPACITY = new GlobalConfig(CATEGORY, "memory.writeback.backendCapacityBytes");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Writeback backend resource UUID; host scope only")
    @BindResourceConfig({HostVO.class})
    public static GlobalConfig WRITEBACK_BACKEND_RESOURCE = new GlobalConfig(CATEGORY, "memory.writeback.backendResourceUuid");
}
