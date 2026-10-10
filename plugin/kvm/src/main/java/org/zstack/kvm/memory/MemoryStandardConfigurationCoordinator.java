package org.zstack.kvm.memory;

import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.ConfigMutation;
import org.zstack.core.config.ConfigMutationContext;
import org.zstack.core.config.ConfigTransactionalMutationExtensionPoint;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.header.errorcode.OperationFailureException;

import javax.persistence.EntityManager;
import java.util.List;

/** One shared transaction participant for all ordinary memory configuration fields. */
public final class MemoryStandardConfigurationCoordinator implements ConfigTransactionalMutationExtensionPoint {
    private final MemoryRepository repository;
    private final PluginRegistry registry;

    public MemoryStandardConfigurationCoordinator(MemoryRepository repository, PluginRegistry registry) {
        this.repository = repository; this.registry = registry;
    }

    /** Pure scalar validation. Cross-field rules belong to the final candidate in the write transaction. */
    public static void validateScalar(MemoryStandardField field, String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Blank memory configuration is not an override; delete the resource override to inherit");
            }
            Object decoded = MemoryStandardConfigCodec.decode(field.path(), value);
            if (decoded != null) {
                MemoryPolicyRules.mergeOverridesOnly("{}", MemoryStandardPolicy.set("{}", field.path(), decoded), "Host");
            }
        } catch (IllegalArgumentException invalid) { throw new GlobalConfigException(invalid.getMessage(), invalid); }
    }

    @Override
    public void beforeMutations(EntityManager em, List<ConfigMutation> changes, ConfigMutationContext context) {
        try {
            List<MemoryLicenseExtensionPoint> providers = registry.getExtensionList(MemoryLicenseExtensionPoint.class);
            if (providers.size() != 1 || providers.get(0).licenseDeadlineMillis() <= System.currentTimeMillis()) {
                throw new MemoryOperationException("MEMORY_LICENSE_UNAVAILABLE",
                        "A valid Cloud License is required for memory configuration changes");
            }
            repository.prepareStandardMutations(em, changes, context);
        } catch (MemoryOperationException rejected) { throw new OperationFailureException(rejected.toErrorCode()); }
        catch (IllegalArgumentException invalid) { throw new GlobalConfigException(invalid.getMessage(), invalid); }
    }

    @Override
    public void afterMutations(EntityManager em, List<ConfigMutation> changes, ConfigMutationContext context) {
        try { repository.completeStandardMutations(em, changes, context); }
        catch (MemoryOperationException rejected) { throw new OperationFailureException(rejected.toErrorCode()); }
        catch (IllegalArgumentException invalid) { throw new GlobalConfigException(invalid.getMessage(), invalid); }
    }
}
