package org.zstack.header.storage.snapshot.group;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by LiangHanYu on 2022/6/9 18:30
 */
public interface MemorySnapshotValidatorExtensionPoint {
    ErrorCode checkVmWhereMemorySnapshotExistExternalDevices(String VmInstanceUuid);
}
