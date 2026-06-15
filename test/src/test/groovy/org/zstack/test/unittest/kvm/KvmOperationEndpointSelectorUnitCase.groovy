package org.zstack.test.unittest.kvm

import org.junit.Test
import org.zstack.header.host.HostInventory
import org.zstack.header.storage.backup.BackupStorageEndpointCandidate
import org.zstack.kvm.KvmOperationEndpointSelector
import org.zstack.kvm.KvmOperationEndpointSelector.Endpoint

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_KVM_10000

class KvmOperationEndpointSelectorUnitCase {
    @Test
    void testSelectTargetEndpointByAddressFamilyWithoutTestLib() {
        HostInventory ipv6Host = host("fd00:172:24::20")
        HostInventory ipv4Host = host("172.20.10.10")

        List<BackupStorageEndpointCandidate> endpointCandidates = [
                BackupStorageEndpointCandidate.copyTarget("172.24.1.11", null, BackupStorageEndpointCandidate.SOURCE_CONFIGURED_HOSTNAME, "ssh", 22, true),
                BackupStorageEndpointCandidate.copyTarget("fd00:172:24::11", null, BackupStorageEndpointCandidate.SOURCE_CONFIGURED_IPV6_ENDPOINT, "ssh", 22, false)
        ]

        KvmOperationEndpointSelector.Selection selection = KvmOperationEndpointSelector.selectForTargetEndpoint(
                "nfs-sftp-download",
                [ipv6Host, ipv4Host],
                [Endpoint.primaryStorage("nfs primary storage", "ps", "[fd00:172:24::10]:/data")],
                KvmOperationEndpointSelector.backupStorageEndpoints("sftp backup storage", "bs", endpointCandidates, "ignored.example.com"),
                ORG_ZSTACK_KVM_10000)

        assert selection.selectedHost.is(ipv6Host)
        assert selection.selectedBackupStorageAddress == "fd00:172:24::11"
        assert selection.selectedBackupStorageEndpoint.endpointSource == BackupStorageEndpointCandidate.SOURCE_CONFIGURED_IPV6_ENDPOINT
        assert selection.selectedHosts == [ipv6Host]
    }

    @Test
    void testHostnameEndpointDoesNotFilterWithoutStaticResolution() {
        HostInventory ipv6Host = host("fd00:172:24::20")
        HostInventory ipv4Host = host("172.20.10.10")

        List<HostInventory> selected = KvmOperationEndpointSelector.filter(
                "hostname-copy-target",
                [ipv6Host, ipv4Host],
                [Endpoint.backupStorage("sftp backup storage", "bs", "storage.example.com")],
                ORG_ZSTACK_KVM_10000)

        assert selected == [ipv6Host, ipv4Host]
    }

    @Test
    void testHostOrderTakesPrecedenceOverEndpointOrder() {
        HostInventory ipv6Host = host("fd00:172:24::20")
        HostInventory ipv4Host = host("172.20.10.10")

        List<BackupStorageEndpointCandidate> endpointCandidates = [
                BackupStorageEndpointCandidate.copyTarget("172.24.1.11", null, BackupStorageEndpointCandidate.SOURCE_CONFIGURED_HOSTNAME, "ssh", 22, true),
                BackupStorageEndpointCandidate.copyTarget("fd00:172:24::11", null, BackupStorageEndpointCandidate.SOURCE_CONFIGURED_IPV6_ENDPOINT, "ssh", 22, false)
        ]

        KvmOperationEndpointSelector.Selection selection = KvmOperationEndpointSelector.selectForTargetEndpoint(
                "shared-storage-copy",
                [ipv6Host, ipv4Host],
                [],
                KvmOperationEndpointSelector.backupStorageEndpoints("sftp backup storage", "bs", endpointCandidates, "ignored.example.com"),
                ORG_ZSTACK_KVM_10000)

        assert selection.selectedHost.is(ipv6Host)
        assert selection.selectedBackupStorageAddress == "fd00:172:24::11"
        assert selection.selectedBackupStorageEndpoint.endpointSource == BackupStorageEndpointCandidate.SOURCE_CONFIGURED_IPV6_ENDPOINT
        assert selection.selectedHosts == [ipv6Host]
    }

    @Test
    void testKnownLiteralEndpointPreferredOverUnknownHostnameFallback() {
        HostInventory ipv6Host = host("fd00:172:24::20")

        List<BackupStorageEndpointCandidate> endpointCandidates = [
                BackupStorageEndpointCandidate.copyTarget("storage.example.com", null, BackupStorageEndpointCandidate.SOURCE_CONFIGURED_HOSTNAME, "ssh", 22, true),
                BackupStorageEndpointCandidate.copyTarget("fd00:172:24::11", null, BackupStorageEndpointCandidate.SOURCE_CONFIGURED_IPV6_ENDPOINT, "ssh", 22, false)
        ]

        KvmOperationEndpointSelector.Selection selection = KvmOperationEndpointSelector.selectForTargetEndpoint(
                "hostname-with-ipv6-fallback",
                [ipv6Host],
                [],
                KvmOperationEndpointSelector.backupStorageEndpoints("sftp backup storage", "bs", endpointCandidates, "ignored.example.com"),
                ORG_ZSTACK_KVM_10000)

        assert selection.selectedHost.is(ipv6Host)
        assert selection.selectedBackupStorageAddress == "fd00:172:24::11"
        assert selection.selectedBackupStorageEndpoint.endpointSource == BackupStorageEndpointCandidate.SOURCE_CONFIGURED_IPV6_ENDPOINT
    }

    @Test
    void testEndpointCandidateInfersAddressFamily() {
        BackupStorageEndpointCandidate ipv6Candidate = BackupStorageEndpointCandidate.copyTarget(
                "fd00:172:24::11",
                null,
                BackupStorageEndpointCandidate.SOURCE_CONFIGURED_IPV6_ENDPOINT,
                "ssh",
                22,
                false)

        assert ipv6Candidate.addressFamily == "ipv6"
    }

    private static HostInventory host(String managementIp) {
        HostInventory inventory = new HostInventory()
        inventory.setManagementIp(managementIp)
        return inventory
    }
}
