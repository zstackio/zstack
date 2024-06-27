package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryIscsiGatewayClientGroupMappingResponse extends XInfiniQueryResponse {
    List<IscsiGatewayClientGroupMappingModule> items;

    public List<IscsiGatewayClientGroupMappingModule> getItems() {
        return items;
    }

    public void setItems(List<IscsiGatewayClientGroupMappingModule> items) {
        this.items = items;
    }
}
