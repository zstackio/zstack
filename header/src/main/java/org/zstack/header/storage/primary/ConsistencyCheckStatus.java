package org.zstack.header.storage.primary;

public enum ConsistencyCheckStatus {
    /** VG found by WWID match, UUID matches the database — fully consistent */
    CONSISTENT,
    /** VG found by WWID match, but its UUID differs from the database — takeover candidate */
    UUID_MISMATCH,
    /** Hosts returned complete VG data, but no VG has a matching WWID set */
    VG_NOT_FOUND
}
