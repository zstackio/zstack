package org.zstack.test.unittest.storage

import org.junit.Test

class OperationReachabilityGuardrailCase {
    @Test
    void testOpenSourceP0CopyPathsSelectBeforeDispatch() {
        assertSourceContains("plugin/nfsPrimaryStorage/src/main/java/org/zstack/storage/primary/nfs/NfsPrimaryToSftpBackupKVMBackend.java", [
                "KvmOperationEndpointSelector.selectForTargetEndpoint(",
                "\"nfs-sftp-download\"",
                "\"nfs-sftp-upload\"",
                "Endpoint.primaryStorage(\"nfs primary storage\"",
                "KvmOperationEndpointSelector.backupStorageEndpoints(\"sftp backup storage\"",
                "getEndpointCandidates()",
                "selection.getSelectedBackupStorageAddress()",
                "msg.setHostUuid(host.getUuid())",
        ])
        assertSourceContains("plugin/sharedMountPointPrimaryStorage/src/main/java/org/zstack/storage/primary/smp/SftpBackupStorageKvmDownloader.java", [
                "KvmOperationEndpointSelector.selectForTargetEndpoint(",
                "\"smp-sftp-download\"",
                "KvmOperationEndpointSelector.backupStorageEndpoints(\"sftp backup storage\"",
                "greply.getEndpointCandidates()",
                "new KvmAgentCommandDispatcher(pinv.getUuid(), selection.getSelectedHostUuids())",
                "selection.getSelectedBackupStorageAddress()",
        ])
        assertSourceContains("plugin/sharedMountPointPrimaryStorage/src/main/java/org/zstack/storage/primary/smp/SftpBackupStorageKvmUploader.java", [
                "KvmOperationEndpointSelector.selectForTargetEndpoint(",
                "\"smp-sftp-upload\"",
                "KvmOperationEndpointSelector.backupStorageEndpoints(\"sftp backup storage\"",
                "r.getEndpointCandidates()",
                "new KvmAgentCommandDispatcher(pinv.getUuid(), selection.getSelectedHostUuids())",
                "selection.getSelectedBackupStorageAddress()",
        ])
        assertSourceContains("plugin/localstorage/src/main/java/org/zstack/storage/primary/local/LocalStorageKvmSftpBackupStorageMediatorImpl.java", [
                "KvmOperationEndpointSelector.selectTargetEndpointForFixedHost(",
                "\"localstorage-sftp-download\"",
                "\"localstorage-sftp-upload\"",
                "KvmOperationEndpointSelector.backupStorageEndpoints(\"sftp backup storage\"",
                "getEndpointCandidates()",
                "selectedEndpoint.getAddress()",
        ])
    }

    private void assertSourceContains(String relativePath, List<String> needles) {
        String source = findRepoFile(relativePath).text
        needles.each { needle ->
            assert source.contains(needle)
        }
    }

    private File findRepoFile(String relativePath) {
        File base = new File(".").canonicalFile
        while (base != null) {
            File file = new File(base, relativePath)
            if (file.exists()) {
                return file
            }
            base = base.parentFile
        }
        assert false: "unable to find ${relativePath}"
    }
}
