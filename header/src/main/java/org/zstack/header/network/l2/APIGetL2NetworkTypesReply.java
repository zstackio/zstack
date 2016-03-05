package org.zstack.header.network.l2;

import org.zstack.header.message.APIReply;

import java.util.List;

/**
 *@apiResult
 * api reply for message :ref:`APIGetL2NetworkTypesMsg`
 *
 *@category l2network
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.header.network.l2.APIGetL2NetworkTypesReply": {
"l2NetworkTypes": [
"L2VlanNetwork",
"L2NoVlanNetwork"
],
"success": true
}
}
 */

public class APIGetL2NetworkTypesReply extends APIReply {
    /**
     * @desc a list of supported l2Network types
     */
    private List<String> l2NetworkTypes;

    public List<String> getL2NetworkTypes() {
        return l2NetworkTypes;
    }

    public void setL2NetworkTypes(List<String> l2NetworkTypes) {
        this.l2NetworkTypes = l2NetworkTypes;
    }
}
