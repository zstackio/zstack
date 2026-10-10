package org.zstack.kvm.memory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.db.DatabaseFacadeImpl;
import org.zstack.core.db.HardDeleteEntityExtensionPoint;
import org.zstack.header.identity.SessionInventory;
import org.zstack.resourceconfig.APIDeleteResourceConfigMsg;
import org.zstack.resourceconfig.APIUpdateResourceConfigMsg;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigCanonicalEvents;
import org.zstack.resourceconfig.ResourceConfigFacadeImpl;
import org.zstack.resourceconfig.ResourceConfigUpdateExtensionPoint;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/** Regression coverage on the real JPA/AspectJ fixture shared with the framework tests. */
public class MemoryResourceConfigPostCommitWovenTest {
    private MemoryConfigTransactionalFrameworkTest fixture;

    @Before
    public void setUp() throws Exception {
        fixture = new MemoryConfigTransactionalFrameworkTest();
        fixture.setUp();
        assertSame("fixture's operational DatabaseFacade is the real Impl, not the early default-return proxy",
                fixtureField("dbfImpl"), fixtureField("dbf"));
    }

    @After
    public void tearDown() throws Exception {
        if (fixture != null) fixture.tearDown();
    }

    @Test
    public void ordinaryUpdateExtensionRemainsSynchronousButCanonicalEventReadsCommittedValue() throws Exception {
        ResourceConfig ordinary = ordinary();
        AtomicBoolean extensionWasSynchronous = new AtomicBoolean();
        ordinary.installUpdateExtension((config, uuid, type, oldValue, newValue) -> {
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive());
            assertEquals("ordinary callback must stay synchronous", 0, canonicalEvents().size());
            assertEquals("independent transaction must not see the uncommitted value", "old-ordinary",
                    readConfig("ordinary"));
            extensionWasSynchronous.set(true);
        });

        setFixtureField("expectedOrdinary", "committed-update");
        update("committed-update");

        assertTrue(extensionWasSynchronous.get());
        assertEquals("committed-update", readConfig("ordinary"));
        assertEquals("exactly one local canonical event after commit", 1, canonicalEvents().size());
        assertTrue("event observer used an independent EntityManager and saw committed data",
                (Boolean) fixtureField("observerSawCommittedState"));
    }

    @Test
    public void ordinaryDeleteExtensionRemainsSynchronousAndCanonicalEventReadsCommittedDeletion() throws Exception {
        ResourceConfig ordinary = ordinary();
        AtomicBoolean extensionWasSynchronous = new AtomicBoolean();
        ordinary.installDeleteExtension((config, uuid, type, originValue) -> {
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive());
            assertEquals("ordinary delete extension remains synchronous", 0, canonicalEvents().size());
            extensionWasSynchronous.set(true);
        });

        setFixtureField("expectedOrdinary", null);
        delete();

        assertTrue(extensionWasSynchronous.get());
        assertNull(readConfig("ordinary"));
        assertEquals("exactly one local delete event after commit", 1, canonicalEvents().size());
        assertTrue("delete observer used an independent EntityManager and saw committed absence",
                (Boolean) fixtureField("observerSawCommittedState"));
    }

    @Test
    public void localCanonicalUpdateEventIsSuppressedOnOuterRollback() throws Exception {
        setFixtureField("expectedOrdinary", "rolled-back-update");
        updateThenRollback("rolled-back-update");
        assertEquals("old-ordinary", readConfig("ordinary"));
        assertTrue("afterCommit observer must not run on rollback", canonicalEvents().isEmpty());
    }

    @Test
    public void localCanonicalDeleteEventIsSuppressedOnOuterRollbackAndRowIsRestored() throws Exception {
        setFixtureField("expectedOrdinary", null);
        deleteThenRollback();
        assertEquals("resource row must be restored when the outer delete transaction rolls back",
                "old-ordinary", readConfig("ordinary"));
        assertTrue("delete event must not escape rollback", canonicalEvents().isEmpty());
    }

    @Test
    public void synchronousDeleteExtensionFailureRollsBackTheRowAndSuppressesCanonicalEvent() throws Exception {
        ResourceConfig ordinary = ordinary();
        ordinary.installDeleteExtension((config, uuid, type, originValue) -> {
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive());
            assertEquals("old-ordinary", originValue);
            assertTrue("canonical event must wait for commit", canonicalEvents().isEmpty());
            throw new IllegalStateException("delete extension failure");
        });
        try {
            ordinary.deleteValue("a01-host");
            fail("ordinary delete extension error must propagate synchronously");
        } catch (IllegalStateException expected) {
            assertEquals("delete extension failure", expected.getMessage());
        }
        assertEquals("delete extension failure must roll back the row", "old-ordinary", readConfig("ordinary"));
        assertTrue("delete extension failure must not emit a canonical event", canonicalEvents().isEmpty());
    }

    @Test
    public void transactionalHardDeleteNotifiesGenericExtensionsAfterCommit() throws Exception {
        ResourceConfig ordinary = ordinary();
        String rowUuid = ordinaryRowUuid();
        AtomicBoolean extensionSawCommittedAbsence = new AtomicBoolean();
        AtomicReference<Collection> notifiedIds = new AtomicReference<>();
        AtomicBoolean typedExtensionCalled = new AtomicBoolean();
        addTypedHardDeleteExtension(new HardDeleteEntityExtensionPoint() {
            @Override public List<Class> getEntityClassForHardDeleteEntityExtension() {
                return java.util.Collections.<Class>singletonList(org.zstack.resourceconfig.ResourceConfigVO.class);
            }
            @Override public void postHardDelete(Collection ids, Class entityClass) {
                typedExtensionCalled.set(true);
                assertEquals(org.zstack.resourceconfig.ResourceConfigVO.class, entityClass);
            }
        });
        addHardDeleteExtension(new HardDeleteEntityExtensionPoint() {
            @Override public List<Class> getEntityClassForHardDeleteEntityExtension() { return null; }
            @Override public void postHardDelete(Collection ids, Class entityClass) {
                assertEquals(org.zstack.resourceconfig.ResourceConfigVO.class, entityClass);
                try {
                    ids.clear();
                    fail("post-commit extension ids must be immutable");
                } catch (UnsupportedOperationException expected) {
                    // The callback receives an immutable snapshot of the deleted identifiers.
                }
                notifiedIds.set(new ArrayList(ids));
                extensionSawCommittedAbsence.set(!ordinaryRowExists(rowUuid));
            }
        });

        setFixtureField("expectedOrdinary", null);
        ordinary.deleteValue("a01-host");

        assertNull(readConfig("ordinary"));
        assertEquals(1, canonicalEvents().size());
        assertEquals(java.util.Collections.singletonList(rowUuid), new ArrayList<>(notifiedIds.get()));
        assertTrue("generic hard-delete extension must run after commit", extensionSawCommittedAbsence.get());
        assertTrue("typed extension registration must be preserved", typedExtensionCalled.get());
    }

    @Test
    public void transactionalHardDeleteDoesNotNotifyExtensionsWhenOuterTransactionRollsBack() throws Exception {
        AtomicBoolean notified = new AtomicBoolean();
        addHardDeleteExtension(new HardDeleteEntityExtensionPoint() {
            @Override public List<Class> getEntityClassForHardDeleteEntityExtension() { return null; }
            @Override public void postHardDelete(Collection ids, Class entityClass) { notified.set(true); }
        });

        setFixtureField("expectedOrdinary", null);
        deleteThenRollback();

        assertEquals("outer rollback restores ResourceConfigVO", "old-ordinary", readConfig("ordinary"));
        assertTrue(canonicalEvents().isEmpty());
        assertFalse("hard-delete extension must be withheld on rollback", notified.get());
    }

    @Test
    public void onePostCommitHardDeleteExtensionFailureDoesNotSkipTheRemainingExtensions() throws Exception {
        AtomicBoolean laterExtensionCalled = new AtomicBoolean();
        addHardDeleteExtension(new HardDeleteEntityExtensionPoint() {
            @Override public List<Class> getEntityClassForHardDeleteEntityExtension() { return null; }
            @Override public void postHardDelete(Collection ids, Class entityClass) {
                throw new IllegalStateException("post-commit cleanup failure");
            }
        });
        addHardDeleteExtension(new HardDeleteEntityExtensionPoint() {
            @Override public List<Class> getEntityClassForHardDeleteEntityExtension() { return null; }
            @Override public void postHardDelete(Collection ids, Class entityClass) { laterExtensionCalled.set(true); }
        });

        setFixtureField("expectedOrdinary", null);
        ordinary().deleteValue("a01-host");

        assertNull(readConfig("ordinary"));
        assertTrue("each after-commit extension is isolated", laterExtensionCalled.get());
    }

    @Test
    public void receivingRemoteCanonicalEventDoesNotRebroadcast() throws Exception {
        ResourceConfig ordinary = ordinary();
        invokeInstallEventTrigger(ordinary);
        AtomicReference<String> receivedUpdateValue = new AtomicReference<>();
        AtomicReference<String> receivedDeleteValue = new AtomicReference<>("not-called");
        ordinary.installUpdateExtension((config, uuid, type, oldValue, newValue) -> receivedUpdateValue.set(newValue));
        ordinary.installDeleteExtension((config, uuid, type, originValue) -> receivedDeleteValue.set(originValue));

        setFixtureField("expectedOrdinary", "remote-new");
        update("remote-new");
        assertEquals("local update event is emitted after commit", 1, canonicalEvents().size());
        canonicalEvents().clear();
        ResourceConfigCanonicalEvents.UpdateEvent remote = new ResourceConfigCanonicalEvents.UpdateEvent();
        remote.setResourceUuid("a01-host"); remote.setResourceType("ResourceVO"); remote.setOldValue("old-ordinary");
        invokeRemote("/resourceConfig/update/compute/ordinary/", remote);
        assertEquals("receiver reads the committed value from storage", "remote-new", receivedUpdateValue.get());
        assertTrue("update received from another MN is not rebroadcast", canonicalEvents().isEmpty());

        setFixtureField("expectedOrdinary", null);
        delete();
        assertEquals("local delete event is emitted after commit", 1, canonicalEvents().size());
        canonicalEvents().clear();
        ResourceConfigCanonicalEvents.DeleteEvent remoteDelete = new ResourceConfigCanonicalEvents.DeleteEvent();
        remoteDelete.setResourceUuid("a01-host"); remoteDelete.setResourceType("ResourceVO");
        remoteDelete.setOldValue("remote-new");
        invokeRemote("/resourceConfig/delete/compute/ordinary/", remoteDelete);
        assertNull("receiver sees the already-committed deletion", receivedDeleteValue.get());
        assertTrue("delete received from another MN is not rebroadcast", canonicalEvents().isEmpty());
    }

    private void invokeInstallEventTrigger(ResourceConfig config) throws Exception {
        java.lang.reflect.Method install = ResourceConfig.class.getDeclaredMethod("installEventTrigger");
        install.setAccessible(true); install.invoke(config);
    }

    private void invokeRemote(String pathPrefix, Object event) throws Exception {
        Map<String, EventCallback> callbacks = (Map<String, EventCallback>) fixtureField("callbacks");
        EventCallback receiver = callbacks.entrySet().stream().filter(entry -> entry.getKey().startsWith(pathPrefix))
                .map(Map.Entry::getValue).findFirst().orElseThrow(AssertionError::new);
        java.lang.reflect.Method run = EventCallback.class.getDeclaredMethod("run", Map.class, Object.class);
        run.setAccessible(true);
        run.invoke(receiver, java.util.Collections.singletonMap("nodeUuid", "remote-management-node"), event);

    }

    @SuppressWarnings("unchecked")
    private void addHardDeleteExtension(HardDeleteEntityExtensionPoint extension) throws Exception {
        DatabaseFacadeImpl dbf = (DatabaseFacadeImpl) fixtureField("dbfImpl");
        Field field = DatabaseFacadeImpl.class.getDeclaredField("hardDeleteForAllExtensions");
        field.setAccessible(true);
        ((List<HardDeleteEntityExtensionPoint>) field.get(dbf)).add(extension);
    }

    @SuppressWarnings("unchecked")
    private void addTypedHardDeleteExtension(HardDeleteEntityExtensionPoint extension) throws Exception {
        DatabaseFacadeImpl dbf = (DatabaseFacadeImpl) fixtureField("dbfImpl");
        Field field = DatabaseFacadeImpl.class.getDeclaredField("hardDeleteExtensions");
        field.setAccessible(true);
        Map<Class, List<HardDeleteEntityExtensionPoint>> extensions =
                (Map<Class, List<HardDeleteEntityExtensionPoint>>) field.get(dbf);
        extensions.computeIfAbsent(org.zstack.resourceconfig.ResourceConfigVO.class, ignored -> new ArrayList<>())
                .add(extension);
    }

    private String ordinaryRowUuid() throws Exception {
        javax.persistence.EntityManagerFactory factory =
                (javax.persistence.EntityManagerFactory) fixtureField("factory");
        javax.persistence.EntityManager em = factory.createEntityManager();
        try {
            return em.createQuery("select c.uuid from ResourceConfigVO c where c.resourceUuid=:resourceUuid " +
                            "and c.category=:category and c.name=:name", String.class)
                    .setParameter("resourceUuid", "a01-host").setParameter("category", "compute")
                    .setParameter("name", "ordinary").getSingleResult();
        } finally {
            em.close();
        }
    }

    private boolean ordinaryRowExists(String rowUuid) {
        try {
            javax.persistence.EntityManagerFactory factory =
                    (javax.persistence.EntityManagerFactory) fixtureField("factory");
            javax.persistence.EntityManager em = factory.createEntityManager();
            try {
                return em.find(org.zstack.resourceconfig.ResourceConfigVO.class, rowUuid) != null;
            } finally {
                em.close();
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Transactional
    public void updateThenRollback(String value) {
        update(value);
        assertTrue("nested ResourceConfig transaction must leave event pending", canonicalEvents().isEmpty());
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }

    @Transactional
    public void deleteThenRollback() {
        delete();
        assertTrue("nested ResourceConfig delete must leave event pending", canonicalEvents().isEmpty());
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }

    private void update(String value) {
        APIUpdateResourceConfigMsg msg = new APIUpdateResourceConfigMsg();
        msg.setCategory("compute"); msg.setName("ordinary"); msg.setResourceUuid("a01-host");
        msg.setValue(value); msg.setSession(session());
        facade().handleMessage(msg);
    }

    private void delete() {
        APIDeleteResourceConfigMsg msg = new APIDeleteResourceConfigMsg();
        msg.setCategory("compute"); msg.setName("ordinary"); msg.setResourceUuid("a01-host");
        msg.setSession(session());
        facade().handleMessage(msg);
    }

    private SessionInventory session() {
        SessionInventory session = new SessionInventory();
        session.setAccountUuid("test-account"); session.setUserUuid("test-user");
        return session;
    }

    @SuppressWarnings("unchecked")
    private ResourceConfig ordinary() throws Exception {
        return ((List<ResourceConfig>) fixtureField("configs")).get(2);
    }

    private ResourceConfigFacadeImpl facade() {
        try { return (ResourceConfigFacadeImpl) fixtureField("facade"); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    private List<String> canonicalEvents() {
        try { return (List<String>) fixtureField("canonicalEvents"); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    private String readConfig(String name) {
        try {
            java.lang.reflect.Method method = MemoryConfigTransactionalFrameworkTest.class
                    .getDeclaredMethod("configRowValue", String.class);
            method.setAccessible(true);
            return (String) method.invoke(fixture, name);
        } catch (Exception e) { throw new AssertionError(e); }
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
}
