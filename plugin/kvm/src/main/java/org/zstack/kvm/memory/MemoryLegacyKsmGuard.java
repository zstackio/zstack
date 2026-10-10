package org.zstack.kvm.memory;

import org.zstack.core.config.GlobalConfigException;

/** Protects the service-owned KSM writer from legacy host.ksm updates. */
public final class MemoryLegacyKsmGuard {
    private MemoryLegacyKsmGuard() { }

    public static void validate(boolean managed, String oldValue, String newValue) {
        if (managed) {
            throw new GlobalConfigException("MEMORY_CONTROLLER_CONFLICT: host KSM is owned by the memory controller; use the memory policy API");
        }
    }

    public static void validateDelete(boolean managed) {
        if (managed) {
            throw new GlobalConfigException("MEMORY_CONTROLLER_CONFLICT: host KSM is owned by the memory controller; use the memory policy API");
        }
    }
}
