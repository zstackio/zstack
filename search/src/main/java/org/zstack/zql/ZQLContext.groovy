package org.zstack.zql

class ZQLContext {
    private static ThreadLocal<Map<String, Object>> local = new ThreadLocal<Map<String, Object>>()

    private static void put(String k, Object v) {
        def map = local.get()
        if (map == null) {
            map = [:]
            local.set(map)
        }

        map[k] = v
    }

    private static Object get(String k) {
        def map = local.get()
        return map == null ? null : map[k]
    }

    private static final QUERY_TARGET_INVENTORY_NAME = "QUERY_TARGET_INVENTORY_NAME"

    static String getQueryTargetInventoryName() {
        return get(QUERY_TARGET_INVENTORY_NAME)
    }

    static void setQueryTargetInventoryName(String name) {
        put(QUERY_TARGET_INVENTORY_NAME, name)
    }
}
