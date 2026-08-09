package org.zstack.header.network.l2;

/** Identifies whether a local L2 create came from the public API or a typed projection. */
public enum NetworkOperationOrigin {
    API,
    ZNS_PROJECTION,
    ZNS_REFRESH
}
