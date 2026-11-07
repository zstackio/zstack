package org.zstack.kvm.memory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * The repository rules that a running management node cannot be driven into: stored data that cannot be
 * read, a lost first insert and an event bus that fails. Database behavior is covered by the Groovy cases.
 */
public class MemoryRepositoryRulesTest {
    private MemoryStateVO stored;
    private HostMemoryStateVO publicState;
    private boolean failingFire;
    private final List<Object> fired = new ArrayList<>();
    private MemoryRepository repository;
    private EntityManager em;
    private Object encryptAspect;
    private Field registryField;
    private Object previousRegistry;

    @Before public void setup() throws Exception {
        // The product's woven persist advice requires a registry even for entities
        // with no encrypted fields. Keep that advice active with an empty test registry.
        Class<?> aspect = Class.forName("org.zstack.core.aspect.EncryptColumnAspect");
        encryptAspect = aspect.getMethod("aspectOf").invoke(null);
        registryField = aspect.getDeclaredField("pluginRegistry"); registryField.setAccessible(true);
        previousRegistry = registryField.get(encryptAspect);
        registryField.set(encryptAspect, Proxy.newProxyInstance(PluginRegistry.class.getClassLoader(),
                new Class<?>[]{PluginRegistry.class}, (proxy, method, args) -> Collections.emptyList()));
        java.lang.reflect.Method metadataInit = org.zstack.core.db.EntityMetadata.class.getDeclaredMethod("staticInit");
        metadataInit.setAccessible(true); metadataInit.invoke(null);
        stored = new MemoryStateVO(); stored.setHostUuid("host1"); stored.setStatus(MemoryRepository.SUCCEEDED);
        stored.setDesiredRevision(3); stored.setAppliedRevision(3L); stored.setControl("active");
        stored.setDesired("{\"control\":\"active\",\"ksm\":{\"enabled\":true},\"zram\":{},\"writeback\":{}}");
        em = (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(),
                new Class<?>[]{EntityManager.class}, (proxy, method, args) -> {
                    if ("find".equals(method.getName())) {
                        return args[0] == MemoryStateVO.class ? stored : args[0] == HostMemoryStateVO.class ? publicState : null;
                    }
                    if ("persist".equals(method.getName()) && args[0] instanceof HostMemoryStateVO) { publicState = (HostMemoryStateVO) args[0]; }
                    return null;
                });
        repository = new MemoryRepository() {
            @Override protected <T> T transaction(Function<EntityManager, T> function) { return function.apply(em); }
        };
        Field evtf = MemoryRepository.class.getDeclaredField("evtf"); evtf.setAccessible(true);
        evtf.set(repository, Proxy.newProxyInstance(EventFacade.class.getClassLoader(), new Class<?>[]{EventFacade.class},
                (proxy, method, args) -> {
                    if ("fire".equals(method.getName())) {
                        if (failingFire) { throw new IllegalStateException("event bus stopped"); }
                        fired.add(args[1]);
                    }
                    return null;
                }));
    }

    @After public void restore() throws Exception {
        registryField.set(encryptAspect, previousRegistry);
    }

    private static MemoryAgentResponse ksmSample(long generation, String tunedState) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.setSuccess(true); response.status = MemoryRepository.SUCCEEDED;
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("sampleTime", System.currentTimeMillis()); state.put("control", "active");
        state.put("appliedGeneration", generation);
        Map<String, Object> ksm = new LinkedHashMap<>();
        ksm.put("enabled", "active".equals(tunedState) ? Boolean.TRUE : null);
        ksm.put("serviceState", "active"); ksm.put("tunedServiceState", tunedState);
        state.put("ksm", ksm);
        response.state = state;
        return response;
    }

    @Test public void retryDelayIsCappedAtTenMinutes() {
        assertEquals(30000, MemoryRepository.retryDelay(1));
        assertEquals(60000, MemoryRepository.retryDelay(2));
        assertEquals(480000, MemoryRepository.retryDelay(5));
        assertEquals(600000, MemoryRepository.retryDelay(6));
        assertEquals(600000, MemoryRepository.retryDelay(40));
    }

    @Test public void anUnreadableStoredConfigurationIsShownAndPlannedAgain() {
        assertEquals(Collections.emptyMap(), MemoryRepository.storedDesired(new MemoryStateVO()));
        stored.setDesired("{broken");
        assertNull("an unreadable configuration is not read as an empty one", MemoryRepository.storedDesired(stored));
        assertFalse("the caller plans the Host again", repository.refreshHostState(em, stored, null));
        assertEquals(MemoryRepository.CONVERGING, publicState.getStatus());
        assertEquals(MemoryRepository.CONFIGURATION_UNREADABLE, publicState.getReason());
    }

    @Test public void anUnreadableObservationIsShownAsUnknownWithItsReason() {
        stored.setState("{broken"); stored.setLastSampleTime(System.currentTimeMillis());
        assertTrue(repository.refreshHostState(em, stored, null));
        assertEquals("Unknown", publicState.getFeatureState());
        assertEquals(MemoryRepository.OBSERVATION_UNREADABLE, publicState.getReason());
    }

    @Test public void anApplyReplyWithoutASampleKeepsTheLastSampleAndItsTime() {
        repository.observe("host1", ksmSample(3, "active"));
        String sample = stored.getState();
        Long sampledAt = stored.getLastSampleTime();
        MemoryAgentResponse reply = new MemoryAgentResponse();
        reply.setSuccess(true); reply.status = MemoryRepository.CONVERGING; reply.reasonCode = "APPLYING";
        // The Agent could not observe the Host after applying; its reply names only why.
        reply.state = new LinkedHashMap<>(Collections.singletonMap("reason", "service status unavailable"));
        repository.record("host1", 3, reply);
        assertEquals("a reply without a sample keeps the last one", sample, stored.getState());
        assertEquals("and the time it was taken", sampledAt, stored.getLastSampleTime());
        assertEquals("the reply still counts for the configuration", MemoryRepository.CONVERGING, stored.getStatus());
    }

    @Test public void anEventThatCannotBeFiredReachesTheCallerAndKeepsTheSample() {
        repository.observe("host1", ksmSample(3, "active"));
        failingFire = true;
        try {
            repository.observe("host1", ksmSample(3, "failed"));
            fail("an event that cannot be fired is an error for the caller");
        } catch (IllegalStateException expected) {
            assertTrue(stored.getState().contains("\"tunedServiceState\":\"failed\""));
        }
    }
}
