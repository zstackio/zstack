package org.zstack.core.ansible;

import org.zstack.core.CoreGlobalProperty;
import org.zstack.utils.path.PathUtil;

/**
 */
public interface AnsibleConstant {
    public static String SERVICE_ID = "ansible";
    public static String ROOT_DIR = PathUtil.getFolderUnderZStackHomeFolder("ansible");
    public static String ZSTACKLIB_ROOT = PathUtil.getFolderUnderZStackHomeFolder("ansible/files/zstacklib/");
    public static String INVENTORY_FILE = PathUtil.getFilePathUnderZStackHomeFolder("ansible/hosts");
    public static String PYPI_REPO = PathUtil.getFilePathUnderZStackHomeFolder(String.format("apache-tomcat/webapps/%s/static/pypi/simple",CoreGlobalProperty.OEM_NAME));
    public static String CONFIGURATION_FILE = PathUtil.getFilePathUnderZStackHomeFolder("ansible/ansible.cfg");
    public static String LOG_PATH = PathUtil.getFilePathUnderZStackHomeFolder("ansible/log");
    public static String IMPORT_PUBLIC_KEY_SCRIPT_PATH = "ansible/import_public_key.sh";
    public static String RSA_PUBLIC_KEY = "ansible/rsaKeys/id_rsa.pub";
    public static String RSA_PRIVATE_KEY = "ansible/rsaKeys/id_rsa";

    public static String KVM_AGENT_NAME = "zstack-kvmagent";
}
