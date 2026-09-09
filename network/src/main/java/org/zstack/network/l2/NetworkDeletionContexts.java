package org.zstack.network.l2;

import org.zstack.core.cascade.CascadeAction;
import org.zstack.header.network.l2.NetworkDeletionContext;

public final class NetworkDeletionContexts {
    private static final String KEY_PREFIX = NetworkDeletionContext.class.getName() + ":";
    private static final String PROJECTION_DELETE_KEY = KEY_PREFIX + "znsProjectionDelete";
    private static final String PREPARED_KEY_PREFIX = KEY_PREFIX + "prepared:";

    private NetworkDeletionContexts() {
    }

    public static NetworkDeletionContext get(CascadeAction action, String l2NetworkUuid) {
        return action.getContext(KEY_PREFIX + l2NetworkUuid);
    }

    public static void put(CascadeAction action, NetworkDeletionContext context) {
        action.putContext(KEY_PREFIX + context.getL2NetworkUuid(), context);
        if (context.isZnsSegmentProjectionDelete()) {
            action.putContext(PROJECTION_DELETE_KEY, context);
        }
    }

    public static NetworkDeletionContext projectionDelete(CascadeAction action) {
        return action.getContext(PROJECTION_DELETE_KEY);
    }

    public static boolean isPrepared(CascadeAction action, String l2NetworkUuid) {
        return Boolean.TRUE.equals(action.getContext(PREPARED_KEY_PREFIX + l2NetworkUuid));
    }

    public static void markPrepared(CascadeAction action, String l2NetworkUuid, boolean prepared) {
        action.putContext(PREPARED_KEY_PREFIX + l2NetworkUuid, prepared);
    }
}
