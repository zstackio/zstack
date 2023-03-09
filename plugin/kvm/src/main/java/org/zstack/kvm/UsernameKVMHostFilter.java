package org.zstack.kvm;

import org.zstack.compute.host.VolumeMigrationTargetHostFilter;
import org.zstack.core.db.Q;
import org.zstack.header.host.HostVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

/**
 * Created by Bryant on 2017/9/7.
 */
public class UsernameKVMHostFilter implements VolumeMigrationTargetHostFilter {
    @Override
    public List<HostVO> filter(String volumeUuid, List<HostVO> candidates) {
        List<String> toRemoveHuuids = Q.New(HostVO.class).notEq(KVMHostVO_.username, "root").select(KVMHostVO_.uuid).listValues();
        if (!toRemoveHuuids.isEmpty()){
            candidates = CollectionUtils.transformToList(candidates, new Function<HostVO, HostVO>() {
                @Override
                public HostVO call(HostVO arg) {
                    return toRemoveHuuids.contains(arg.getUuid()) ? null : arg;
                }
            });
        }
        return candidates;
    }
}
