package org.zstack.xinfini.sdk.volume;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryVolumeResponse extends XInfiniQueryResponse {
    List<VolumeModule> items;

    public List<VolumeModule> getItems() {
        return items;
    }

    public void setItems(List<VolumeModule> items) {
        this.items = items;
    }
}
