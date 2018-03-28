package org.zstack.zql

import org.zstack.header.identity.SessionInventory
import org.zstack.header.zql.ZQLExtensionContext

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

    private static Object computeIfAbsent(String k, Object obj) {
        Object ob = get(k)
        if (ob == null) {
            ob = obj
            put(k, ob)
        }

        return ob
    }

    private static final QUERY_TARGET_INVENTORY_NAME = "QUERY_TARGET_INVENTORY_NAME"
    private static final QUERY_TARGET_INVENTORY_STACK = "QUERY_TARGET_INVENTORY_STACK"
    private static final API_SESSION = "API_SESSION"

    static String getQueryTargetInventoryName() {
        return get(QUERY_TARGET_INVENTORY_NAME)
    }

    static void setQueryTargetInventoryName(String name) {
        put(QUERY_TARGET_INVENTORY_NAME, name)
    }

    static void putAPISession(SessionInventory session) {
        put(API_SESSION, session)
    }

    static SessionInventory getAPISession() {
        return get(API_SESSION) as SessionInventory
    }

    static void pushQueryTargetInventoryName(String targetInventoryName) {
        Stack<String> stack = computeIfAbsent(QUERY_TARGET_INVENTORY_STACK, new Stack()) as Stack<String>
        stack.push(targetInventoryName)
    }

    static String popQueryTargetInventoryName() {
        Stack<String> stack = get(QUERY_TARGET_INVENTORY_STACK) as Stack<String>
        assert stack != null : "no query target inventory statck yet"
        return stack.pop()
    }

    static String peekQueryTargetInventoryName() {
        Stack<String> stack = get(QUERY_TARGET_INVENTORY_STACK) as Stack<String>
        assert stack != null : "no query target inventory statck yet"
        return stack.peek()
    }

    static class ZQLExtensionContextImpl implements ZQLExtensionContext {
        @Override
        String getQueryTargetInventoryName() {
            return ZQLContext.getQueryTargetInventoryName()
        }

        @Override
        SessionInventory getAPISession() {
            return ZQLContext.getAPISession()
        }
    }

    static ZQLExtensionContext createZQLExtensionContext() {
        return new ZQLExtensionContextImpl()
    }
}
