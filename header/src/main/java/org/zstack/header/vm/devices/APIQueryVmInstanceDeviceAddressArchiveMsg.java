package org.zstack.header.vm.devices;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by LiangHanYu on 2022/6/17 17:31
 */
@AutoQuery(replyClass = APIQueryVmInstanceDeviceAddressArchiveReply.class, inventoryClass = VmInstanceDeviceAddressArchiveInventory.class)
@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/vmInstance/device/address/archive",
        optionalPaths = {"/vmInstance/device/address/archive/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVmInstanceDeviceAddressArchiveReply.class
)
public class APIQueryVmInstanceDeviceAddressArchiveMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }
}
