package org.zstack.xinfini.sdk.pool;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryPoolResponse extends XInfiniQueryResponse {
    List<PoolModule> items;

    public List<PoolModule> getItems() {
        return items;
    }

    public void setItems(List<PoolModule> items) {
        this.items = items;
    }
}
