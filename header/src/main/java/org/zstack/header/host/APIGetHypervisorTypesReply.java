package org.zstack.header.host;

import org.zstack.header.message.APIReply;

import java.util.List;

/**
 *@apiResult
 *
 * api reply for message :ref:`APIGetHypervisorTypesMsg`
 *
 *@since 0.1.0
 *
 *@example
 *
 * {
"org.zstack.header.host.APIGetHypervisorTypesReply": {
"hypervisorTypes": [
"KVM",
"Simulator"
],
"success": true
}
}
 */
public class APIGetHypervisorTypesReply extends APIReply {
    /**
     * @desc a list of hypervisor types supported by zstack
     * @choices
     * - KVM
     * - Simulator
     */
    private List<String> hypervisorTypes;

    public List<String> getHypervisorTypes() {
        return hypervisorTypes;
    }

    public void setHypervisorTypes(List<String> hypervisorTypes) {
        this.hypervisorTypes = hypervisorTypes;
    }
}
