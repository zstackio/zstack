package org.zstack.test.integration.network.l2network

import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.l2.L2NetworkInventory
import org.zstack.header.network.l2.L2NetworkUpdateExtensionPoint
import org.zstack.header.network.l2.L2NetworkVO
import org.zstack.network.l2.L2NetworkExtensionPointEmitter
import org.zstack.sdk.L2NetworkInventory as SdkL2NetworkInventory
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
                name = "cleanup-zone"

                l2NoVlanNetwork {
                    name = "plain-l2"
                    physicalInterface = "eth5"
                }

                l2NoVlanNetwork {
                    name = "metadata-update-l2"
                    physicalInterface = "eth7"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)
            testPlainDeleteL2Network()
            testAsyncUpdateRuntimeFailureCompletesWithError()
            testL2MetadataMutationCommitsRemoteBeforeLocal()
        }
    }

    void testPlainDeleteL2Network() {
        SdkL2NetworkInventory l2 = env.inventoryByName("plain-l2")
        deleteL2Network {
            uuid = l2.uuid
        }
        assert !dbf.isExist(l2.uuid, L2NetworkVO.class)
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
        List<L2NetworkUpdateExtensionPoint> extensions = bean(PluginRegistry.class)
                .getExtensionList(L2NetworkUpdateExtensionPoint.class)
        extensions.add(extension)

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
            extensions.remove(extension)
        }

        L2NetworkVO updated = dbf.findByUuid(l2.uuid, L2NetworkVO.class)
        assert extension.nameBeforeContinuation == "metadata-update-l2"
        assert extension.observedNames == ["must-not-commit", "metadata-updated"]
        assert updated.name == "metadata-updated"
        assert updated.description == "metadata-description"
    }

    @Override
    void clean() {
        env.delete()
    }

    private static class ThrowingL2UpdateExtension implements L2NetworkUpdateExtensionPoint {
        @Override
        void beforeChangeL2NetworkVlanId(L2NetworkInventory inventory) {
            throw new IllegalStateException("simulated update failure")
        }

        @Override
        void afterChangeL2NetworkVlanId(L2NetworkInventory inventory) { }
    }

    private static class RecordingMetadataChange implements L2NetworkUpdateExtensionPoint {
        DatabaseFacade dbf
        String l2Uuid
        boolean failRemote
        String nameBeforeContinuation
        List<String> observedNames = []

        @Override
        void beforeChangeL2NetworkVlanId(L2NetworkInventory l2Inv) {
        }

        @Override
        void afterChangeL2NetworkVlanId(L2NetworkInventory l2Inv) {
        }

        @Override
        void beforeUpdateL2Network(L2NetworkInventory l2Inv, Map<String, Object> headers,
                                   Completion completion) {
            if (l2Inv.uuid != l2Uuid) {
                completion.success()
                return
            }
            observedNames.add(l2Inv.name)
            nameBeforeContinuation = dbf.findByUuid(l2Uuid, L2NetworkVO.class).name
            if (failRemote) {
                completion.fail(new ErrorCode("TEST.REMOTE.FAILURE", "simulated remote failure"))
                return
            }
            completion.success()
        }
    }
}
