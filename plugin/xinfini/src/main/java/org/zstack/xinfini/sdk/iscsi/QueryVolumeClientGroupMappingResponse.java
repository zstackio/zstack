package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryVolumeClientGroupMappingResponse extends XInfiniQueryResponse {
    List<VolumeClientGroupMappingModule> items;

    public List<VolumeClientGroupMappingModule> getItems() {
        return items;
    }

    public void setItems(List<VolumeClientGroupMappingModule> items) {
        this.items = items;
    }
}
