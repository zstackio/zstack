package org.zstack.kvm.memory;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MemoryBatchCursorTest {
    @SuppressWarnings("unchecked")
    @Test public void failedFirstPageCannotStarveLaterResources() throws Exception {
        Class<?> type;
        try { type = Class.forName("org.zstack.kvm.memory.MemoryBatchCursor"); }
        catch (ClassNotFoundException e) { fail("Fair batch cursor is required"); return; }
        Object cursor = type.newInstance();
        java.lang.reflect.Method next = type.getDeclaredMethod("next", String.class, Collection.class, int.class);
        next.setAccessible(true);
        List<String> resources = Arrays.asList("c", "a", "b", "a", "d", "e");
        assertEquals(Arrays.asList("a", "b"), next.invoke(cursor, "host", resources, 2));
        assertEquals(Arrays.asList("c", "d"), next.invoke(cursor, "host", resources, 2));
        assertEquals(Arrays.asList("e", "a"), next.invoke(cursor, "host", resources, 2));
        assertEquals(Arrays.asList("a", "b"), next.invoke(cursor, "other", resources, 2));
        assertEquals(Arrays.asList("c", "e"), next.invoke(cursor, "host", Arrays.asList("c", "e"), 32));
        assertEquals(Collections.emptyList(), next.invoke(cursor, "host", Collections.emptyList(), 32));
    }
}
