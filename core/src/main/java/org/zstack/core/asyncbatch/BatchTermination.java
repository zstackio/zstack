package org.zstack.core.asyncbatch;

public enum BatchTermination {
    ALL_ITEMS_PROCESSED,
    STOPPED_ON_FAILURE,
    STOPPED_BY_ITEM
}
