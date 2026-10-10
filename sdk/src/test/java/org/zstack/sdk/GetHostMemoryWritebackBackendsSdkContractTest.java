package org.zstack.sdk;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class GetHostMemoryWritebackBackendsSdkContractTest {
    @Test
    public void exposesRequiredHostParameterAndGetRoute() throws Exception {
        GetHostMemoryWritebackBackendsAction action = new GetHostMemoryWritebackBackendsAction();
        assertEquals("hostUuid", GetHostMemoryWritebackBackendsAction.class.getField("hostUuid").getName());
        assertTrue(GetHostMemoryWritebackBackendsAction.class.getField("hostUuid")
                .getAnnotation(Param.class).required());

        Method method = GetHostMemoryWritebackBackendsAction.class.getDeclaredMethod("getRestInfo");
        method.setAccessible(true);
        RestInfo info = (RestInfo) method.invoke(action);
        assertEquals("GET", info.httpMethod);
        assertEquals("/hosts/{hostUuid}/memory-writeback-backends", info.path);
        assertTrue(info.needSession);
        assertFalse(info.needPoll);
        assertEquals("", info.parameterName);
        assertEquals(MemoryWritebackBackendInventory.class, GetHostMemoryWritebackBackendsResult.class
                .getMethod("getInventory").getReturnType());
        java.lang.reflect.Field candidates = MemoryWritebackBackendInventory.class.getField("candidates");
        assertEquals(java.util.List.class, candidates.getType());
        assertEquals(MemoryWritebackBackendCandidateInventory.class,
                Class.forName("org.zstack.sdk.MemoryWritebackBackendCandidateInventory"));
        assertEquals(MemoryWritebackStableIdentityInventory.class,
                MemoryWritebackBackendCandidateInventory.class.getMethod("getStableIdentity").getReturnType());
    }
}
