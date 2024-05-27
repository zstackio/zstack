package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryVolumeClientMappingResponse extends XInfiniQueryResponse {
    List<VolumeClientMappingModule> items;

    public List<VolumeClientMappingModule> getItems() {
        return items;
    }

    public void setItems(List<VolumeClientMappingModule> items) {
        this.items = items;
    }
}
