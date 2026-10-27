package org.zstack.kvm.memory;

import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.function.BiConsumer;

/** Hooks standard memory configuration into the existing GlobalConfig/ResourceConfig extension points. */
public final class MemoryStandardConfigurationCoordinator {
    private static final CLogger logger = Utils.getLogger(MemoryStandardConfigurationCoordinator.class);
    private final MemoryRepository repository;
    private final PluginRegistry registry;
    private final ThreadFacade thdf;
    private final BiConsumer<String, Boolean> changed;

    public MemoryStandardConfigurationCoordinator(MemoryRepository repository, PluginRegistry registry, ThreadFacade thdf,
                                                  BiConsumer<String, Boolean> changed) {
        this.repository = repository; this.registry = registry; this.thdf = thdf; this.changed = changed;
    }

    public void install(ResourceConfigFacade rcf) {
        for (MemoryStandardField field : MemoryStandardField.values()) {
            GlobalConfig global = field.config();
            // ResourceConfig also runs the GlobalConfig validators, so keep them scope-free.
            global.installValidateExtension((category, name, oldValue, newValue) -> validateScalar(field, newValue));
            global.installLocalBeforeUpdateExtension((oldConfig, newValue) -> validateChange(field, null, newValue));
            global.installLocalUpdateExtension((oldConfig, newConfig) -> changed(null, reenablesZram(field, newConfig.value())));
            ResourceConfig resource = rcf.getResourceConfig(global.getIdentity());
            resource.installValidatorExtension((resourceUuid, oldValue, newValue) -> validateChange(field, resourceUuid, newValue));
            resource.installLocalUpdateExtension((config, resourceUuid, resourceType, oldValue, newValue) ->
                    changed(resourceUuid, reenablesZram(field, newValue)));
            resource.installLocalDeleteExtension((config, resourceUuid, resourceType, originValue) -> changed(resourceUuid, false));
        }
    }

    /** Pure scalar validation. Cross-field rules are checked against the stored candidate in validateChange. */
    public static void validateScalar(MemoryStandardField field, String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Blank memory configuration is not an override; delete the resource override to inherit");
            }
            Object decoded = MemoryStandardConfigCodec.decode(field.path(), value);
            if (decoded instanceof Long && ((Long) decoded < 4096 || (Long) decoded % 4096 != 0)) {
                throw new IllegalArgumentException(field.path() + " must be a positive 4KiB-aligned byte count");
            }
        } catch (IllegalArgumentException invalid) { throw new GlobalConfigException(invalid.getMessage(), invalid); }
    }

    private void validateChange(MemoryStandardField field, String resourceUuid, String newValue) {
        validateScalar(field, newValue);
        // The legacy host.ksm switch predates memory optimization and stays license-free.
        if (field.config() != KVMGlobalConfig.HOST_KSM) {
            List<MemoryLicenseExtensionPoint> providers = registry.getExtensionList(MemoryLicenseExtensionPoint.class);
            if (providers.size() != 1 || providers.get(0).licenseDeadlineMillis() <= System.currentTimeMillis()) {
                throw new GlobalConfigException("A valid Cloud License is required for memory configuration changes");
            }
        }
        try {
            repository.validateStandardChange(field, resourceUuid, newValue);
        } catch (IllegalArgumentException invalid) {
            throw new GlobalConfigException(invalid.getMessage(), invalid);
        }
    }

    private static boolean reenablesZram(MemoryStandardField field, String value) {
        return field == MemoryStandardField.ZRAM_ENABLED && "true".equalsIgnoreCase(value == null ? null : value.trim());
    }

    /** The value is already committed; per-Host application is queued off the API thread. */
    private void changed(String resourceUuid, boolean zramEnabled) {
        thdf.submit(new Task<Void>() {
            @Override public String getName() { return "memory-standard-config-changed"; }
            @Override public Void call() {
                try {
                    changed.accept(resourceUuid, zramEnabled);
                } catch (RuntimeException e) {
                    logger.warn(String.format("unable to plan the memory configuration for resource[%s]; "
                            + "the next Host connect plans it", resourceUuid), e);
                }
                return null;
            }
        });
    }
}
