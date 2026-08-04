package org.zstack.sdnController.znsproxy;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.springframework.util.StringUtils;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.FutureReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.kvm.KVMHostVO;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.zstack.sdnController.ZnsProxyGlobalProperty.ANSIBLE_MODULE_PATH;
import static org.zstack.sdnController.ZnsProxyGlobalProperty.ANSIBLE_PLAYBOOK_NAME;
import static org.zstack.sdnController.ZnsProxyGlobalProperty.PACKAGE_PATH;
import static org.zstack.sdnController.ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH;
import static org.zstack.sdnController.ZnsProxyGlobalProperty.PROXY_AGENT_PORT;
import static org.zstack.sdnController.ZnsProxyGlobalProperty.PROXY_PACKAGE_NAME;

public class ZnsProxyInstaller {
    private static final long PREPARE_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
    private static final String MANIFEST_NAME = "zns-proxy-manifest.json";
    private static final Pattern COMPONENT_VERSION_PATTERN = Pattern.compile(
            "^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$");
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-f]{64}$");
    private static final Gson GSON = new Gson();

    private final DatabaseFacade dbf;

    public ZnsProxyInstaller(DatabaseFacade dbf) {
        this.dbf = dbf;
    }

    public void install(ZnsProxyPrepareServiceCmd cmd) {
        if (cmd == null) {
            throw new CloudRuntimeException("prepare zns-proxy service failed: command is empty");
        }
        List<String> hostUuids = normalizeHostUuids(cmd.hostUuids);

        File localPackage = resolveAndVerifyPackage(cmd);
        for (String hostUuid : hostUuids) {
            installOnHost(hostUuid, localPackage);
        }
    }

    public void ensureHost(String hostUuid) {
        installOnHost(hostUuid, resolveAndVerifyPackage(new ZnsProxyPrepareServiceCmd()));
    }

    void installOnHost(String hostUuid, File localPackage) {
        KVMHostVO host = dbf.findByUuid(hostUuid, KVMHostVO.class);
        if (host == null) {
            throw new CloudRuntimeException(String.format("prepare zns-proxy service failed: host %s not found", hostUuid));
        }
        if (StringUtils.isEmpty(host.getManagementIp())) {
            throw new CloudRuntimeException(String.format("prepare zns-proxy service failed: host %s has empty management ip", hostUuid));
        }
        if (StringUtils.isEmpty(host.getUsername())) {
            throw new CloudRuntimeException(String.format("prepare zns-proxy service failed: host %s has empty username", hostUuid));
        }
        if (StringUtils.isEmpty(host.getPassword())) {
            throw new CloudRuntimeException(String.format("prepare zns-proxy service failed: host %s has empty password", hostUuid));
        }

        SshFileMd5Checker checker = new SshFileMd5Checker();
        checker.setTargetIp(host.getManagementIp());
        checker.setUsername(host.getUsername());
        checker.setPassword(host.getPassword());
        checker.setSshPort(normalizeSshPort(host));
        checker.addSrcDestPair(localPackage.getAbsolutePath(), PACKAGE_PATH);

        AnsibleRunner runner = new AnsibleRunner();
        runner.installChecker(checker);
        runner.setRunOnLocal(true);
        runner.setForceRun(true);
        runner.setPassword(host.getPassword());
        runner.setUsername(host.getUsername());
        runner.setSshPort(normalizeSshPort(host));
        runner.setTargetIp(host.getManagementIp());
        runner.setTargetUuid(host.getUuid());
        runner.setAgentPort(PROXY_AGENT_PORT);
        runner.setPlayBookName(ANSIBLE_PLAYBOOK_NAME);
        runner.setDeployArguments(new ZnsProxyDeployArguments(localPackage, PACKAGE_PATH, healthUrl()));

        FutureReturnValueCompletion completion = new FutureReturnValueCompletion(null);
        runner.run(completion);
        completion.await(PREPARE_TIMEOUT);
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    public static String buildInstallCommand(String packagePath) {
        return shellQuote(packagePath) + " install";
    }

    public static File resolvePackage(ZnsProxyPrepareServiceCmd cmd) {
        if (cmd == null) {
            throw new CloudRuntimeException("prepare zns-proxy service failed: command is empty");
        }

        String packageName;
        if (StringUtils.hasText(cmd.packageName)) {
            packageName = cmd.packageName.trim();
        } else if (StringUtils.hasText(cmd.proxyVersion)) {
            packageName = "zns-proxy-" + cmd.proxyVersion.trim() + ".bin";
        } else {
            packageName = PROXY_PACKAGE_NAME;
        }

        File localPackage = packageInRepository(packageName);
        if (localPackage.exists() && localPackage.isFile()) {
            return localPackage;
        }

        File classPathPackage = packageInAnsibleModule(packageName);
        if (classPathPackage == null || !classPathPackage.exists() || !classPathPackage.isFile()) {
            throw new CloudRuntimeException(String.format(
                    "prepare zns-proxy service failed: package %s not found in %s or classpath %s",
                    packageName, PACKAGE_REPOSITORY_PATH, ANSIBLE_MODULE_PATH));
        }
        return classPathPackage;
    }

    public static File resolveDefaultPackage() {
        ZnsProxyPrepareServiceCmd cmd = new ZnsProxyPrepareServiceCmd();
        return resolvePackage(cmd);
    }

    public static File resolveAndVerifyPackage(ZnsProxyPrepareServiceCmd cmd) {
        if (cmd == null) {
            throw new CloudRuntimeException("prepare zns-proxy service failed: command is empty");
        }

        String packageName = resolvePackageName(cmd);
        File classPathPackage = packageInAnsibleModule(packageName);
        File localPackage;
        File manifestFile;
        if (isFile(classPathPackage)) {
            localPackage = classPathPackage;
            manifestFile = adjacentManifest(localPackage);
        } else {
            localPackage = packageInRepository(packageName);
            manifestFile = adjacentManifest(localPackage);
        }
        if (!isFile(localPackage)) {
            throw new CloudRuntimeException(String.format(
                    "prepare zns-proxy service failed: package %s not found in classpath %s or %s",
                    packageName, ANSIBLE_MODULE_PATH, PACKAGE_REPOSITORY_PATH));
        }
        if (!isFile(manifestFile)) {
            throw new CloudRuntimeException(String.format(
                    "prepare zns-proxy service failed: manifest %s not found next to package %s",
                    MANIFEST_NAME, localPackage.getAbsolutePath()));
        }

        ZnsProxyManifest manifest;
        try (FileReader reader = new FileReader(manifestFile)) {
            manifest = GSON.fromJson(reader, ZnsProxyManifest.class);
        } catch (IOException | JsonParseException e) {
            throw new CloudRuntimeException(String.format(
                    "prepare zns-proxy service failed: cannot read manifest %s: %s",
                    manifestFile.getAbsolutePath(), e.getMessage()));
        }
        verifyManifest(localPackage, manifestFile, manifest);
        return localPackage;
    }

    static String healthUrl() {
        return "http://127.0.0.1:" + PROXY_AGENT_PORT + "/zns-proxy/api/v1/health";
    }

    private static List<String> normalizeHostUuids(List<String> hostUuids) {
        if (hostUuids == null || hostUuids.isEmpty()) {
            throw new CloudRuntimeException("prepare zns-proxy service failed: hostUuids is empty");
        }

        List<String> normalized = new ArrayList<String>(hostUuids.size());
        for (String hostUuid : hostUuids) {
            if (!StringUtils.hasText(hostUuid)) {
                throw new CloudRuntimeException("prepare zns-proxy service failed: hostUuids is empty");
            }
            normalized.add(hostUuid.trim());
        }
        return normalized;
    }

    private static String shellQuote(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private static File packageInRepository(String packageName) {
        validatePackageName(packageName);
        return new File(PACKAGE_REPOSITORY_PATH, packageName);
    }

    private static File packageInAnsibleModule(String packageName) {
        validatePackageName(packageName);
        return PathUtil.findFileOnClassPath(String.format("%s/%s", ANSIBLE_MODULE_PATH, packageName));
    }

    private static File adjacentManifest(File localPackage) {
        return localPackage == null ? null : new File(localPackage.getParentFile(), MANIFEST_NAME);
    }

    private static boolean isFile(File file) {
        return file != null && file.exists() && file.isFile();
    }

    private static String resolvePackageName(ZnsProxyPrepareServiceCmd cmd) {
        if (StringUtils.hasText(cmd.packageName)) {
            return cmd.packageName.trim();
        }
        if (StringUtils.hasText(cmd.proxyVersion)) {
            return "zns-proxy-" + cmd.proxyVersion.trim() + ".bin";
        }
        return PROXY_PACKAGE_NAME;
    }

    private static void verifyManifest(File localPackage, File manifestFile, ZnsProxyManifest manifest) {
        if (manifest == null) {
            throw invalidManifest(manifestFile, "content is empty");
        }
        if (!"zns-proxy".equals(manifest.component)) {
            throw invalidManifest(manifestFile, "component must be zns-proxy");
        }
        if (!localPackage.getName().equals(manifest.packageName) ||
                !localPackage.getName().equals(manifest.path)) {
            throw invalidManifest(manifestFile, "packageName and path must match " + localPackage.getName());
        }
        if (manifest.version == null || !COMPONENT_VERSION_PATTERN.matcher(manifest.version).matches()) {
            throw invalidManifest(manifestFile, "version must be a canonical four-part version");
        }
        if (manifest.sha256 == null || !SHA256_PATTERN.matcher(manifest.sha256).matches()) {
            throw invalidManifest(manifestFile, "sha256 must be lowercase hexadecimal");
        }
        if (manifest.arch == null || manifest.arch.isEmpty()) {
            throw invalidManifest(manifestFile, "arch must be a non-empty list");
        }
        if (!StringUtils.hasText(manifest.buildTime)) {
            throw invalidManifest(manifestFile, "buildTime is required");
        }
        String actualSha256 = sha256(localPackage);
        if (!actualSha256.equals(manifest.sha256)) {
            throw invalidManifest(manifestFile, String.format(
                    "sha256 mismatch for %s: expected %s, actual %s",
                    localPackage.getName(), manifest.sha256, actualSha256));
        }
    }

    private static CloudRuntimeException invalidManifest(File manifestFile, String reason) {
        return new CloudRuntimeException(String.format(
                "prepare zns-proxy service failed: invalid manifest %s: %s",
                manifestFile.getAbsolutePath(), reason));
    }

    private static String sha256(File file) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            try (InputStream stream = new FileInputStream(file)) {
                byte[] buffer = new byte[1024 * 1024];
                int count;
                while ((count = stream.read(buffer)) != -1) {
                    digest.update(buffer, 0, count);
                }
            }
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest.digest()) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format(
                    "prepare zns-proxy service failed: calculate sha256 for %s: %s",
                    file.getAbsolutePath(), e.getMessage()));
        }
    }

    private static int normalizeSshPort(KVMHostVO host) {
        return host.getPort() == null || host.getPort() <= 0 ? 22 : host.getPort();
    }

    private static void validatePackageName(String packageName) {
        if (!StringUtils.hasText(packageName)) {
            throw new CloudRuntimeException("prepare zns-proxy service failed: packageName is empty");
        }
        String trimmed = packageName.trim();
        if (!trimmed.equals(new File(trimmed).getName()) || trimmed.contains("..")) {
            throw new CloudRuntimeException(String.format(
                    "prepare zns-proxy service failed: invalid packageName %s",
                    packageName));
        }
    }

    private static class ZnsProxyManifest {
        String component;
        String packageName;
        String version;
        List<String> arch;
        String sha256;
        String path;
        String buildTime;
    }
}
