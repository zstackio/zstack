package org.zstack.header.storage.snapshot.group;

/**
 * Created by LiangHanYu on 2022/6/9 18:30
 */
public interface MemorySnapshotValidatorExtensionPoint {
    String checkVmWhereMemorySnapshotExistExternalDevices(String VmInstanceUuid);
}
