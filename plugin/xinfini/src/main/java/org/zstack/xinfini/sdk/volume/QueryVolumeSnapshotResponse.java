package org.zstack.xinfini.sdk.volume;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryVolumeSnapshotResponse extends XInfiniQueryResponse {
    List<VolumeSnapshotModule> items;

    public List<VolumeSnapshotModule> getItems() {
        return items;
    }

    public void setItems(List<VolumeSnapshotModule> items) {
        this.items = items;
    }
}
