package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.header.network.service.NetworkServiceProviderType;

/**
 * Created by xing5 on 2016/10/31.
 */
public interface VyosConstants {
    String VYOS_VM_TYPE = "Vyos";
    String VYOS_ROUTER_PROVIDER_TYPE = "Vyos";

    String ANSIBLE_PLAYBOOK_NAME = "zvr.py";
    String ANSIBLE_MODULE_PATH = "ansible/zvr";

    NetworkServiceProviderType PROVIDER_TYPE = new NetworkServiceProviderType(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE);

    enum BootstrapInfoKey {
        vyosPassword
    }
}
