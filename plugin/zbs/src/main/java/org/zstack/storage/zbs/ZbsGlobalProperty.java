package org.zstack.storage.zbs;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

/**
 * @author Xingwei Yu
 * @date 2024/3/27 16:43
 */
@GlobalPropertyDefinition
public class ZbsGlobalProperty {
    @GlobalProperty(name="Zbs.primaryStorage.ansibleModulePath", defaultValue = "ansible/zbsp")
    public static String PRIMARY_STORAGE_MODULE_PATH;
    @GlobalProperty(name = "Zbs.primaryStorage.ansiblePlaybook", defaultValue = "zbsp.py")
    public static String PRIMARY_STORAGE_PLAYBOOK_NAME;
    @GlobalProperty(name = "Zbs.primaryStorage.agentPackageName", defaultValue = "zbsprimarystorage-5.5.0.tar.gz")
    public static String PRIMARY_STORAGE_PACKAGE_NAME;
    @GlobalProperty(name = "Zbs.primaryStorageAgent.port", defaultValue = "7763")
    public static int PRIMARY_STORAGE_AGENT_PORT;
    @GlobalProperty(name="Zbs.primaryStorageAgent.urlRootPath", defaultValue = "")
    public static String PRIMARY_STORAGE_AGENT_URL_ROOT_PATH;
    @GlobalProperty(name="MN.network.", defaultValue = "")
    public static List<String> MN_NETWORKS;
    @GlobalProperty(name="Zbs.vhost.targetImage", defaultValue = "zbs-vhost:latest")
    public static String VHOST_TARGET_IMAGE;
    // empty -> agent computes cores from host cpu count (VHOST_TARGET_CORE_COUNT); set to override e.g. "[0,1]"
    @GlobalProperty(name="Zbs.vhost.targetCores", defaultValue = "")
    public static String VHOST_TARGET_CORES;
    @GlobalProperty(name="Zbs.vhost.targetCoreCount", defaultValue = "2")
    public static int VHOST_TARGET_CORE_COUNT;
    @GlobalProperty(name="Zbs.vhost.hugepageNr", defaultValue = "256")
    public static int VHOST_HUGEPAGE_NR;
    // image delivery: local tar shipped onto the host wins; else agent downloads from this url. either may be empty.
    @GlobalProperty(name="Zbs.vhost.targetImageTar", defaultValue = "")
    public static String VHOST_TARGET_IMAGE_TAR;
    @GlobalProperty(name="Zbs.vhost.targetImageUrl", defaultValue = "")
    public static String VHOST_TARGET_IMAGE_URL;
}
