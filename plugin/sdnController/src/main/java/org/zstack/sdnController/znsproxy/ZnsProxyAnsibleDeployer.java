package org.zstack.sdnController.znsproxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.header.Component;
import org.zstack.sdnController.ZnsProxyGlobalProperty;

public class ZnsProxyAnsibleDeployer implements Component {
    @Autowired
    private AnsibleFacade asf;

    @Override
    public boolean start() {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            asf.deployModule(
                    ZnsProxyGlobalProperty.ANSIBLE_MODULE_PATH,
                    ZnsProxyGlobalProperty.ANSIBLE_PLAYBOOK_NAME);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
