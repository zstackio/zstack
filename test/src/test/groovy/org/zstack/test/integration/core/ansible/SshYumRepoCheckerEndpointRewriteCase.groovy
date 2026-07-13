package org.zstack.test.integration.core.ansible

import org.junit.Test
import org.zstack.core.ansible.SshYumRepoChecker

import java.nio.file.Files

class SshYumRepoCheckerEndpointRewriteCase {
    private static final int REST_PORT = 8080
    private static final String REPO_FILES = "/etc/yum.repos.d/{zstack,qemu-kvm-ev}-mn.repo"

    @Test
    void testRewritesIpv6RepositoryAuthorityToIpv4Endpoint() {
        assert rewriteRepositories("[2001:db8::1]", "192.168.1.10") == [
                "baseurl=http://192.168.1.10:8080/zstack/static/yum/",
                "baseurl=http://192.168.1.10:8080/zstack/static/yum/",
        ]
    }

    @Test
    void testRewritesIpv4RepositoryAuthorityToIpv6Endpoint() {
        assert rewriteRepositories("192.168.1.10", "2001:db8::1") == [
                "baseurl=http://[2001:db8::1]:8080/zstack/static/yum/",
                "baseurl=http://[2001:db8::1]:8080/zstack/static/yum/",
        ]
    }

    private static List<String> rewriteRepositories(String currentEndpoint, String managementNodeEndpoint) {
        File repoDir = Files.createTempDirectory("zstack-yum-repo").toFile()
        try {
            ["zstack", "qemu-kvm-ev"].each { repoName ->
                new File(repoDir, "${repoName}-mn.repo").text =
                        "baseurl=http://${currentEndpoint}:${REST_PORT}/zstack/static/yum/\n"
            }

            String command = SshYumRepoChecker.buildYumRepoEndpointRewriteCommand(managementNodeEndpoint, REST_PORT)
            Process process = ["bash", "-c", command.replace(REPO_FILES, "${repoDir.absolutePath}/{zstack,qemu-kvm-ev}-mn.repo")].execute()
            assert process.waitFor() == 0: process.errorStream.text

            return ["zstack", "qemu-kvm-ev"].collect { repoName ->
                new File(repoDir, "${repoName}-mn.repo").text.trim()
            }
        } finally {
            repoDir.deleteDir()
        }
    }
}
