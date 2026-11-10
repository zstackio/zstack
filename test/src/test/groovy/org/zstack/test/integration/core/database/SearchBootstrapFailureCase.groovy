package org.zstack.test.integration.core.database

import org.hibernate.search.exception.SearchException
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager
import org.hibernate.search.MassIndexer
import org.hibernate.search.jpa.FullTextEntityManager
import org.mockito.Mockito
import org.hibernate.search.store.impl.RAMDirectoryProvider
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.db.SearchAwareEntityManagerFactoryBean
import org.zstack.core.jsonlabel.JsonLabel
import org.zstack.core.jsonlabel.JsonLabelVO
import org.zstack.core.jsonlabel.JsonLabelVO_
import org.zstack.core.thread.SyncTask
import org.zstack.core.thread.ThreadFacade
import org.zstack.core.search.SearchGlobalProperty
import org.zstack.header.configuration.InstanceOfferingVO
import org.zstack.header.managementnode.ManagementNodeInventory
import org.zstack.header.managementnode.ManagementNodeVO
import org.zstack.header.managementnode.ManagementNodeVO_
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.ZQLQueryAction
import org.zstack.search.SearchFacadeImpl
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.gson.JSONObjectUtil

import java.sql.SQLException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class SearchBootstrapFailureCase extends SubCase {
    EnvSpec env
    InstanceOfferingInventory offering
    boolean originalSearchEnabled

    static class UnavailableDirectoryProvider extends RAMDirectoryProvider {
        static Throwable failure
        static int starts

        @Override
        void start(DirectoryBasedIndexManager manager) {
            starts++
            throw new SearchException("search cache initialization failed", failure)
        }
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {}
    }

    @Override
    void test() {
        env.create {
            originalSearchEnabled = SearchGlobalProperty.SearchAutoRegister
            offering = createInstanceOffering {
                name = "bootstrapbefore"
                cpuNum = 1
                memorySize = 1024 * 1024 * 1024
            } as InstanceOfferingInventory
            try {
                testSearchFailureDegradesAndRecoveryRebuilds()
                testExplicitlyDisabledSearchDoesNotStart()
                testDatabaseFailureIsNotRetried()
                testInterruptedAndFatalFailuresAreNotRetried()
            } finally {
                Thread.interrupted()
                SearchGlobalProperty.SearchAutoRegister = originalSearchEnabled
                UnavailableDirectoryProvider.failure = null
            }
        }
    }

    // Bootstrapping happens before APIs exist, so use a separate real factory with the running MN's datasource.
    private SearchAwareEntityManagerFactoryBean newFactory(boolean enabled, String provider) {
        def source = Test.componentLoader.springIoc.getBean("&entityManagerFactory") as LocalContainerEntityManagerFactoryBean
        def unit = source.persistenceUnitInfo
        def factory = new SearchAwareEntityManagerFactoryBean()
        factory.persistenceUnitManager = [
                obtainDefaultPersistenceUnitInfo: { unit },
                obtainPersistenceUnitInfo: { String name -> unit }
        ] as PersistenceUnitManager
        factory.persistenceUnitName = unit.persistenceUnitName
        factory.jpaVendorAdapter = new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter()
        factory.jpaDialect = source.jpaDialect
        factory.jpaPropertyMap = new HashMap<>(source.jpaPropertyMap)
        factory.jpaPropertyMap.putAll([
                "hibernate.search.autoregister_listeners": String.valueOf(enabled),
                "hibernate.search.default.directory_provider": provider,
                "hibernate.search.default.worker.backend": "lucene"
        ])
        return factory
    }

    void testSearchFailureDegradesAndRecoveryRebuilds() {
        def facade = Test.componentLoader.getComponent(SearchFacadeImpl.class)
        def inv = ManagementNodeInventory.valueOf(Q.New(ManagementNodeVO.class)
                .eq(ManagementNodeVO_.uuid, Platform.managementServerId).find())
        assert Platform.isVIPNode(): "Recovery must run on the actual test VIP"
        def builtField = SearchFacadeImpl.class.getDeclaredField("indexesBuiltOnVip")
        builtField.accessible = true
        retryInSecs { assert builtField.get(facade) }
        String disabledKey = "search.startupDisabled." + inv.uuid
        String rebuildPrefix = "search.rebuildRequired."
        assert !new JsonLabel().exists(disabledKey)
        List<Long> existingPending = Q.New(JsonLabelVO.class).like(JsonLabelVO_.labelKey, rebuildPrefix + "%")
                .select(JsonLabelVO_.id).listValues()
        def threadsField = SearchFacadeImpl.class.getDeclaredField("thdf")
        threadsField.accessible = true
        def originalThreads = threadsField.get(facade) as ThreadFacade
        def heldThreads = Mockito.mock(ThreadFacade.class)
        List<SyncTask> queued = new CopyOnWriteArrayList<>()
        Mockito.doAnswer { invocation ->
            queued.add(invocation.arguments[0] as SyncTask)
            return CompletableFuture.completedFuture(null)
        }.when(heldThreads).syncSubmit(Mockito.any(SyncTask.class))
        threadsField.set(facade, heldThreads)

        SearchGlobalProperty.SearchAutoRegister = true
        UnavailableDirectoryProvider.failure = new IllegalStateException("initial state transfer timed out")
        UnavailableDirectoryProvider.starts = 0
        def factory = newFactory(true, UnavailableDirectoryProvider.name)
        def manager
        try {
            RuntimeException bootstrapFailure = null
            try {
                factory.afterPropertiesSet()
            } catch (RuntimeException failure) {
                bootstrapFailure = failure
            }
            assert bootstrapFailure == null: "Search failure must not prevent ordinary database startup: " + bootstrapFailure
            assert !SearchGlobalProperty.SearchAutoRegister
            assert UnavailableDirectoryProvider.starts == 1
            manager = factory.object.createEntityManager()
            assert manager.find(InstanceOfferingVO.class, offering.uuid).name == offering.name
            String oldName = offering.name
            // Exercise real post-update listeners, which must not retain the failed search integrator.
            manager.transaction.begin()
            manager.find(InstanceOfferingVO.class, offering.uuid).name = "bootstrapafter"
            manager.transaction.commit()
            offering.name = "bootstrapafter"
            assert queryInstanceOffering { conditions = ["uuid=" + offering.uuid] }[0].name == offering.name

            facade.iJoin(inv)
            assertSearchUnavailable()
            SearchGlobalProperty.SearchAutoRegister = true
            assertSearchUnavailable() // A healthy peer must also reject the incomplete shared index.
            facade.iJoin(inv)
            assert !new JsonLabel().exists(disabledKey)
            assertSearchUnavailable() // Recovery is queued but has not rebuilt the shared index yet.

            // Fail the external Hibernate indexer while keeping the real facade, DB and ZQL paths.
            def failedIndexer = Mockito.mock(MassIndexer.class, Mockito.RETURNS_SELF)
            Mockito.doThrow(new IllegalStateException("cache writer unavailable")).when(failedIndexer).startAndWait()
            def failedManager = Mockito.mock(FullTextEntityManager.class)
            Mockito.when(failedManager.createIndexer()).thenReturn(failedIndexer)
            def failingFacade = Mockito.spy(new SearchFacadeImpl())
            threadsField.set(failingFacade, heldThreads)
            Mockito.doReturn(failedManager).when(failingFacade).getFullTextEntityManager()
            failingFacade.nodeJoin(inv)
            try {
                queued.last().call()
                Mockito.verify(failedIndexer).startAndWait()
            } finally {
                Thread.interrupted()
            }
            assertSearchUnavailable()

            // A new recovered failure after the rebuild snapshot must keep its own query fence.
            Mockito.doAnswer {
                CompletableFuture.runAsync({
                    try {
                        SearchGlobalProperty.SearchAutoRegister = false
                        facade.iJoin(inv)
                    } finally {
                        SearchGlobalProperty.SearchAutoRegister = true
                    }
                    facade.iJoin(inv)
                } as Runnable).get(30, TimeUnit.SECONDS)
                return facade.getFullTextEntityManager()
            }.when(failingFacade).getFullTextEntityManager()
            failingFacade.nodeJoin(inv)
            queued.last().call()
            assert builtField.get(failingFacade): "The rebuild must succeed before checking the newer failure fence"
            assertSearchUnavailable()

            facade.nodeJoin(inv)
            queued.last().call() // Run the real VIP rebuild against the shared production search backend.
            def updated = zQLQuery { delegate.zql = "search '" + offering.name + "' from instanceoffering" }
            assert JSONObjectUtil.toJsonString(updated).contains(offering.uuid)
            def old = zQLQuery { delegate.zql = "search '" + oldName + "' from instanceoffering" }
            assert !JSONObjectUtil.toJsonString(old).contains(offering.uuid)
            assert SearchGlobalProperty.SearchAutoRegister
        } finally {
            threadsField.set(facade, originalThreads)
            new JsonLabel().delete(disabledKey)
            List<Long> ownedPending = Q.New(JsonLabelVO.class).like(JsonLabelVO_.labelKey, rebuildPrefix + "%")
                    .eq(JsonLabelVO_.labelValue, inv.uuid).select(JsonLabelVO_.id).listValues()
            ownedPending.removeAll(existingPending)
            if (!ownedPending.isEmpty()) {
                SQL.New(JsonLabelVO.class).in(JsonLabelVO_.id, ownedPending).hardDelete()
            }
            manager?.close()
            factory.destroy()
        }
    }

    private void assertSearchUnavailable() {
        def result = new ZQLQueryAction(sessionId: env.session.uuid, zql: "search 'bootstrap' from instanceoffering").call()
        assert result.error != null: "Search must remain unavailable until the VIP rebuild succeeds"
        assert JSONObjectUtil.toJsonString(result.error).contains("SEARCH.1000")
        assert queryInstanceOffering { conditions = ["uuid=" + offering.uuid] }.size() == 1
    }

    void testExplicitlyDisabledSearchDoesNotStart() {
        SearchGlobalProperty.SearchAutoRegister = true
        UnavailableDirectoryProvider.starts = 0
        def factory = newFactory(false, UnavailableDirectoryProvider.name)
        try {
            factory.afterPropertiesSet()
            assert UnavailableDirectoryProvider.starts == 0
            assert SearchGlobalProperty.SearchAutoRegister
        } finally {
            factory.destroy()
        }
    }

    void testDatabaseFailureIsNotRetried() {
        UnavailableDirectoryProvider.failure = new SQLException("database unavailable")
        assertBootstrapFailsWithoutRetry()
    }

    void testInterruptedAndFatalFailuresAreNotRetried() {
        try {
            UnavailableDirectoryProvider.failure = new InterruptedException("bootstrap cancelled")
            assertBootstrapFailsWithoutRetry()
            assert Thread.currentThread().isInterrupted()
        } finally {
            Thread.interrupted()
        }
        UnavailableDirectoryProvider.failure = new OutOfMemoryError("fatal bootstrap failure")
        assertBootstrapFailsWithoutRetry()
    }

    private void assertBootstrapFailsWithoutRetry() {
        SearchGlobalProperty.SearchAutoRegister = true
        UnavailableDirectoryProvider.starts = 0
        def factory = newFactory(true, UnavailableDirectoryProvider.name)
        boolean failed = false
        try {
            factory.afterPropertiesSet()
        } catch (RuntimeException expected) {
            failed = true
        } finally {
            factory.destroy()
        }
        assert failed
        assert UnavailableDirectoryProvider.starts == 1
        assert SearchGlobalProperty.SearchAutoRegister
    }

    @Override
    void clean() {
        env.delete()
    }
}
