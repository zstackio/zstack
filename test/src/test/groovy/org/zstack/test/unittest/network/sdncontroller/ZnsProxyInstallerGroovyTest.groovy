package org.zstack.test.unittest.network.sdncontroller

import groovy.json.JsonOutput
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.sdnController.ZnsProxyGlobalProperty
import org.zstack.sdnController.znsproxy.ZnsProxyInstaller

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import static org.mockito.Mockito.mock

class ZnsProxyInstallerGroovyTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    private String originalPackageRepositoryPath
    private String originalProxyPackageName
    private String originalAnsibleModulePath

    @Before
    void setUp() {
        originalPackageRepositoryPath = ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH
        originalProxyPackageName = ZnsProxyGlobalProperty.PROXY_PACKAGE_NAME
        originalAnsibleModulePath = ZnsProxyGlobalProperty.ANSIBLE_MODULE_PATH
        ZnsProxyGlobalProperty.PROXY_PACKAGE_NAME = "zns-proxy.bin"
        ZnsProxyGlobalProperty.ANSIBLE_MODULE_PATH = "missing/znsproxy-${System.nanoTime()}"
    }

    @After
    void tearDown() {
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = originalPackageRepositoryPath
        ZnsProxyGlobalProperty.PROXY_PACKAGE_NAME = originalProxyPackageName
        ZnsProxyGlobalProperty.ANSIBLE_MODULE_PATH = originalAnsibleModulePath
    }

    @Test
    void testResolvesRepositoryPackageWithValidManifest() {
        File repo = tempFolder.newFolder("repository-package")
        File pkg = validPackage(repo, "1.2.0.1")
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.absolutePath

        assert ZnsProxyInstaller.resolveAndVerifyPackage().absolutePath == pkg.absolutePath
    }

    @Test
    void testPrefersCompleteClasspathRelease() {
        File repo = tempFolder.newFolder("stale-repository")
        new File(repo, "zns-proxy.bin").setText("stale-proxy", StandardCharsets.UTF_8.name())
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.absolutePath

        URL classPathRoot = getClass().classLoader.getResource("")
        File module = new File(new File(classPathRoot.toURI()), "ansible/znsproxy-groovy-${System.nanoTime()}")
        assert module.mkdirs()
        ZnsProxyGlobalProperty.ANSIBLE_MODULE_PATH = "ansible/${module.name}"
        File classPathPackage = validPackage(module, "1.2.0.1", "current-proxy")

        try {
            assert ZnsProxyInstaller.resolveAndVerifyPackage().absolutePath == classPathPackage.absolutePath
        } finally {
            FileUtils.deleteDirectory(module)
        }
    }

    @Test
    void testRejectsShaMismatchAndNonCanonicalVersion() {
        File shaRepo = tempFolder.newFolder("sha-mismatch")
        File shaPackage = packageFile(shaRepo)
        writeManifest(shaRepo, shaPackage, "a" * 64, "1.2.0.1")
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = shaRepo.absolutePath
        assert expectCloudRuntime { ZnsProxyInstaller.resolveAndVerifyPackage() }.message.contains("sha256 mismatch")

        File versionRepo = tempFolder.newFolder("invalid-version")
        File versionPackage = packageFile(versionRepo)
        writeManifest(versionRepo, versionPackage, sha256(versionPackage), "1.2.0")
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = versionRepo.absolutePath
        assert expectCloudRuntime { ZnsProxyInstaller.resolveAndVerifyPackage() }.message.contains(
                "version must be a canonical four-part version")
    }

    @Test
    void testEnsureHostUsesVerifiedPackageBeforeHostLookup() {
        File repo = tempFolder.newFolder("ensure-host")
        validPackage(repo, "1.2.0.1")
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.absolutePath
        DatabaseFacade dbf = mock(DatabaseFacade.class)

        CloudRuntimeException error = expectCloudRuntime {
            new ZnsProxyInstaller(dbf).ensureHost("missing-host")
        }

        assert error.message.contains("host missing-host not found")
    }

    private static File validPackage(File directory, String version, String content = "proxy") {
        File pkg = packageFile(directory, content)
        writeManifest(directory, pkg, sha256(pkg), version)
        return pkg
    }

    private static File packageFile(File directory, String content = "proxy") {
        File pkg = new File(directory, "zns-proxy.bin")
        pkg.setText(content, StandardCharsets.UTF_8.name())
        return pkg
    }

    private static void writeManifest(File directory, File pkg, String checksum, String version) {
        new File(directory, "zns-proxy-manifest.json").setText(JsonOutput.toJson([
                component  : "zns-proxy",
                packageName: pkg.name,
                version    : version,
                arch       : ["amd64"],
                sha256     : checksum,
                path       : pkg.name,
                buildTime  : "2026-07-31T00:00:00Z",
        ]), StandardCharsets.UTF_8.name())
    }

    private static String sha256(File file) {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(file.bytes)
        return digest.collect { String.format("%02x", it & 0xff) }.join()
    }

    private static CloudRuntimeException expectCloudRuntime(Closure action) {
        try {
            action.call()
        } catch (CloudRuntimeException error) {
            return error
        }
        throw new AssertionError("expected CloudRuntimeException")
    }
}
