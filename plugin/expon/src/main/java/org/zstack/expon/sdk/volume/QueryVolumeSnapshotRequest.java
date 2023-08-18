package org.zstack.expon.sdk.volume;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponQuery;
import org.zstack.expon.sdk.ExponRestRequest;

@ExponRestRequest(
        path = "/block/snaps",
        method = HttpMethod.GET,
        responseClass = QueryVolumeSnapshotResponse.class,
        sync = true
)
@ExponQuery(inventoryClass = VolumeModule.class, replyClass = QueryVolumeResponse.class)
public class QueryVolumeSnapshotRequest extends QueryVolumeRequest {
}
