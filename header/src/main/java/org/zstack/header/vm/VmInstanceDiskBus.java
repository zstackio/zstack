package org.zstack.header.vm;

public enum VmInstanceDiskBus {
    VIRTIO("virtio"),
    IDE("ide");

    private String name;

    VmInstanceDiskBus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static VmInstanceDiskBus findByName(String name) {
        for (VmInstanceDiskBus type: values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
