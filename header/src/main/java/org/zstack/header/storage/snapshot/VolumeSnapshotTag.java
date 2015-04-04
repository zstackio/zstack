package org.zstack.header.storage.snapshot;

/**
 */
public enum VolumeSnapshotTag {
    CAPABILITY_HYPERVISOR_SNAPSHOT("capability:snapshot:hypervisor:%s", false);

    private String tag;
    private boolean completeTag;

    private VolumeSnapshotTag(String tag, boolean completeTag) {
        this.tag = tag;
        this.completeTag = completeTag;
    }

    @Override
    public String toString() {
        if (!completeTag) {
            throw new IllegalArgumentException("tag[" + tag + "] is not complete, call completeTag() instead of toString()");
        }
        return tag;
    }

    public String completeTag(String...args) {
        return String.format(tag, args);
    }
}
