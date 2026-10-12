package org.zstack.kvm.memory;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigInitExtensionPoint;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfigVO_;
import org.zstack.core.db.Q;
import org.zstack.kvm.KVMGlobalConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Supplies first-install memory defaults before the first GlobalConfig row is
 * created. Existing clouds are identified by the legacy host.ksm row, which
 * predates this feature even when an old cloud has no KVM Host. The extension
 * is read-only; GlobalConfigFacade owns persistence and upgrade merge rules.
 */
public class MemoryFirstInstallGlobalConfigExtension implements GlobalConfigInitExtensionPoint {
    @Override
    public List<GlobalConfig> getGenerationGlobalConfig() {
        GlobalConfigVO hostKsm = Q.New(GlobalConfigVO.class)
                .eq(GlobalConfigVO_.category, KVMGlobalConfig.CATEGORY)
                .eq(GlobalConfigVO_.name, KVMGlobalConfig.HOST_KSM.getName()).find();
        if (hostKsm != null) {
            List<GlobalConfig> existing = new ArrayList<>();
            existing.add(config(KVMGlobalConfig.HOST_KSM, "enable host ksm",
                    hostKsm.getDefaultValue(), hostKsm.getValue()));
            preserveExisting(existing, MemoryStandardGlobalConfig.KSM_ZERO_PAGES);
            preserveExisting(existing, MemoryStandardGlobalConfig.ZRAM_ENABLED);
            return existing;
        }

        List<GlobalConfig> configs = new ArrayList<>();
        configs.add(config(MemoryStandardGlobalConfig.KSM_ZERO_PAGES,
                "KSM zero-page scanning", "true"));
        configs.add(config(MemoryStandardGlobalConfig.ZRAM_ENABLED,
                "Enable ZRAM", "true"));
        return configs;
    }

    private void preserveExisting(List<GlobalConfig> configs, GlobalConfig source) {
        GlobalConfigVO row = Q.New(GlobalConfigVO.class)
                .eq(GlobalConfigVO_.category, source.getCategory())
                .eq(GlobalConfigVO_.name, source.getName()).find();
        if (row != null) {
            configs.add(config(source, row.getDescription(), row.getDefaultValue(), row.getValue()));
        }
    }

    private GlobalConfig config(GlobalConfig source, String description, String value) {
        return config(source, description, value, value);
    }

    private GlobalConfig config(GlobalConfig source, String description, String defaultValue, String value) {
        org.zstack.core.config.schema.GlobalConfig.Config definition =
                new org.zstack.core.config.schema.GlobalConfig.Config();
        definition.setCategory(source.getCategory()); definition.setName(source.getName());
        definition.setDescription(description); definition.setDefaultValue(defaultValue); definition.setValue(value);
        definition.setType(String.class.getName());
        return GlobalConfig.valueOf(definition);
    }
}
