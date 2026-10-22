package org.zstack.header.vm;

/** The guest-visible model supported by a NIC backend, independent of the guest OS. */
public enum VmNicDriverModel {
    Emulated,
    Virtio,
    Passthrough
}
