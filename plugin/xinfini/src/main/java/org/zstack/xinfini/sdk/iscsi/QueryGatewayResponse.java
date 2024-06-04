package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;
import org.zstack.xinfini.sdk.node.NodeModule;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryGatewayResponse extends XInfiniQueryResponse {
    List<GatewayModule> items;

    public List<GatewayModule> getItems() {
        return items;
    }

    public void setItems(List<GatewayModule> items) {
        this.items = items;
    }
}
