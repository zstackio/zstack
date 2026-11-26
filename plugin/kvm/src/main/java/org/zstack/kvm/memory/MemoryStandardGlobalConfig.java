package org.zstack.kvm.memory;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.resourceconfig.BindResourceConfig;
import org.zstack.header.host.HostVO;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.header.vm.VmInstanceVO;

/** Standard ResourceConfig identities for scalar memory settings. */
@GlobalConfigDefinition
public class MemoryStandardGlobalConfig {
    private static final String CATEGORY = KVMGlobalConfig.CATEGORY;
    private static final String EMPTY = "";

    @GlobalConfigDef(defaultValue = "true", description = "Allow proactive reclaim of this VM's idle memory")
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig VM_ZRAM_RECLAIM_ENABLED = new GlobalConfig(CATEGORY, "memory.zram.reclaim.enabled");

    @GlobalConfigDef(defaultValue = EMPTY, description = "KSM zero-page scanning")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig KSM_ZERO_PAGES = new GlobalConfig(CATEGORY, "memory.ksm.zeroPagesEnabled");

    @GlobalConfigValidation(validValues = {"zram", "swap"})
    @GlobalConfigDef(defaultValue = "zram", description = "Memory reclaim backend")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig RECLAIM_BACKEND = new GlobalConfig(CATEGORY, "memory.reclaim.backend");

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

    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "true", description = "Migrate cold pages directly between ZRAM pools; false always migrates the ordinary way")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig SWAP_DIRECT_ENABLED = new GlobalConfig(CATEGORY, "memory.swapDirect.enabled");
}
