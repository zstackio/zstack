package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryIscsiGatewayResponse extends XInfiniQueryResponse {
    List<IscsiGatewayModule> items;

    public List<IscsiGatewayModule> getItems() {
        return items;
    }

    public void setItems(List<IscsiGatewayModule> items) {
        this.items = items;
    }
}
