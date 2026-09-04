package org.zstack.test.integration.core.ansible

import org.junit.Test
import org.zstack.core.ansible.SshYumRepoChecker
import org.zstack.utils.network.IPv6NetworkUtils

import java.nio.file.Files
import java.nio.file.Path

class SshYumRepoCheckerTest {
    private static final int REST_PORT = 8080
    private static final String ZSTACK_REPO_PATH =
            "/zstack/static/zstack-repo/\$basearch/\$YUM0/"
    private static final String QEMU_REPO_PATH =
            "/zstack/static/zstack-repo/\$basearch/\$YUM0/Extra/qemu-kvm-ev/"

    @Test
    void testRepairsStaleIpv4AndIpv6Endpoints() {
        [
                ["[2001:db8::1]", "192.168.1.10"],
                ["192.168.1.10", "2001:db8::1"],
        ].eachWithIndex { endpoints, index ->
            File repoDir = Files.createTempDirectory("zstack-yum-repo-stale-${index}").toFile()
            try {
                File zstackRepo = new File(repoDir, "zstack-mn.repo")
                File qemuRepo = new File(repoDir, "qemu-kvm-ev-mn.repo")
                writeRepository(zstackRepo, "zstack-mn", endpoints[0])
                writeRepository(qemuRepo, "qemu-kvm-ev-mn", endpoints[0])

                Process process = runCommand(repoDir.toPath(), endpoints[1])
                assert process.exitValue() == 0: process.errorStream.text
                assert zstackRepo.readLines().find { it.startsWith("baseurl=") } ==
                        "baseurl=http://${formatEndpoint(endpoints[1])}${ZSTACK_REPO_PATH}"
                assert qemuRepo.readLines().find { it.startsWith("baseurl=") } ==
                        "baseurl=http://${formatEndpoint(endpoints[1])}${QEMU_REPO_PATH}"
            } finally {
                repoDir.deleteDir()
            }
        }
    }

    @Test
    void testRejectsMissingAndEmptyRepositories() {
        ["zstack-mn.repo", "qemu-kvm-ev-mn.repo"].each { brokenRepoName ->
            [missing: null, empty: ""].each { state, content ->
                File repoDir = Files.createTempDirectory("zstack-yum-repo-${state}").toFile()
                try {
                    File zstackRepo = new File(repoDir, "zstack-mn.repo")
                    File qemuRepo = new File(repoDir, "qemu-kvm-ev-mn.repo")
                    writeRepository(zstackRepo, "zstack-mn", "192.168.1.10")
                    writeRepository(qemuRepo, "qemu-kvm-ev-mn", "192.168.1.10")

                    File brokenRepo = new File(repoDir, brokenRepoName)
                    if (content == null) {
                        assert brokenRepo.delete()
                    } else {
                        brokenRepo.text = content
                    }

                    assert runCommand(repoDir.toPath(), "192.168.1.11").exitValue() != 0
                } finally {
                    repoDir.deleteDir()
                }
            }
        }
    }

    @Test
    void testAcceptsMatchingIpv4AndIpv6Endpoints() {
        [
                ["192.168.1.10", "192.168.1.10"],
                ["[2001:db8::1]", "2001:db8::1"],
        ].eachWithIndex { endpoints, index ->
            File repoDir = Files.createTempDirectory("zstack-yum-repo-matching-${index}").toFile()
            try {
                File zstackRepo = new File(repoDir, "zstack-mn.repo")
                File qemuRepo = new File(repoDir, "qemu-kvm-ev-mn.repo")
                writeRepository(zstackRepo, "zstack-mn", endpoints[0])
                writeRepository(qemuRepo, "qemu-kvm-ev-mn", endpoints[0])
                def before = [zstackRepo.text, qemuRepo.text]

                Process process = runCommand(repoDir.toPath(), endpoints[1])
                assert process.exitValue() == 0: process.errorStream.text
                assert [zstackRepo.text, qemuRepo.text] == before
            } finally {
                repoDir.deleteDir()
            }
        }
    }

    private static void writeRepository(File file, String section, String endpoint) {
        String path = section == "zstack-mn" ? ZSTACK_REPO_PATH : QEMU_REPO_PATH
        file.text = "[${section}]\nname=${section}\nbaseurl=http://${endpoint}:${REST_PORT}${path}\ngpgcheck=0\nmodule_hotfixes=true\nenabled=0\n"
    }

    private static Process runCommand(Path repoDir, String endpoint) {
        String command = SshYumRepoChecker.buildYumRepoEndpointRewriteCommand(
                endpoint, REST_PORT, repoDir.toString())
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", command)
        Process process = builder.start()
        process.waitFor()
        return process
    }

    private static String formatEndpoint(String endpoint) {
        return IPv6NetworkUtils.formatHostPort(endpoint, REST_PORT)
    }
}
