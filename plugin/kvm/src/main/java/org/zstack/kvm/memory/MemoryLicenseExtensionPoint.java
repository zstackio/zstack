package org.zstack.kvm.memory;

/** Existing Cloud entitlement only; an absent provider fails closed. */
public interface MemoryLicenseExtensionPoint {
    /** Absolute expiry of the Cloud License, or 0 without one; a license that cannot be read throws. */
    long licenseDeadlineMillis();
}
