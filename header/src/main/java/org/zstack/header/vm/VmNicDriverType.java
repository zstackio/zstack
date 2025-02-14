package org.zstack.header.vm;

public enum VmNicDriverType {
    VIRTIO("virtio"),
    E1000_KVM("e1000"),
    PCNET("pcnet"),

    E1000_ESX("E1000"),
    VMXNET3("Vmxnet3");

    private String type;

    VmNicDriverType(String type) {
        this.type = type;
    }

    boolean isKvmType() {
        if (type.equals("virtio") || type.equals("e1000") || type.equals("pcnet")) {
            return true;
        }

        return false;
    }

    boolean isESXType() {
        if (type.equals("E1000") || type.equals("Vmxnet3")) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return type;
    }
}
