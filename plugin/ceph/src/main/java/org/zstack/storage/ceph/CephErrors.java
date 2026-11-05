package org.zstack.storage.ceph;

public enum CephErrors {
    MIGRATION_NETWORK_PRECHECK_FAILED(1000);

    private String code;

    CephErrors(int id) {
        code = String.format("CEPH.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
