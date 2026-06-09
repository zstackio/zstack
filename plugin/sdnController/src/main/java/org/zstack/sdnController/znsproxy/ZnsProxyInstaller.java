package org.zstack.sdnController.znsproxy;

import org.springframework.util.StringUtils;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.FutureReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.kvm.KVMHostVO;
import org.zstack.sdnController.SdnControllerSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.sdnController.ZnsProxyGlobalProperty.ANSIBLE_MODULE_PATH;
import static org.zstack.sdnController.ZnsProxyGlobalProperty.ANSIBLE_PLAYBOOK_NAME;
import static org.zstack.sdnController.ZnsProxyGlobalProperty.PACKAGE_PATH;
import static org.zstack.sdnController.ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH;
import static org.zstack.sdnController.ZnsProxyGlobalProperty.PROXY_AGENT_PORT;
import static org.zstack.sdnController.ZnsProxyGlobalProperty.PROXY_PACKAGE_NAME;

public class ZnsProxyInstaller {
    private static final long PREPARE_TIMEOUT = TimeUnit.MINUTES.toMillis(10);

    private final DatabaseFacade dbf;

    public ZnsProxyInstaller(DatabaseFacade dbf) {
        this.dbf = dbf;
    }

    public void install(ZnsProxyPrepareServiceCmd cmd) {
        if (cmd == null) {
            throw new CloudRuntimeException("prepare zns-proxy service failed: command is empty");
        }
        List<String> hostUuids = normalizeHostUuids(cmd.hostUuids);

        File localPackage = resolvePackage(cmd);
        for (String hostUuid : hostUuids) {
            installOnHost(hostUuid, localPackage);
            markHostPrepared(hostUuid);
        }
    }

    public void reinstallPreparedHost(String hostUuid) {
        installOnHost(hostUuid, resolveDefaultPackage());
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

    private void markHostPrepared(String hostUuid) {
        SystemTagCreator creator = SdnControllerSystemTags.ZNS_PROXY_PREPARED.newSystemTagCreator(hostUuid);
        creator.ignoreIfExisting = true;
        creator.create();
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
}
