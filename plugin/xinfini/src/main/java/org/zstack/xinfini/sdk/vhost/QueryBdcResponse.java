package org.zstack.xinfini.sdk.vhost;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryBdcResponse extends XInfiniQueryResponse {
    List<BdcModule> items;

    public List<BdcModule> getItems() {
        return items;
    }

    public void setItems(List<BdcModule> items) {
        this.items = items;
    }
}
