package org.zstack.xinfini.sdk.node;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryNodeResponse extends XInfiniQueryResponse {
    List<NodeModule> items;

    public List<NodeModule> getItems() {
        return items;
    }

    public void setItems(List<NodeModule> items) {
        this.items = items;
    }
}
