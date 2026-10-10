package org.zstack.kvm.memory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.resourceconfig.APIUpdateResourceConfigsMsg;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacadeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Regression coverage for the explicit ResourceConfig callback deferral boundary.
 * The existing woven transaction fixture provides the real JPA/AspectJ runtime;
 * this class owns independent N06 assertions without changing that fixture.
 */
public class MemoryResourceConfigCallbackCompatibilityTest {
    private MemoryConfigTransactionalFrameworkTest fixture;

    @Before
    public void setUp() throws Exception {
        fixture = new MemoryConfigTransactionalFrameworkTest();
        fixture.setUp();
    }

    @After
    public void tearDown() throws Exception {
        if (fixture != null) fixture.tearDown();
    }

    @Test
    public void ordinaryUpdateAndDeleteCallbacksAreSynchronousAndPropagateIntoRollback() throws Exception {
        ResourceConfig ordinary = ordinaryConfig();
        ordinary.installUpdateExtension((config, uuid, type, oldValue, newValue) -> {
            throw new IllegalStateException("N06 ordinary update callback failure");
        });
        try {
            ordinary.updateValue("a01-host", "should-rollback");
            fail("ordinary update callback must execute synchronously and propagate");
        } catch (IllegalStateException expected) {
            assertEquals("N06 ordinary update callback failure", expected.getMessage());
        }
        assertEquals("old-ordinary", invokeString("configValue", "ordinary"));

        ordinary.installDeleteExtension((config, uuid, type, originValue) -> {
            throw new IllegalStateException("N06 ordinary delete callback failure");
        });
        try {
            ordinary.deleteValue("a01-host");
            fail("ordinary delete callback must execute synchronously and propagate");
        } catch (IllegalStateException expected) {
            assertEquals("N06 ordinary delete callback failure", expected.getMessage());
        }
        // This assertion is intentionally about the legacy extension contract
        // (synchronous invocation and exception propagation), not a new promise
        // that every ResourceConfig SQL helper is transactionally reversible.
    }

    @Test
    public void mixedOptInBatchDefersNonParticipantCallbacksUntilWholeBatchCommits() throws Exception {
        setFixtureField("expectedAlpha", "new-alpha");
        setFixtureField("expectedOrdinary", "new-ordinary");
        setFixtureField("expectedMarkers", 4L);
        ResourceConfig ordinary = ordinaryConfig();
        AtomicBoolean callbackCalled = new AtomicBoolean();
        ordinary.installUpdateExtension((config, uuid, type, oldValue, newValue) -> {
            assertEquals("new-ordinary", invokeString("configValue", "ordinary"));
            assertEquals("participant markers must be visible when any batch callback runs", 4L,
                    invokeLong("markerCount"));
            callbackCalled.set(true);
        });

        APIUpdateResourceConfigsMsg request = new APIUpdateResourceConfigsMsg();
        request.setResourceUuid("a01-host");
        request.setSession((org.zstack.header.identity.SessionInventory) invoke("session"));
        APIUpdateResourceConfigsMsg.ResourceConfigAO alpha = new APIUpdateResourceConfigsMsg.ResourceConfigAO();
        alpha.setCategory("memory"); alpha.setName("alpha"); alpha.setValue("new-alpha");
        APIUpdateResourceConfigsMsg.ResourceConfigAO ordinaryAo = new APIUpdateResourceConfigsMsg.ResourceConfigAO();
        ordinaryAo.setCategory("compute"); ordinaryAo.setName("ordinary"); ordinaryAo.setValue("new-ordinary");
        request.setResourceConfigs(Arrays.asList(alpha, ordinaryAo));

        ResourceConfigFacadeImpl facade = (ResourceConfigFacadeImpl) fixtureField("facade");
        facade.handleMessage(request);

        assertTrue("nonparticipant callback must run after transaction commit", callbackCalled.get());
        assertEquals("new-alpha", invokeString("configValue", "alpha"));
        assertEquals("new-ordinary", invokeString("configValue", "ordinary"));
        assertEquals(4L, invokeLong("markerCount"));
        assertTrue("canonical observers must see the full committed batch",
                (Boolean) fixtureField("observerSawCommittedState"));
    }

    private ResourceConfig ordinaryConfig() throws Exception {
        List<ResourceConfig> configs = (List<ResourceConfig>) fixtureField("configs");
        return configs.get(2);
    }

    private Object fixtureField(String name) throws Exception {
        Field field = MemoryConfigTransactionalFrameworkTest.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(fixture);
    }

    private void setFixtureField(String name, Object value) throws Exception {
        Field field = MemoryConfigTransactionalFrameworkTest.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(fixture, value);
    }

    private Object invoke(String name, Object... args) throws Exception {
        Method method;
        if (args.length == 0) {
            method = MemoryConfigTransactionalFrameworkTest.class.getDeclaredMethod(name);
        } else {
            Class<?>[] types = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
            method = MemoryConfigTransactionalFrameworkTest.class.getDeclaredMethod(name, types);
        }
        method.setAccessible(true);
        return method.invoke(fixture, args);
    }

    private String invokeString(String name, String argument) {
        try {
            Method method = MemoryConfigTransactionalFrameworkTest.class.getDeclaredMethod(name, String.class);
            method.setAccessible(true);
            return (String) method.invoke(fixture, argument);
        } catch (Exception exception) {
            throw new AssertionError("fixture read failed: " + name, exception);
        }
    }

    private long invokeLong(String name) {
        try {
            return (Long) invoke(name);
        } catch (Exception exception) {
            throw new AssertionError("fixture read failed: " + name, exception);
        }
    }
}
