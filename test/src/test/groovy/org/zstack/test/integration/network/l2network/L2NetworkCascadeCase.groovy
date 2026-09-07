package org.zstack.test.integration.network.l2network

import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cascade.CascadeAction
import org.zstack.core.cascade.CascadeConstant
import org.zstack.core.cascade.CascadeFacade
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor
import org.zstack.header.message.Message
import org.zstack.header.network.LocalNetworkConfigChange
import org.zstack.header.network.NetworkConfigChange
import org.zstack.header.network.NetworkConfigChangeCoordinator
import org.zstack.header.network.l2.L2DeleteConfirmExtensionPoint
import org.zstack.header.network.l2.L2NetworkInventory
import org.zstack.header.network.l2.L2NetworkUpdateExtensionPoint
import org.zstack.header.network.l2.L2NetworkVO
import org.zstack.header.network.l2.NetworkDeletionContext
import org.zstack.header.network.l3.IpRangeDeletionMsg
import org.zstack.header.network.l3.L3NetworkDeleteExtensionPoint
import org.zstack.header.network.l3.L3NetworkDeletionMsg
import org.zstack.header.network.l3.L3NetworkException
import org.zstack.header.network.l3.L3NetworkInventory
import org.zstack.header.network.l3.L3NetworkVO
import org.zstack.header.zone.ZoneVO
import org.zstack.network.l2.L2NetworkConfirmedDeleteCoordinator
import org.zstack.network.l2.L2NetworkExtensionPointEmitter
import org.zstack.sdk.L2NetworkInventory as SdkL2NetworkInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORK_L2_10024

class L2NetworkCascadeCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "check-zone"

                l2NoVlanNetwork {
                    name = "check-l2-1"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "check-l3"
                    }
                }

                l2NoVlanNetwork {
                    name = "check-l2-2"
                    physicalInterface = "eth1"
                }
            }

            zone {
                name = "cleanup-zone"

                l2NoVlanNetwork {
                    name = "cleanup-l2"
                    physicalInterface = "eth2"

                    l3Network {
                        name = "cleanup-l3"

                        ip {
                            startIp = "192.168.80.10"
                            endIp = "192.168.80.20"
                            netmask = "255.255.255.0"
                            gateway = "192.168.80.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "force-l2"
                    physicalInterface = "eth3"
                }

                l2NoVlanNetwork {
                    name = "force-check-l2"
                    physicalInterface = "eth6"

                    l3Network {
                        name = "force-check-l3"
                    }
                }

                l2NoVlanNetwork {
                    name = "metadata-l2"
                    physicalInterface = "eth4"
                }

                l2NoVlanNetwork {
                    name = "plain-l2"
                    physicalInterface = "eth5"
                }

                l2NoVlanNetwork {
                    name = "metadata-update-l2"
                    physicalInterface = "eth7"
                }

                l2NoVlanNetwork {
                    name = "delete-failure-l2"
                    physicalInterface = "eth8"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)
            testFenceExistsBeforeChildCheck()
            testDeleteZoneCancelsEveryConfirmedDeleteOnCheckFailure()
            testConfirmedDeleteFailureCancelsPreparedFences()
            testRepeatedPrepareReusesOperationContext()
            testRepeatedPrepareFailureCancelsExistingFence()
            testDelayedBeginDoesNotBlockCoordinator()
            testWholeL2ContextPropagatesToChildren()
            testParentForceChecksBeforeDeletingChildren()
            testParentForcePreservesProviderError()
            testPermissiveDeleteFailureCancelsProviderFence()
            testUnsupportedProviderKeepsExistingDeleteBehavior()
            testAsyncUpdateRuntimeFailureCompletesWithError()
            testL2MetadataMutationCommitsRemoteBeforeLocal()
            testDeleteL2NetworkRemovesConfirmedMetadataOnce()
        }
    }

    void testFenceExistsBeforeChildCheck() {
        ZoneInventory zone = env.inventoryByName("check-zone")
        List<String> events = []
        def confirmation = new RecordingDeleteConfirmation(events: events)
        def childCheck = new RecordingL3DeleteCheck(events: events, failCheck: true)
        List<L2DeleteConfirmExtensionPoint> confirmations = bean(PluginRegistry.class)
                .getExtensionList(L2DeleteConfirmExtensionPoint.class)
        List<L3NetworkDeleteExtensionPoint> childChecks = bean(PluginRegistry.class)
                .getExtensionList(L3NetworkDeleteExtensionPoint.class)
        confirmations.add(confirmation)
        childChecks.add(childCheck)

        try {
            expectError {
                deleteZone {
                    uuid = zone.uuid
                }
            }
        } finally {
            childChecks.remove(childCheck)
            confirmations.remove(confirmation)
        }

        assert events.any { it.startsWith("begin:") }
        assert events.findIndexOf { it.startsWith("child-check:") } >
                events.findLastIndexOf { it.startsWith("begin:") }
    }

    void testDeleteZoneCancelsEveryConfirmedDeleteOnCheckFailure() {
        ZoneInventory zone = env.inventoryByName("check-zone")
        def extension = new RecordingDeleteConfirmation(failOnSecondCheck: true)
        List<L2DeleteConfirmExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(L2DeleteConfirmExtensionPoint.class)
        extensions.add(extension)

        try {
            expectError {
                deleteZone {
                    uuid = zone.uuid
                }
            }
        } finally {
            extensions.remove(extension)
        }

        assert extension.begun.size() == 2
        assert extension.cancelled == extension.begun.toList().reverse()
        assert dbf.isExist(zone.uuid, org.zstack.header.zone.ZoneVO.class)
        assert extension.begun.every { dbf.isExist(it, L2NetworkVO.class) }
    }

    void testConfirmedDeleteFailureCancelsPreparedFences() {
        ZoneInventory zone = env.inventoryByName("check-zone")
        def inventory = org.zstack.header.zone.ZoneInventory.valueOf(
                dbf.findByUuid(zone.uuid, ZoneVO.class))
        def extension = new RecordingDeleteConfirmation(failOnSecondBegin: true)
        List<L2DeleteConfirmExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(L2DeleteConfirmExtensionPoint.class)
        extensions.add(extension)
        def action = new CascadeAction()
                .setRootIssuer(ZoneVO.simpleName)
                .setRootIssuerContext([inventory])
                .setParentIssuer(ZoneVO.simpleName)
                .setParentIssuerContext([inventory])
                .setActionCode(CascadeConstant.DELETION_CHECK_CODE)
        ErrorCode failure

        try {
            bean(CascadeFacade.class).asyncCascade(action, new Completion(null) {
                @Override
                void success() { assert false }

                @Override
                void fail(ErrorCode errorCode) { failure = errorCode }
            })
        } finally {
            extensions.remove(extension)
        }

        assert failure != null
        assert extension.begun.size() == 2
        assert extension.cancelled == [extension.begun.first()]
    }

    void testRepeatedPrepareReusesOperationContext() {
        SdkL2NetworkInventory l2 = env.inventoryByName("metadata-update-l2")
        L2NetworkInventory inventory = L2NetworkInventory.valueOf(
                dbf.findByUuid(l2.uuid, L2NetworkVO.class))
        def extension = new RecordingDeleteConfirmation()
        List<L2DeleteConfirmExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(L2DeleteConfirmExtensionPoint.class)
        extensions.add(extension)
        def action = new CascadeAction()
                .setRootIssuer(L2NetworkVO.simpleName)
                .setActionCode(CascadeConstant.DELETION_CHECK_CODE)
        int successCount = 0

        try {
            2.times {
                bean(L2NetworkConfirmedDeleteCoordinator.class).prepare(
                        action, [inventory], new Completion(null) {
                    @Override
                    void success() { successCount++ }

                    @Override
                    void fail(ErrorCode errorCode) { assert false: errorCode }
                })
            }
        } finally {
            extensions.remove(extension)
        }

        assert successCount == 2
        assert extension.begun == [l2.uuid]
        assert extension.checked == [l2.uuid, l2.uuid]
        assert extension.operationUuids.size() == 1
    }

    void testRepeatedPrepareFailureCancelsExistingFence() {
        SdkL2NetworkInventory l2 = env.inventoryByName("metadata-update-l2")
        L2NetworkInventory inventory = L2NetworkInventory.valueOf(
                dbf.findByUuid(l2.uuid, L2NetworkVO.class))
        def extension = new RecordingDeleteConfirmation(failOnSecondCheck: true)
        List<L2DeleteConfirmExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(L2DeleteConfirmExtensionPoint.class)
        extensions.add(extension)
        def action = new CascadeAction()
                .setRootIssuer(L2NetworkVO.simpleName)
                .setActionCode(CascadeConstant.DELETION_CHECK_CODE)
        ErrorCode failure

        try {
            bean(L2NetworkConfirmedDeleteCoordinator.class).prepare(
                    action, [inventory], new Completion(null) {
                @Override
                void success() { }

                @Override
                void fail(ErrorCode errorCode) { assert false: errorCode }
            })
            bean(L2NetworkConfirmedDeleteCoordinator.class).prepare(
                    action, [inventory], new Completion(null) {
                @Override
                void success() { assert false }

                @Override
                void fail(ErrorCode errorCode) { failure = errorCode }
            })
        } finally {
            extensions.remove(extension)
        }

        assert failure != null
        assert extension.begun == [l2.uuid]
        assert extension.checked == [l2.uuid, l2.uuid]
        assert extension.cancelled == [l2.uuid]
    }

    void testDelayedBeginDoesNotBlockCoordinator() {
        SdkL2NetworkInventory l2 = env.inventoryByName("metadata-update-l2")
        L2NetworkInventory inventory = L2NetworkInventory.valueOf(
                dbf.findByUuid(l2.uuid, L2NetworkVO.class))
        def extension = new RecordingDeleteConfirmation(deferBegin: true)
        List<L2DeleteConfirmExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(L2DeleteConfirmExtensionPoint.class)
        extensions.add(extension)
        def action = new CascadeAction()
                .setRootIssuer(L2NetworkVO.simpleName)
                .setActionCode(CascadeConstant.DELETION_CHECK_CODE)
        boolean completed

        try {
            bean(L2NetworkConfirmedDeleteCoordinator.class).prepare(
                    action, [inventory], new Completion(null) {
                @Override
                void success() { completed = true }

                @Override
                void fail(ErrorCode errorCode) { assert false: errorCode }
            })
            assert !completed
            assert extension.pendingBegin != null
            extension.pendingBegin.success()
            assert completed
        } finally {
            extensions.remove(extension)
        }
    }

    void testDeleteL2NetworkRemovesConfirmedMetadataOnce() {
        SdkL2NetworkInventory l2 = env.inventoryByName("metadata-l2")
        def extension = new RecordingDeleteConfirmation()
        List<L2DeleteConfirmExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(L2DeleteConfirmExtensionPoint.class)
        extensions.add(extension)

        try {
            deleteL2Network {
                uuid = l2.uuid
            }
        } finally {
            extensions.remove(extension)
        }

        assert !dbf.isExist(l2.uuid, L2NetworkVO.class)
        assert extension.checkCount == 1
        assert extension.localMetadataDeletes == [l2.uuid]
    }

    void testWholeL2ContextPropagatesToChildren() {
        SdkL2NetworkInventory l2 = env.inventoryByName("cleanup-l2")
        def extension = new RecordingDeleteConfirmation()
        List<L2DeleteConfirmExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(L2DeleteConfirmExtensionPoint.class)
        extensions.add(extension)
        List<NetworkDeletionContext> contexts = []
        bean(CloudBus.class).installBeforeDeliveryMessageInterceptor(
                new AbstractBeforeDeliveryMessageInterceptor() {
                    @Override
                    void beforeDeliveryMessage(Message msg) {
                        contexts.add(msg.networkDeletionContext)
                    }
                }, L3NetworkDeletionMsg.class, IpRangeDeletionMsg.class)

        try {
            deleteL2Network {
                uuid = l2.uuid
            }
        } finally {
            extensions.remove(extension)
        }

        assert contexts.size() == 2
        assert contexts.every { it?.origin == NetworkDeletionContext.Origin.WHOLE_L2_SEGMENT_DELETE }
        assert contexts.every { it.rootIssuer == L2NetworkVO.simpleName }
        assert contexts*.operationUuid.toSet() == extension.operationUuids.toSet()
    }

    void testParentForcePreservesProviderError() {
        SdkL2NetworkInventory l2 = env.inventoryByName("force-l2")
        def extension = new RecordingDeleteConfirmation(failDelete: true)
        List<L2DeleteConfirmExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(L2DeleteConfirmExtensionPoint.class)
        extensions.add(extension)

        try {
            expectError {
                deleteL2Network {
                    uuid = l2.uuid
                    deleteMode = "Enforcing"
                }
            }
        } finally {
            extensions.remove(extension)
        }

        assert dbf.isExist(l2.uuid, L2NetworkVO.class)
        assert extension.forceDeleteFlags == [true]
        assert extension.cancelled.isEmpty()
    }

    void testPermissiveDeleteFailureCancelsProviderFence() {
        SdkL2NetworkInventory l2 = env.inventoryByName("delete-failure-l2")
        def extension = new RecordingDeleteConfirmation(failDelete: true)
        List<L2DeleteConfirmExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(L2DeleteConfirmExtensionPoint.class)
        extensions.add(extension)

        try {
            expectError {
                deleteL2Network {
                    uuid = l2.uuid
                }
            }
        } finally {
            extensions.remove(extension)
        }

        assert dbf.isExist(l2.uuid, L2NetworkVO.class)
        assert extension.cancelled.isEmpty()
    }

    void testParentForceChecksBeforeDeletingChildren() {
        SdkL2NetworkInventory l2 = env.inventoryByName("force-check-l2")
        def l3 = env.inventoryByName("force-check-l3")
        def extension = new RecordingDeleteConfirmation(failCheck: true)
        List<L2DeleteConfirmExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(L2DeleteConfirmExtensionPoint.class)
        extensions.add(extension)

        try {
            expectError {
                deleteL2Network {
                    uuid = l2.uuid
                    deleteMode = "Enforcing"
                }
            }
        } finally {
            extensions.remove(extension)
        }

        assert extension.checkCount == 1
        assert dbf.isExist(l2.uuid, L2NetworkVO.class)
        assert dbf.isExist(l3.uuid, L3NetworkVO.class)
    }

    void testUnsupportedProviderKeepsExistingDeleteBehavior() {
        SdkL2NetworkInventory l2 = env.inventoryByName("plain-l2")
        def extension = new RecordingDeleteConfirmation(supported: false, failDelete: true)
        List<L2DeleteConfirmExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(L2DeleteConfirmExtensionPoint.class)
        extensions.add(extension)

        try {
            deleteL2Network {
                uuid = l2.uuid
            }
        } finally {
            extensions.remove(extension)
        }

        assert !dbf.isExist(l2.uuid, L2NetworkVO.class)
        assert extension.begun.isEmpty()
    }

    void testAsyncUpdateRuntimeFailureCompletesWithError() {
        SdkL2NetworkInventory l2 = env.inventoryByName("metadata-update-l2")
        L2NetworkInventory inventory = L2NetworkInventory.valueOf(
                dbf.findByUuid(l2.uuid, L2NetworkVO.class))
        def extension = new ThrowingL2UpdateExtension()
        List<L2NetworkUpdateExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(L2NetworkUpdateExtensionPoint.class)
        extensions.add(extension)
        ErrorCode failure

        try {
            bean(L2NetworkExtensionPointEmitter.class).beforeUpdate(
                    inventory, new Completion(null) {
                @Override
                void success() { assert false }

                @Override
                void fail(ErrorCode errorCode) { failure = errorCode }
            })
        } finally {
            extensions.remove(extension)
        }

        assert failure.globalErrorCode == ORG_ZSTACK_NETWORK_L2_10024
    }

    void testL2MetadataMutationCommitsRemoteBeforeLocal() {
        SdkL2NetworkInventory l2 = env.inventoryByName("metadata-update-l2")
        def extension = new RecordingMetadataChange(dbf: dbf, l2Uuid: l2.uuid, failRemote: true)
        bean(PluginRegistry.class).defineDynamicExtension(
                NetworkConfigChangeCoordinator.class, extension)

        try {
            expectError {
                updateL2Network {
                    uuid = l2.uuid
                    name = "must-not-commit"
                    description = "must-not-commit"
                }
            }
            assert dbf.findByUuid(l2.uuid, L2NetworkVO.class).name == "metadata-update-l2"

            extension.failRemote = false
            updateL2Network {
                uuid = l2.uuid
                name = "metadata-updated"
                description = "metadata-description"
            }
        } finally {
            extension.l2Uuid = null
        }

        L2NetworkVO updated = dbf.findByUuid(l2.uuid, L2NetworkVO.class)
        assert extension.nameBeforeContinuation == "metadata-update-l2"
        assert extension.changes*.kind == [
                NetworkConfigChange.Kind.L2_METADATA_CHANGE,
                NetworkConfigChange.Kind.L2_METADATA_CHANGE]
        assert updated.name == "metadata-updated"
        assert updated.description == "metadata-description"
    }

    @Override
    void clean() {
        env.delete()
    }

    private static class RecordingDeleteConfirmation implements L2DeleteConfirmExtensionPoint {
        boolean supported = true
        boolean failOnSecondCheck
        boolean failCheck
        boolean failDelete
        boolean failOnSecondBegin
        boolean deferBegin
        Completion pendingBegin
        int checkCount
        List<String> events = []
        List<String> begun = []
        List<String> cancelled = []
        List<String> localMetadataDeletes = []
        List<String> operationUuids = []
        List<Boolean> forceDeleteFlags = []
        List<String> checked = []
        List<NetworkDeletionContext> checkContexts = []

        @Override
        boolean supports(L2NetworkInventory inventory) {
            return supported
        }

        @Override
        void begin(L2NetworkInventory inventory, NetworkDeletionContext context,
                   Completion completion) {
            operationUuids.add(context.operationUuid)
            forceDeleteFlags.add(context.forceDelete)
            begun.add(inventory.uuid)
            events.add("begin:${inventory.uuid}")
            if (failOnSecondBegin && begun.size() == 2) {
                completion.fail(new ErrorCode("TEST", "simulated begin failure"))
                return
            }
            if (deferBegin) {
                pendingBegin = completion
            } else {
                completion.success()
            }
        }

        @Override
        void check(L2NetworkInventory inventory, NetworkDeletionContext context,
                   Completion completion) {
            checked.add(inventory.uuid)
            checkContexts.add(context)
            checkCount++
            if (failCheck || failOnSecondCheck && checkCount == 2) {
                completion.fail(new ErrorCode("TEST", "simulated confirmation failure"))
            } else {
                completion.success()
            }
        }

        @Override
        void delete(L2NetworkInventory inventory, NetworkDeletionContext context,
                    Completion completion) {
            if (failDelete) {
                completion.fail(new ErrorCode("TEST", "simulated provider delete failure"))
            } else {
                completion.success()
            }
        }

        @Override
        void cancel(L2NetworkInventory inventory, NetworkDeletionContext context,
                    Completion completion) {
            cancelled.add(inventory.uuid)
            completion.success()
        }

        @Override
        void deleteLocalMetadata(L2NetworkInventory inventory) {
            localMetadataDeletes.add(inventory.uuid)
        }
    }

    private static class ThrowingL2UpdateExtension implements L2NetworkUpdateExtensionPoint {
        @Override
        void beforeChangeL2NetworkVlanId(L2NetworkInventory inventory) {
            throw new IllegalStateException("simulated update failure")
        }

        @Override
        void afterChangeL2NetworkVlanId(L2NetworkInventory inventory) { }
    }

    private static class RecordingL3DeleteCheck implements L3NetworkDeleteExtensionPoint {
        List<String> events
        boolean failCheck

        @Override
        String preDeleteL3Network(L3NetworkInventory inventory) throws L3NetworkException {
            events.add("child-check:${inventory.uuid}")
            if (failCheck) {
                throw new L3NetworkException("simulated child check failure")
            }
            return null
        }

        @Override
        void beforeDeleteL3Network(L3NetworkInventory inventory) {
        }

        @Override
        void afterDeleteL3Network(L3NetworkInventory inventory) {
        }
    }

    private static class RecordingMetadataChange implements NetworkConfigChangeCoordinator {
        DatabaseFacade dbf
        String l2Uuid
        boolean failRemote
        String nameBeforeContinuation
        List<NetworkConfigChange> changes = []

        @Override
        boolean isApplicable(NetworkConfigChange change) {
            return change.kind == NetworkConfigChange.Kind.L2_METADATA_CHANGE && change.l2Uuid == l2Uuid
        }

        @Override
        void coordinate(NetworkConfigChange change, LocalNetworkConfigChange localChange,
                    Completion completion) {
            changes.add(change)
            nameBeforeContinuation = dbf.findByUuid(l2Uuid, L2NetworkVO.class).name
            if (failRemote) {
                completion.fail(new ErrorCode("TEST.REMOTE.FAILURE", "simulated remote failure"))
                return
            }
            localChange.apply(completion)
        }
    }
}
