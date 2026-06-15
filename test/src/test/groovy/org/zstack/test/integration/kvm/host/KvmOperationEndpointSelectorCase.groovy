package org.zstack.test.integration.kvm.host

import org.zstack.compute.host.HostSystemTags
import org.zstack.core.db.Q
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.storage.backup.BackupStorageEndpointCandidate
import org.zstack.kvm.KvmOperationEndpointSelector
import org.zstack.kvm.KvmOperationEndpointSelector.Endpoint
import org.zstack.storage.primary.smp.KvmAgentCommandDispatcher
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_KVM_10000

class KvmOperationEndpointSelectorCase extends SubCase {
    EnvSpec env
    ClusterInventory cluster
    HostInventory ipv4Host
    HostInventory ipv6Host
    HostInventory dualStackHost

    private static final String OPERATION = "test-copy-before-kvm-dispatch"

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.noHostBasicEnv()
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            prepareHosts()
            testLiteralEndpointFamilyFiltersCandidates()
            testHostnameEndpointDoesNotStaticFilterCandidates()
            testTargetEndpointCandidatesSelectAddressReachableByActor()
            testFixedHostSelectsReachableTargetEndpoint()
            testCopyOperationDiagnosticContractIncludesActorsAndResources()
            testFixedHostMismatchRaisesStructuredDiagnostic()
            testDispatcherUsesProvidedCandidateHostUuids()
        }
    }

    void prepareHosts() {
        cluster = env.inventoryByName("cluster") as ClusterInventory

        ipv4Host = addKVMHost {
            name = "ipv4-host"
            managementIp = "172.20.10.10"
            username = "root"
            password = "password"
            clusterUuid = cluster.uuid
        } as HostInventory

        ipv6Host = addKVMHost {
            name = "ipv6-host"
            managementIp = "2001:db8::10"
            username = "root"
            password = "password"
            clusterUuid = cluster.uuid
        } as HostInventory

        dualStackHost = addKVMHost {
            name = "dual-stack-host"
            managementIp = "172.20.10.20"
            username = "root"
            password = "password"
            clusterUuid = cluster.uuid
            systemTags = [
                    HostSystemTags.EXTRA_IPS.instantiateTag(map(e(HostSystemTags.EXTRA_IPS_TOKEN, "fd00:172:24::20")))
            ]
        } as HostInventory
    }

    void testLiteralEndpointFamilyFiltersCandidates() {
        List<org.zstack.header.host.HostInventory> candidates = headerHosts(ipv4Host, ipv6Host, dualStackHost)

        List<org.zstack.header.host.HostInventory> ipv4Selected = KvmOperationEndpointSelector.filter(
                OPERATION,
                candidates,
                [Endpoint.primaryStorage("nfs primary storage", "ps", "172.24.1.10:/data")],
                ORG_ZSTACK_KVM_10000)
        assert selectedUuids(ipv4Selected) == [ipv4Host.uuid, dualStackHost.uuid]

        List<org.zstack.header.host.HostInventory> ipv6Selected = KvmOperationEndpointSelector.filter(
                OPERATION,
                candidates,
                [Endpoint.backupStorage("sftp backup storage", "bs", "[fd00:172:24::10]:/data")],
                ORG_ZSTACK_KVM_10000)
        assert selectedUuids(ipv6Selected) == [ipv6Host.uuid, dualStackHost.uuid]

        List<org.zstack.header.host.HostInventory> dualSelected = KvmOperationEndpointSelector.filter(
                OPERATION,
                candidates,
                [
                        Endpoint.primaryStorage("nfs primary storage", "ps", "172.24.1.10:/data"),
                        Endpoint.backupStorage("sftp backup storage", "bs", "[fd00:172:24::10]:/data")
                ],
                ORG_ZSTACK_KVM_10000)
        assert selectedUuids(dualSelected) == [dualStackHost.uuid]
    }

    void testHostnameEndpointDoesNotStaticFilterCandidates() {
        List<org.zstack.header.host.HostInventory> candidates = headerHosts(ipv4Host, ipv6Host, dualStackHost)

        List<org.zstack.header.host.HostInventory> selected = KvmOperationEndpointSelector.filter(
                OPERATION,
                candidates,
                [Endpoint.backupStorage("sftp backup storage", "bs", "storage.example.com:/data")],
                ORG_ZSTACK_KVM_10000)

        assert selectedUuids(selected) == [ipv4Host.uuid, ipv6Host.uuid, dualStackHost.uuid]
    }

    void testTargetEndpointCandidatesSelectAddressReachableByActor() {
        List<org.zstack.header.host.HostInventory> candidates = headerHosts(ipv6Host, ipv4Host, dualStackHost)
        List<BackupStorageEndpointCandidate> endpointCandidates = [
                BackupStorageEndpointCandidate.copyTarget("172.24.1.11", null, BackupStorageEndpointCandidate.SOURCE_CONFIGURED_HOSTNAME, "ssh", 22, true),
                BackupStorageEndpointCandidate.copyTarget("fd00:172:24::11", null, BackupStorageEndpointCandidate.SOURCE_CONFIGURED_IPV6_ENDPOINT, "ssh", 22, false)
        ]

        KvmOperationEndpointSelector.Selection selection = KvmOperationEndpointSelector.selectForTargetEndpoint(
                "nfs-sftp-download",
                candidates,
                [Endpoint.primaryStorage("nfs primary storage", "ps", "[fd00:172:24::10]:/data")],
                KvmOperationEndpointSelector.backupStorageEndpoints("sftp backup storage", "bs", endpointCandidates, "ignored.example.com"),
                ORG_ZSTACK_KVM_10000)

        assert selection.selectedHost.uuid == ipv6Host.uuid
        assert selection.selectedBackupStorageAddress == "fd00:172:24::11"
        assert selection.selectedBackupStorageEndpoint.endpointSource == BackupStorageEndpointCandidate.SOURCE_CONFIGURED_IPV6_ENDPOINT
        assert selectedUuids(selection.selectedHosts) == [ipv6Host.uuid, dualStackHost.uuid]
    }

    void testFixedHostSelectsReachableTargetEndpoint() {
        Endpoint selected = KvmOperationEndpointSelector.selectTargetEndpointForFixedHost(
                "localstorage-sftp-download",
                ipv4Host.uuid,
                [],
                [
                        Endpoint.backupStorage("sftp backup storage", "bs", "fd00:172:24::11", BackupStorageEndpointCandidate.SOURCE_CONFIGURED_IPV6_ENDPOINT),
                        Endpoint.backupStorage("sftp backup storage", "bs", "172.24.1.11", BackupStorageEndpointCandidate.SOURCE_CONFIGURED_HOSTNAME)
                ],
                ORG_ZSTACK_KVM_10000)

        assert selected.address == "172.24.1.11"
        assert selected.endpointSource == BackupStorageEndpointCandidate.SOURCE_CONFIGURED_HOSTNAME
    }

    void testCopyOperationDiagnosticContractIncludesActorsAndResources() {
        OperationFailureException failure = null
        try {
            KvmOperationEndpointSelector.filter(
                    "nfs-sftp-download",
                    headerHosts(ipv4Host),
                    [
                            Endpoint.primaryStorage("nfs primary storage", "ps-uuid", "172.24.1.10:/data"),
                            Endpoint.backupStorage("sftp backup storage", "bs-uuid", "[fd00:172:24::10]:/data")
                    ],
                    ORG_ZSTACK_KVM_10000)
        } catch (OperationFailureException e) {
            failure = e
        }

        assert failure != null
        Map details = JSONObjectUtil.toObject(failure.errorCode.details, LinkedHashMap.class)
        assert details.diagnosisType == "operationReachability"
        assert details.operation == "nfs-sftp-download"
        assert details.networkPlane == "storage-copy"
        assert details.probeTtl == 0
        assert details.failureClass == "ADDRESS_FAMILY_MISMATCH"
        assert details.candidateHostCount == 1
        assert details.inferredOnly
        assert details.primaryStorageUuid == "ps-uuid"
        assert details.primaryStorageType == "nfs primary storage"
        assert details.primaryStorageEndpoint == "172.24.1.10:/data"
        assert details.primaryStorageEndpointFamily == "ipv4"
        assert details.backupStorageUuid == "bs-uuid"
        assert details.backupStorageType == "sftp backup storage"
        assert details.backupStorageEndpoint == "[fd00:172:24::10]:/data"
        assert details.backupStorageEndpointFamily == "ipv6"
        assert details.backupStorageEndpointSource == "backupStorageCredential.hostname"
        assert details.candidateHosts[0].hostUuid == ipv4Host.uuid
        assert details.candidateHosts[0].managementIpFamily == "ipv4"
        assert details.candidateHosts[0].excludedReason.contains("ADDRESS_FAMILY_MISMATCH")
        assert details.candidateHosts[0].failedEdges[0].actorUuid == ipv4Host.uuid
        assert details.failedEdges[0].actorUuid == ipv4Host.uuid
        assert details.failedEdges[0].targetRole == "backupStorage"
        assert details.failedEdges[0].targetAddressFamily == "ipv6"
    }

    void testFixedHostMismatchRaisesStructuredDiagnostic() {
        OperationFailureException failure = null
        try {
            KvmOperationEndpointSelector.validateFixedHost(
                    "localstorage-sftp-download",
                    ipv4Host.uuid,
                    [Endpoint.backupStorage("sftp backup storage", "bs", "[fd00:172:24::10]:/data")],
                    ORG_ZSTACK_KVM_10000)
        } catch (OperationFailureException e) {
            failure = e
        }

        assert failure != null
        assert failure.errorCode.code == ORG_ZSTACK_KVM_10000
        Map details = JSONObjectUtil.toObject(failure.errorCode.details, LinkedHashMap.class)
        assert details.diagnosisType == "operationReachability"
        assert details.operation == "localstorage-sftp-download"
        assert details.networkPlane == "storage-copy"
        assert details.failureClass == "ADDRESS_FAMILY_MISMATCH"
        assert details.fixedHostUuid == ipv4Host.uuid
        assert details.selectedHostUuid == null
        assert details.primaryStorageUuid == null
        assert details.backupStorageUuid == "bs"
        assert details.backupStorageEndpointSource == "backupStorageCredential.hostname"
        assert details.endpoints[0].resourceType == "sftp backup storage"
        assert details.endpoints[0].addressFamily == "ipv6"
        assert details.candidateHosts[0].hostUuid == ipv4Host.uuid
        assert details.candidateHosts[0].managementIpFamily == "ipv4"
        assert details.candidateHosts[0].addressFamilies.contains("ipv4")
        assert details.candidateHosts[0].excludedReason.contains("ipv6")
        assert details.failedEdges[0].targetAddressFamily == "ipv6"
        assert details.recommendations[0].contains("managementIp/extraIps")
    }

    void testDispatcherUsesProvidedCandidateHostUuids() {
        KvmAgentCommandDispatcher dispatcher = KvmAgentCommandDispatcher.createForOperation(
                "test-smp-primary-storage",
                "smp-sftp-download",
                [ipv4Host.uuid, dualStackHost.uuid],
                [Endpoint.backupStorage("sftp backup storage", "bs", "[fd00:172:24::10]:/data")],
                ORG_ZSTACK_KVM_10000)

        def field = KvmAgentCommandDispatcher.class.getDeclaredField("hostUuids")
        field.setAccessible(true)
        assert field.get(dispatcher) == [dualStackHost.uuid]
    }

    private List<org.zstack.header.host.HostInventory> headerHosts(HostInventory... hosts) {
        return hosts.collect { HostInventory host ->
            HostVO vo = Q.New(HostVO.class)
                    .eq(HostVO_.uuid, host.uuid)
                    .find()
            assert vo != null
            return org.zstack.header.host.HostInventory.valueOf(vo)
        }
    }

    private List<String> selectedUuids(List<org.zstack.header.host.HostInventory> hosts) {
        return hosts.collect { it.uuid }
    }

}
