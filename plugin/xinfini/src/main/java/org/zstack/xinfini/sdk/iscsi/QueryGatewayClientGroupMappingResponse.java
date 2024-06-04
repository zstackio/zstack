package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryGatewayClientGroupMappingResponse extends XInfiniQueryResponse {
    List<GatewayClientGroupMappingModule> items;

    public List<GatewayClientGroupMappingModule> getItems() {
        return items;
    }

    public void setItems(List<GatewayClientGroupMappingModule> items) {
        this.items = items;
    }
}
