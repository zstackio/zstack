package org.zstack.xinfini.sdk.vhost;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryBdcBdevResponse extends XInfiniQueryResponse {
    List<BdcBdevModule> items;

    public List<BdcBdevModule> getItems() {
        return items;
    }

    public void setItems(List<BdcBdevModule> items) {
        this.items = items;
    }
}
