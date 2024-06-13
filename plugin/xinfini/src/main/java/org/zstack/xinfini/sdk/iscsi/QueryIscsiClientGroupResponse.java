package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryIscsiClientGroupResponse extends XInfiniQueryResponse {
    List<IscsiClientGroupModule> items;

    public List<IscsiClientGroupModule> getItems() {
        return items;
    }

    public void setItems(List<IscsiClientGroupModule> items) {
        this.items = items;
    }
}
