package org.zstack.physicalserver;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigInitExtensionPoint;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfigVO_;
import org.zstack.core.db.Q;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@GlobalConfigDefinition
public class PhysicalServerResourceAssignmentGlobalConfig implements
        GlobalConfigInitExtensionPoint {
    private static final Path CGROUP_ROOT = Paths.get("/sys/fs/cgroup");

    @GlobalConfigDef(
            type = Boolean.class,
            defaultValue = "false",
            description = "Enable physical server resource assignment enforcement")
    public static GlobalConfig ENABLED = new GlobalConfig(
            PhysicalServerResourceAssignmentConfig.CATEGORY,
            PhysicalServerResourceAssignmentConfig.ENABLED);

    @Override
    public List<GlobalConfig> getGenerationGlobalConfig() {
        GlobalConfigVO existing = Q.New(GlobalConfigVO.class)
                .eq(GlobalConfigVO_.category,
                        PhysicalServerResourceAssignmentConfig.CATEGORY)
                .eq(GlobalConfigVO_.name,
                        PhysicalServerResourceAssignmentConfig.ENABLED)
                .find();
        boolean enabled = defaultEnabled(
                existing == null ? null : existing.getDefaultValue(),
                CGROUP_ROOT);

        org.zstack.core.config.schema.GlobalConfig.Config config =
                new org.zstack.core.config.schema.GlobalConfig.Config();
        config.setCategory(PhysicalServerResourceAssignmentConfig.CATEGORY);
        config.setName(PhysicalServerResourceAssignmentConfig.ENABLED);
        config.setDescription(
                "Enable physical server resource assignment enforcement");
        config.setType(Boolean.class.getName());
        config.setDefaultValue(Boolean.toString(enabled));
        config.setValue(Boolean.toString(enabled));
        return Collections.singletonList(GlobalConfig.valueOf(config));
    }

    static boolean defaultEnabled(String existingDefault, Path cgroupRoot) {
        if (existingDefault != null) {
            return Boolean.parseBoolean(existingDefault);
        }
        return Files.isRegularFile(cgroupRoot.resolve("cgroup.controllers"));
    }
}
