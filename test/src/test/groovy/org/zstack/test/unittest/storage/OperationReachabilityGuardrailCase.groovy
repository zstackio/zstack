package org.zstack.test.unittest.storage

import org.junit.Test

class OperationReachabilityGuardrailCase {
    @Test
    void testOpenSourceP0CopyPathsSelectBeforeDispatch() {
        assertSourceContains("plugin/nfsPrimaryStorage/src/main/java/org/zstack/storage/primary/nfs/NfsPrimaryToSftpBackupKVMBackend.java", [
                "KvmOperationEndpointSelector.selectOne(",
                "\"nfs-sftp-download\"",
                "\"nfs-sftp-upload\"",
                "Endpoint.primaryStorage(\"nfs primary storage\"",
                "Endpoint.backupStorage(\"sftp backup storage\"",
                "msg.setHostUuid(host.getUuid())",
        ])
        assertSourceContains("plugin/sharedMountPointPrimaryStorage/src/main/java/org/zstack/storage/primary/smp/SftpBackupStorageKvmDownloader.java", [
                "KvmAgentCommandDispatcher.createForOperation(",
                "\"smp-sftp-download\"",
                "getCandidateHostUuids()",
                "Endpoint.backupStorage(\"sftp backup storage\"",
        ])
        assertSourceContains("plugin/sharedMountPointPrimaryStorage/src/main/java/org/zstack/storage/primary/smp/SftpBackupStorageKvmUploader.java", [
                "KvmAgentCommandDispatcher.createForOperation(",
                "\"smp-sftp-upload\"",
                "getCandidateHostUuids()",
                "Endpoint.backupStorage(\"sftp backup storage\"",
        ])
        assertSourceContains("plugin/localstorage/src/main/java/org/zstack/storage/primary/local/LocalStorageKvmSftpBackupStorageMediatorImpl.java", [
                "KvmOperationEndpointSelector.validateFixedHost(",
                "\"localstorage-sftp-download\"",
                "\"localstorage-sftp-upload\"",
                "Endpoint.backupStorage(\"sftp backup storage\"",
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
