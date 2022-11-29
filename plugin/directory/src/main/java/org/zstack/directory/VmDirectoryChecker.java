package org.zstack.directory;

import org.zstack.core.db.Q;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;

import java.util.List;

/**
 * @author shenjin
 * @date 2022/12/11 11:17
 */
public class VmDirectoryChecker implements DirectoryChecker{
    public static DirectoryType type = new DirectoryType(VmInstanceVO.class.getSimpleName());

    @Override
    public boolean check(String directoryUuid, List<String> resourceUuids) {
        DirectoryVO vo = Q.New(DirectoryVO.class).eq(DirectoryVO_.uuid, directoryUuid).find();
        List<String> zoneUuids = Q.New(VmInstanceVO.class).select(VmInstanceVO_.zoneUuid)
                .in(VmInstanceVO_.uuid, resourceUuids).listValues();
        return zoneUuids.stream().allMatch(s -> s.equals(vo.getZoneUuid()));
    }

    @Override
    public DirectoryType getType() {
        return type;
    }
}
