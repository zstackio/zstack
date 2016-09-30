package org.zstack.core.ansible;

/**
 */
public interface AnsibleChecker {
    boolean needDeploy();

    void deleteDestFile();
}
