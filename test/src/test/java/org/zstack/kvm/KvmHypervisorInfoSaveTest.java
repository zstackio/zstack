package org.zstack.kvm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.wiring.BeanConfigurerSupport;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.aspectj.AnnotationTransactionAspect;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.util.ReflectionUtils;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DatabaseGlobalProperty;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.GetVirtualizerInfoMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMAgentCommands.GetVirtualizerInfoRsp;
import org.zstack.kvm.KVMAgentCommands.VirtualizerInfoTO;
import org.zstack.kvm.hypervisor.KvmHypervisorInfoExtensions;
import org.zstack.kvm.hypervisor.KvmHypervisorInfoManager;
import org.zstack.kvm.hypervisor.KvmHypervisorInfoManagerImpl;
import org.zstack.kvm.hypervisor.KvmHypervisorMetadataStore;
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoVO;
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoVO_;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.SingularAttribute;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class KvmHypervisorInfoSaveTest {
    private static final String HOST_UUID = "61150909000040008000000000000011";
    private static final String VM_UUID = "4a6173618107489e8013f294e37a533d";
    private final Map<Field, Object> originalStaticFields = new HashMap<>();
    private final Map<String, KvmHypervisorInfoVO> committedRows = new ConcurrentHashMap<>();
    private DatabaseFacade db;
    private CloudBus bus;
    private Connection connection;
    private TestTransactionManager transactions;
    private TransactionManager originalTransactionManager;
    private Object originalBeanConfigurerSupport;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        originalTransactionManager = AnnotationTransactionAspect.aspectOf().getTransactionManager();
        replaceStatic(DatabaseGlobalProperty.class, "GLockWaitTimeout", TimeUnit.HOURS.toSeconds(8));
        Field configurer = ReflectionUtils.findField(AnnotationBeanConfigurerAspect.class, "beanConfigurerSupport");
        ReflectionUtils.makeAccessible(configurer);
        originalBeanConfigurerSupport = ReflectionUtils.getField(configurer, AnnotationBeanConfigurerAspect.aspectOf());
        setField(AnnotationBeanConfigurerAspect.aspectOf(), "beanConfigurerSupport", new BeanConfigurerSupport());
        db = mock(DatabaseFacade.class);
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("databaseFacade", db);
        AutowiredAnnotationBeanPostProcessor autowiring = new AutowiredAnnotationBeanPostProcessor();
        autowiring.setBeanFactory(beanFactory);
        beanFactory.addBeanPostProcessor(autowiring);
        bus = mock(CloudBus.class);
        DataSource dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        when(db.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(statement);
        when(connection.isValid(anyInt())).thenReturn(true);
        when(statement.executeQuery()).thenReturn(result);
        when(result.first()).thenReturn(true);
        when(result.next()).thenReturn(true);
        when(result.getInt(1)).thenReturn(1);

        ComponentLoader loader = mock(ComponentLoader.class);
        when(loader.getComponent(DatabaseFacade.class)).thenReturn(db);
        when(loader.getComponent(KvmHypervisorMetadataStore.class)).thenReturn(mock(KvmHypervisorMetadataStore.class));
        replaceStatic(Platform.class, "loader", loader);
        AnnotationBeanConfigurerAspect.aspectOf().setBeanFactory(beanFactory);
        for (String name : Arrays.asList("uuid", "architecture", "osDistribution", "osRelease", "osVersion")) {
            SingularAttribute attribute = mock(SingularAttribute.class);
            when(attribute.getJavaType()).thenReturn(String.class);
            replaceStatic(KVMHostVO_.class, name, attribute);
        }
        replaceStatic(KvmHypervisorInfoVO_.class, "uuid", mock(SingularAttribute.class));

        CriteriaBuilder builder = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);
        EntityManager entityManager = mock(EntityManager.class);
        when(db.getCriteriaBuilder()).thenReturn(builder);
        when(db.getEntityManager()).thenReturn(entityManager);
        TypedQuery emptyQuery = mock(TypedQuery.class);
        when(emptyQuery.getResultList()).thenReturn(Collections.emptyList());
        when(entityManager.createQuery(builder.createTupleQuery())).thenReturn(emptyQuery);
        CriteriaQuery<KvmHypervisorInfoVO> infoQuery = builder.createQuery(KvmHypervisorInfoVO.class);
        TypedQuery<KvmHypervisorInfoVO> infoRows = mock(TypedQuery.class);
        when(entityManager.createQuery(infoQuery)).thenReturn(infoRows);
        when(infoRows.getResultList()).thenAnswer(invocation -> new ArrayList<>(committedRows.values()));

        transactions = new TestTransactionManager();
        AnnotationTransactionAspect.aspectOf().setTransactionManager(transactions);
        doAnswer(invocation -> {
            Collection<KvmHypervisorInfoVO> rows = invocation.getArgument(0);
            transactions.current.get().pending.addAll(rows);
            return null;
        }).when(db).persistCollection(anyCollection());
        doAnswer(invocation -> {
            Collection<KvmHypervisorInfoVO> rows = invocation.getArgument(0);
            transactions.current.get().pending.addAll(rows);
            return null;
        }).when(db).updateCollection(anyCollection());
    }

    @After
    public void tearDown() {
        AnnotationTransactionAspect.aspectOf().setTransactionManager(originalTransactionManager);
        setField(AnnotationBeanConfigurerAspect.aspectOf(), "beanConfigurerSupport", originalBeanConfigurerSupport);
        originalStaticFields.forEach((field, value) -> ReflectionUtils.setField(field, null, value));
    }

    @Test
    public void insertsThenUpdatesThroughBothSaveEntries() throws Exception {
        KvmHypervisorInfoManagerImpl manager = manager();
        manager.save(report());
        assertEquals(2, committedRows.size());
        assertEquals("4.2.0", committedRows.get(VM_UUID).getVersion());
        VirtualizerInfoTO updated = info(HOST_UUID);
        updated.setVersion("6.2.0");
        manager.saveHostInfo(updated);
        assertEquals("6.2.0", committedRows.get(HOST_UUID).getVersion());
        assertEquals(2, transactions.committed.get());
        verify(db).persistCollection(anyCollection());
        verify(db).updateCollection(anyCollection());
        verify(connection, times(2)).close();
    }

    @Test
    public void serializesSavesAcrossManagersUntilCommitCompletes() throws Exception {
        CountDownLatch committing = new CountDownLatch(1);
        CountDownLatch releaseCommit = new CountDownLatch(1);
        CountDownLatch secondSubmitted = new CountDownLatch(1);
        AtomicInteger commits = new AtomicInteger();
        transactions.beforeCommit = () -> {
            if (commits.getAndIncrement() == 0) {
                committing.countDown();
                await(releaseCommit);
            }
        };
        KvmHypervisorInfoManagerImpl first = manager();
        KvmHypervisorInfoManagerImpl second = manager();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstSave = executor.submit(() -> first.save(report()));
            assertTrue(committing.await(5, TimeUnit.SECONDS));
            Future<?> secondSave = executor.submit(() -> {
                secondSubmitted.countDown();
                second.save(report());
            });
            assertTrue(secondSubmitted.await(5, TimeUnit.SECONDS));
            try {
                secondSave.get(200, TimeUnit.MILLISECONDS);
                fail("another save must wait while the first transaction is committing");
            } catch (TimeoutException expected) {
                assertTrue(committedRows.isEmpty());
                verify(connection, never()).close();
                verify(db).persistCollection(anyCollection());
            }
            releaseCommit.countDown();
            firstSave.get(5, TimeUnit.SECONDS);
            secondSave.get(5, TimeUnit.SECONDS);
            assertEquals(2, committedRows.size());
            assertEquals(2, transactions.committed.get());
            verify(db).persistCollection(anyCollection());
            verify(db).updateCollection(anyCollection());
            verify(connection, times(2)).close();
        } finally {
            releaseCommit.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void rollsBackAndUnlocksAfterWriteFailure() throws Exception {
        RuntimeException failure = new RuntimeException("write failed");
        doThrow(failure).when(db).persistCollection(anyCollection());
        try {
            manager().save(report());
            fail("write failure must propagate");
        } catch (RuntimeException e) {
            assertSame(failure, e);
        }
        assertTrue(committedRows.isEmpty());
        assertEquals(1, transactions.rolledBack.get());
        verify(connection).close();
        doNothing().when(db).persistCollection(anyCollection());
        manager().save(report());
        assertEquals(1, transactions.committed.get());
        verify(connection, times(2)).close();
    }

    @Test
    public void unlocksAfterCommitFailure() throws Exception {
        RuntimeException failure = new RuntimeException("commit failed");
        transactions.beforeCommit = () -> { throw failure; };
        try {
            manager().save(report());
            fail("commit failure must propagate");
        } catch (RuntimeException e) {
            assertSame(failure, e);
        }
        assertTrue(committedRows.isEmpty());
        verify(connection).close();
        transactions.beforeCommit = () -> {};
        manager().save(report());
        assertEquals(2, committedRows.size());
        verify(connection, times(2)).close();
    }

    @Test
    public void refreshesOnMigrationUsingOperationHost() {
        KvmHypervisorInfoManager manager = mock(KvmHypervisorInfoManager.class);
        KvmHypervisorInfoExtensions extensions = extensions(manager);
        KVMHostInventory host = new KVMHostInventory();
        host.setUuid(HOST_UUID);
        VmInstanceSpec spec = new VmInstanceSpec();
        spec.setDestHost(host);
        VmInstanceInventory vm = new VmInstanceInventory();
        vm.setUuid(VM_UUID);
        vm.setHostUuid("previous-host");
        KVMAgentCommands.VmDevicesInfoResponse deviceRsp = new KVMAgentCommands.VmDevicesInfoResponse();
        deviceRsp.setVirtualizerInfo(info(VM_UUID));
        KVMAgentCommands.RebootVmResponse rebootRsp = new KVMAgentCommands.RebootVmResponse();
        rebootRsp.setVirtualizerInfo(info(VM_UUID));

        extensions.afterReceiveVmDeviceInfoResponse(null, deviceRsp, spec);
        extensions.rebootVmOnKvmSuccess(host, vm, rebootRsp);
        vm.setHostUuid(HOST_UUID);
        extensions.afterReceiveVmDeviceInfoResponse(vm, deviceRsp, null);
        extensions.afterMigrateVm(vm, "previous-host");

        verify(manager, times(3)).saveVmInfo(any(VirtualizerInfoTO.class));
        ArgumentCaptor<GetVirtualizerInfoMsg> messages = ArgumentCaptor.forClass(GetVirtualizerInfoMsg.class);
        verify(bus, times(1)).send(messages.capture(), any(CloudBusCallBack.class));
        GetVirtualizerInfoMsg msg = messages.getValue();
        assertEquals(HOST_UUID, msg.getHostUuid());
        assertEquals(Collections.singletonList(VM_UUID), msg.getVmInstanceUuids());
        verify(bus).makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, HOST_UUID);
        verify(bus, never()).call(any(GetVirtualizerInfoMsg.class));
        ArgumentCaptor<CloudBusCallBack> callbacks = ArgumentCaptor.forClass(CloudBusCallBack.class);
        verify(bus, times(1)).send(any(GetVirtualizerInfoMsg.class), callbacks.capture());
        MessageReply failed = new MessageReply();
        ErrorCode error = new ErrorCode();
        error.setCode("test.refresh.failed");
        failed.setError(error);
        callbacks.getAllValues().forEach(callback -> callback.run(failed));
        verifyNoMoreInteractions(manager);
    }

    @Test
    public void skipsRefreshWhenTheResponseHasNoVirtualizerInfo() {
        KvmHypervisorInfoManager manager = mock(KvmHypervisorInfoManager.class);
        KvmHypervisorInfoExtensions extensions = extensions(manager);
        extensions.afterReceiveVmDeviceInfoResponse(null, new KVMAgentCommands.VmDevicesInfoResponse(), null);
        extensions.rebootVmOnKvmSuccess(null, null, new KVMAgentCommands.RebootVmResponse());
        verifyNoInteractions(bus, manager);
    }

    private KvmHypervisorInfoExtensions extensions() {
        return extensions(mock(KvmHypervisorInfoManager.class));
    }

    private KvmHypervisorInfoExtensions extensions(KvmHypervisorInfoManager manager) {
        KvmHypervisorInfoExtensions extensions = new KvmHypervisorInfoExtensions();
        setField(extensions, "bus", bus);
        setField(extensions, "manager", manager);
        return extensions;
    }

    private KvmHypervisorInfoManagerImpl manager() {
        KvmHypervisorInfoManagerImpl manager = new KvmHypervisorInfoManagerImpl();
        setField(manager, "db", db);
        return manager;
    }

    private GetVirtualizerInfoRsp report() {
        GetVirtualizerInfoRsp rsp = new GetVirtualizerInfoRsp();
        rsp.setHostInfo(info(HOST_UUID));
        rsp.setVmInfoList(Collections.singletonList(info(VM_UUID)));
        return rsp;
    }

    private VirtualizerInfoTO info(String uuid) {
        VirtualizerInfoTO info = new VirtualizerInfoTO();
        info.setUuid(uuid);
        info.setVirtualizer("qemu-kvm");
        info.setVersion("4.2.0");
        return info;
    }

    private void setField(Object target, String name, Object value) {
        Field field = ReflectionUtils.findField(target.getClass(), name);
        assertNotNull(field);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, target, value);
    }

    private void replaceStatic(Class<?> type, String name, Object value) {
        Field field = ReflectionUtils.findField(type, name);
        assertNotNull(field);
        ReflectionUtils.makeAccessible(field);
        originalStaticFields.put(field, ReflectionUtils.getField(field, null));
        ReflectionUtils.setField(field, null, value);
    }

    private void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static class Transaction {
        boolean active;
        List<KvmHypervisorInfoVO> pending = new ArrayList<>();
    }

    private class TestTransactionManager extends AbstractPlatformTransactionManager {
        final ThreadLocal<Transaction> current = ThreadLocal.withInitial(Transaction::new);
        final AtomicInteger committed = new AtomicInteger();
        final AtomicInteger rolledBack = new AtomicInteger();
        Runnable beforeCommit = () -> {};

        @Override protected Object doGetTransaction() { return current.get(); }
        @Override protected boolean isExistingTransaction(Object transaction) { return ((Transaction) transaction).active; }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { ((Transaction) transaction).active = true; }
        @Override protected void doCommit(DefaultTransactionStatus status) {
            beforeCommit.run();
            current.get().pending.forEach(row -> committedRows.put(row.getUuid(), row));
            committed.incrementAndGet();
        }
        @Override protected void doRollback(DefaultTransactionStatus status) { rolledBack.incrementAndGet(); }
        @Override protected void doSetRollbackOnly(DefaultTransactionStatus status) {}
        @Override protected void doCleanupAfterCompletion(Object transaction) { current.remove(); }
    }
}
