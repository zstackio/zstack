package org.zstack.test.integration.network.l3network

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.core.db.DatabaseFacade
import org.springframework.transaction.support.TransactionSynchronizationAdapter
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.zstack.header.tag.UserTagVO
import org.zstack.header.tag.UserTagVO_
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.message.MessageReply
import org.zstack.header.network.NetworkConfigChange
import org.zstack.header.network.LocalNetworkConfigChange
import org.zstack.header.network.NetworkConfigChangeCoordinator
import org.zstack.core.thread.ThreadFacade
import org.zstack.core.thread.ChainTask
import org.zstack.core.thread.SyncTaskChain
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.identity.SharedResourceVO
import org.zstack.header.identity.SharedResourceVO_
import org.zstack.header.network.l2.ExternalNetworkRef
import org.zstack.header.network.l2.NetworkCreateContext
import org.zstack.header.network.l2.NetworkOperationOrigin
import org.zstack.header.network.l3.*
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants

class ProjectedIpRangeSetCase extends SubCase {
    EnvSpec env
    CloudBus bus
    String l3Uuid
    long version = 1

    @Override
    void setup() { useSpring(NetworkTest.springSpec) }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"
                    l3Network {
                        name = "l3"
                        ip {
                            name = "original-name"
                            startIp = "192.168.10.10"
                            endIp = "192.168.10.100"
                            gateway = "192.168.10.1"
                            netmask = "255.255.255.0"
                        }
                    }
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            bus = bean(CloudBus)
            l3Uuid = env.inventoryByName("l3").uuid
            verifyEmptyProjectionPreservesDeclaredFamily()
            def original = localRanges()[0]
            shareResource { resourceUuids = [l3Uuid]; toPublic = true }
            def first = allocate("192.168.10.20")
            def second = allocate("192.168.10.80")

            3.times {
                def result = replace([range("192.168.10.10", "192.168.10.100")])
                assert result.success: result.error
                assert result.ranges*.uuid == [original.uuid]
                assert localRanges()[0].name == "original-name"
            }
            def tag = createUserTag {
                resourceUuid = original.uuid
                resourceType = IpRangeVO.simpleName
                tag = "projection-transaction-tag"
            }
            verifyTransactionRollback(original.uuid, tag.uuid, first, second)
            def split = replace([range("192.168.10.10", "192.168.10.50"),
                                 range("192.168.10.51", "192.168.10.150")])
            assert split.success: split.error
            assert !Q.New(IpRangeVO).eq(IpRangeVO_.uuid, original.uuid).isExists()
            def lower = localRanges().find { it.endIp == "192.168.10.50" }
            def upper = localRanges().find { it.startIp == "192.168.10.51" }
            assertUsedIp(first.uuid, first.ip, lower.uuid)
            assertUsedIp(second.uuid, second.ip, upper.uuid)
            assertOwner(lower.uuid)
            assertOwner(upper.uuid)
            assert Q.New(SharedResourceVO).eq(SharedResourceVO_.resourceUuid, lower.uuid).isExists()
            assert Q.New(SharedResourceVO).eq(SharedResourceVO_.resourceUuid, upper.uuid).isExists()
            assert !Q.New(AccountResourceRefVO).eq(AccountResourceRefVO_.resourceUuid, original.uuid).isExists()
            assert !Q.New(UserTagVO).eq(UserTagVO_.uuid, tag.uuid).isExists()

            def before = localRanges().collect { [it.uuid, it.startIp, it.endIp] }
            def rejected = replace([range("192.168.10.10", "192.168.10.40")])
            assert !rejected.success
            assert localRanges().collect { [it.uuid, it.startIp, it.endIp] } == before
            assertUsedIp(first.uuid, first.ip, lower.uuid)
            assertUsedIp(second.uuid, second.ip, upper.uuid)

            def merged = replace([range("192.168.10.10", "192.168.10.150")])
            assert merged.success: merged.error
            assert localRanges().size() == 1
            String mergedUuid = localRanges()[0].uuid
            assertUsedIp(first.uuid, first.ip, mergedUuid)
            assertUsedIp(second.uuid, second.ip, mergedUuid)
            assertOwner(mergedUuid)

            assert !replace([]).success
            [first, second].each { used ->
                ReturnIpMsg msg = new ReturnIpMsg(l3NetworkUuid: l3Uuid, usedIpUuid: used.uuid)
                bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3Uuid)
                assert bus.call(msg).success
            }
            assert replace([]).success
            assert localRanges().empty
            assert Q.New(L3NetworkVO).eq(L3NetworkVO_.uuid, l3Uuid).isExists()
            assert !Q.New(UsedIpVO).eq(UsedIpVO_.l3NetworkUuid, l3Uuid).isExists()
            verifyObsoleteDhcpReservationRollback()
            verifyDeleteReleasesQueueBeforeCoordinatorProjection()
            verifyDeleteWaitingForL2DoesNotBlockEarlierProjection()
        }
    }

    private void verifyEmptyProjectionPreservesDeclaredFamily() {
        DatabaseFacade db = bean(DatabaseFacade)
        String originalL3Uuid = l3Uuid
        String l2Uuid = db.findByUuid(originalL3Uuid, L3NetworkVO).l2NetworkUuid
        try {
            [IPv6Constants.IPv4, IPv6Constants.IPv6, IPv6Constants.DUAL_STACK].each { family ->
                def empty = createL3Network {
                    name = "empty-projection-${family}".toString()
                    l2NetworkUuid = l2Uuid
                    ipVersion = family == IPv6Constants.DUAL_STACK ? IPv6Constants.IPv4 : family
                }
                l3Uuid = empty.uuid
                if (family == IPv6Constants.DUAL_STACK) {
                    def declared = db.findByUuid(l3Uuid, L3NetworkVO)
                    declared.ipVersion = family
                    db.update(declared)
                }
                2.times {
                    def result = replace([])
                    assert result.success: result.error
                    assert localRanges().empty
                    assert db.findByUuid(l3Uuid, L3NetworkVO).ipVersion == family:
                            'empty projection must retain the family declared when creating L3'
                }
            }
        } finally {
            l3Uuid = originalL3Uuid
        }
    }

    private void verifyObsoleteDhcpReservationRollback() {
        DatabaseFacade db = bean(DatabaseFacade)
        def original = addIpRange {
            name = 'obsolete-dhcp'; l3NetworkUuid = this.l3Uuid
            startIp = '192.168.10.10'; endIp = '192.168.10.100'
            gateway = '192.168.10.1'; netmask = '255.255.255.0'
        }
        def dhcp = allocate('192.168.10.20')
        def ordinary = allocate('192.168.10.30')
        def tag = createUserTag {
            resourceUuid = original.uuid; resourceType = IpRangeVO.simpleName; tag = 'obsolete-dhcp-rollback'
        }
        def blocked = replacement([])
        blocked.obsoleteDhcpServerIpUuid = dhcp.uuid
        assert !bus.call(blocked).success
        assertUsedIp(dhcp.uuid, dhcp.ip, original.uuid)
        assertUsedIp(ordinary.uuid, ordinary.ip, original.uuid)
        def release = new ReturnIpMsg(l3NetworkUuid: l3Uuid, usedIpUuid: ordinary.uuid)
        bus.makeTargetServiceIdByResourceUuid(release, L3NetworkConstant.SERVICE_ID, l3Uuid)
        assert bus.call(release).success

        AtomicBoolean injected = new AtomicBoolean()
        PluginRegistry registry = bean(PluginRegistry)
        ValidateProjectedIpRangeSetExtensionPoint guard = { ReplaceProjectedIpRangesMsg message, L3NetworkVO l3 ->
            assert message.obsoleteDhcpServerIpUuid == dhcp.uuid
            assert db.entityManager.find(UsedIpVO, dhcp.uuid) != null
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                void beforeCommit(boolean readOnly) {
                    assert db.entityManager.createQuery('select count(ip) from UsedIpVO ip where ip.uuid = :uuid')
                            .setParameter('uuid', dhcp.uuid).singleResult == 0L
                    assert db.entityManager.createQuery('select count(r) from NormalIpRangeVO r where r.uuid = :uuid')
                            .setParameter('uuid', original.uuid).singleResult == 0L
                    assert db.entityManager.createQuery('select count(r) from AccountResourceRefVO r where r.resourceUuid = :uuid')
                            .setParameter('uuid', original.uuid).singleResult == 0L
                    injected.set(true)
                    throw new IllegalStateException('injected failure after obsolete DHCP reservation removal')
                }
            })
        } as ValidateProjectedIpRangeSetExtensionPoint
        registry.defineDynamicExtension(ValidateProjectedIpRangeSetExtensionPoint, guard)
        try {
            def rollback = replacement([])
            rollback.obsoleteDhcpServerIpUuid = dhcp.uuid
            assert !bus.call(rollback).success
            assert injected.get()
        } finally {
            registry.getExtensionList(ValidateProjectedIpRangeSetExtensionPoint).remove(guard)
        }
        assertUsedIp(dhcp.uuid, dhcp.ip, original.uuid)
        assert localRanges()*.uuid == [original.uuid]
        assertOwner(original.uuid)
        assert Q.New(UserTagVO).eq(UserTagVO_.uuid, tag.uuid).isExists()
        assert Q.New(IpRangeEO).eq(IpRangeVO_.uuid, original.uuid).find().deleted == null
        def accepted = replacement([])
        accepted.obsoleteDhcpServerIpUuid = dhcp.uuid
        def reply = bus.call(accepted)
        assert reply.success: reply.error
        assert !db.isExist(dhcp.uuid, UsedIpVO)
        assert localRanges().empty
        assert !Q.New(AccountResourceRefVO).eq(AccountResourceRefVO_.resourceUuid, original.uuid).isExists()
        assert !Q.New(UserTagVO).eq(UserTagVO_.uuid, tag.uuid).isExists()
        assert db.findByUuid(l3Uuid, L3NetworkVO).ipVersion == 0
    }

    private void verifyTransactionRollback(String originalUuid, String tagUuid, def first, def second) {
        DatabaseFacade db = bean(DatabaseFacade)
        AtomicBoolean injected = new AtomicBoolean()
        AtomicReference<List<String>> created = new AtomicReference<>([])
        PluginRegistry registry = bean(PluginRegistry)
        ValidateProjectedIpRangeSetExtensionPoint guard = { ReplaceProjectedIpRangesMsg message, L3NetworkVO l3 ->
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                void beforeCommit(boolean readOnly) {
                    assert db.entityManager.createQuery("select count(r) from NormalIpRangeVO r where r.uuid = :uuid")
                            .setParameter("uuid", originalUuid).singleResult == 0L
                    assert db.entityManager.createQuery("select count(t) from UserTagVO t where t.uuid = :uuid")
                            .setParameter("uuid", tagUuid).singleResult == 0L
                    assert db.entityManager.createQuery("select count(r) from AccountResourceRefVO r where r.resourceUuid = :uuid")
                            .setParameter("uuid", originalUuid).singleResult == 0L
                    List<String> newUuids = db.entityManager.createQuery("select r.uuid from NormalIpRangeVO r where r.l3NetworkUuid = :l3")
                            .setParameter("l3", l3Uuid).resultList
                    assert newUuids.size() == 2
                    created.set(newUuids)
                    injected.set(true)
                    throw new IllegalStateException("injected failure after range and metadata changes")
                }
            })
        } as ValidateProjectedIpRangeSetExtensionPoint
        registry.defineDynamicExtension(ValidateProjectedIpRangeSetExtensionPoint, guard)
        try {
            assert !replace([range("192.168.10.10", "192.168.10.50"),
                             range("192.168.10.51", "192.168.10.150")]).success
            assert injected.get()
        } finally {
            registry.getExtensionList(ValidateProjectedIpRangeSetExtensionPoint).remove(guard)
        }
        assert localRanges()*.uuid == [originalUuid]
        created.get().each { uuid ->
            assert !Q.New(AccountResourceRefVO).eq(AccountResourceRefVO_.resourceUuid, uuid).isExists()
            assert !Q.New(SharedResourceVO).eq(SharedResourceVO_.resourceUuid, uuid).isExists()
        }
        assertUsedIp(first.uuid, first.ip, originalUuid)
        assertUsedIp(second.uuid, second.ip, originalUuid)
        assertOwner(originalUuid)
        assert Q.New(UserTagVO).eq(UserTagVO_.uuid, tagUuid).isExists()
        assert Q.New(IpRangeEO).eq(IpRangeVO_.uuid, originalUuid).find().deleted == null
        assert Q.New(IpRangeEO).eq(IpRangeVO_.l3NetworkUuid, l3Uuid).count() == 1
    }

    private void verifyDeleteWaitingForL2DoesNotBlockEarlierProjection() {
        def added = addIpRange {
            name = "delete-behind-projection"
            l3NetworkUuid = l3Uuid
            startIp = "192.168.10.10"
            endIp = "192.168.10.100"
            gateway = "192.168.10.1"
            netmask = "255.255.255.0"
        }
        ThreadFacade threads = bean(ThreadFacade)
        PluginRegistry registry = bean(PluginRegistry)
        String queue = "ip-range-sync-test-l2-" + l3Uuid
        AtomicReference<SyncTaskChain> oldProjectionChain = new AtomicReference<>()
        AtomicBoolean released = new AtomicBoolean()
        AtomicBoolean projected = new AtomicBoolean()
        CountDownLatch oldProjectionStarted = new CountDownLatch(1)
        Runnable release = {
            if (released.compareAndSet(false, true)) { oldProjectionChain.get().next() }
        } as Runnable
        threads.chainSubmit(new ChainTask(null) {
            @Override
            String getSyncSignature() { queue }
            @Override
            String getName() { "hold-test-l2-for-earlier-projection" }
            @Override
            void run(SyncTaskChain chain) {
                oldProjectionChain.set(chain)
                oldProjectionStarted.countDown()
            }
        })
        assert oldProjectionStarted.await(3, TimeUnit.SECONDS)
        NetworkConfigChangeCoordinator coordinator = new NetworkConfigChangeCoordinator() {
            @Override
            boolean isApplicable(NetworkConfigChange change) {
                return change.ipRangeConfiguration?.l3Uuid == l3Uuid &&
                        change.ipRangeConfiguration.operation == NetworkConfigChange.CollectionChangeOperation.REMOVE
            }

            @Override
            void coordinate(NetworkConfigChange change, LocalNetworkConfigChange local, Completion completion) {
                threads.chainSubmit(new ChainTask(completion) {
                    @Override
                    String getSyncSignature() { queue }
                    @Override
                    String getName() { "delete-waiting-for-test-l2" }
                    @Override
                    void run(SyncTaskChain chain) {
                        local.apply(new Completion(completion) {
                            @Override
                            void success() {
                                chain.next()
                                completion.success()
                            }
                            @Override
                            void fail(ErrorCode error) {
                                chain.next()
                                completion.fail(error)
                            }
                        })
                    }
                })
                ReplaceProjectedIpRangesMsg earlier = replacement([range("192.168.10.10", "192.168.10.100")])
                earlier.timeout = 3000
                bus.send(earlier, new CloudBusCallBack(completion) {
                    @Override
                    void run(MessageReply reply) {
                        projected.set(reply.success)
                        release.run()
                    }
                })
            }
        }
        registry.defineDynamicExtension(NetworkConfigChangeCoordinator, coordinator)
        try {
            deleteIpRange { uuid = added.uuid }
            assert projected.get(): "delete waiting for L2 blocked the earlier projection from operate-l3"
        } finally {
            registry.getExtensionList(NetworkConfigChangeCoordinator).remove(coordinator)
            release.run()
        }
    }

    private void verifyDeleteReleasesQueueBeforeCoordinatorProjection() {
        def range = addIpRange {
            name = "delete-with-projection"
            l3NetworkUuid = l3Uuid
            startIp = "192.168.10.10"
            endIp = "192.168.10.100"
            gateway = "192.168.10.1"
            netmask = "255.255.255.0"
        }
        PluginRegistry registry = bean(PluginRegistry)
        boolean projected = false
        NetworkConfigChangeCoordinator coordinator = new NetworkConfigChangeCoordinator() {
            @Override
            boolean isApplicable(NetworkConfigChange change) {
                return change.ipRangeConfiguration?.l3Uuid == l3Uuid &&
                        change.ipRangeConfiguration.operation == NetworkConfigChange.CollectionChangeOperation.REMOVE
            }

            @Override
            void coordinate(NetworkConfigChange change, LocalNetworkConfigChange localChange, Completion completion) {
                assert change.ipRangeConfiguration.removedRange.startIp == "192.168.10.10"
                assert change.ipRangeConfiguration.removedRange.endIp == "192.168.10.100"
                localChange.apply(new Completion(completion) {
                    @Override
                    void success() {
                        ReplaceProjectedIpRangesMsg projection = replacement([])
                        projection.timeout = 3000
                        bus.send(projection, new CloudBusCallBack(completion) {
                            @Override
                            void run(MessageReply reply) {
                                if (reply.success) {
                                    projected = true
                                    completion.success()
                                } else {
                                    completion.fail(reply.error)
                                }
                            }
                        })
                    }

                    @Override
                    void fail(ErrorCode error) { completion.fail(error) }
                })
            }
        }
        registry.defineDynamicExtension(NetworkConfigChangeCoordinator, coordinator)
        try {
            deleteIpRange { uuid = range.uuid }
            assert projected: "coordinator projection could not reacquire operate-l3 queue"
        } finally {
            registry.getExtensionList(NetworkConfigChangeCoordinator).remove(coordinator)
        }
    }

    private MessageReply replace(List<ReplaceProjectedIpRangesMsg.Range> target) {
        return bus.call(replacement(target))
    }

    private ReplaceProjectedIpRangesMsg replacement(List<ReplaceProjectedIpRangesMsg.Range> target) {
        String owner = Q.New(AccountResourceRefVO).select(AccountResourceRefVO_.accountUuid)
                .eq(AccountResourceRefVO_.resourceUuid, l3Uuid).findValue()
        ReplaceProjectedIpRangesMsg msg = new ReplaceProjectedIpRangesMsg(
                l3NetworkUuid: l3Uuid, ranges: target, expectedSourceType: NormalIpRangeVO.simpleName,
                context: NetworkCreateContext.projection(NetworkOperationOrigin.ZNS_REFRESH,
                        new ExternalNetworkRef("logical-network", owner), "projection-operation", version++,
                        NetworkCreateContext.APPLY_LOCAL_STEP))
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3Uuid)
        return msg
    }

    private def allocate(String ip) {
        AllocateIpMsg msg = new AllocateIpMsg(l3NetworkUuid: l3Uuid, requiredIp: ip)
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3Uuid)
        AllocateIpReply reply = bus.call(msg) as AllocateIpReply
        assert reply.success: reply.error
        return reply.ipInventory
    }

    private static ReplaceProjectedIpRangesMsg.Range range(String start, String end) {
        return new ReplaceProjectedIpRangesMsg.Range(startIp: start, endIp: end,
                gateway: "192.168.10.1", netmask: "255.255.255.0", ipVersion: 4)
    }

    private List<NormalIpRangeVO> localRanges() {
        return Q.New(NormalIpRangeVO).eq(IpRangeVO_.l3NetworkUuid, l3Uuid)
                .orderByAsc(IpRangeVO_.startIp).list()
    }

    private void assertUsedIp(String uuid, String ip, String rangeUuid) {
        UsedIpVO used = Q.New(UsedIpVO).eq(UsedIpVO_.uuid, uuid).find()
        assert used != null
        assert used.ip == ip
        assert used.ipRangeUuid == rangeUuid
        assert used.l3NetworkUuid == l3Uuid
    }

    private void assertOwner(String rangeUuid) {
        assert Q.New(AccountResourceRefVO).eq(AccountResourceRefVO_.resourceUuid, rangeUuid).isExists()
    }

    @Override
    void clean() { env.delete() }
}
