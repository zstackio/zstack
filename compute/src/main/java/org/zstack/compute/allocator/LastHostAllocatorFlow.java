package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2017/11/8.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LastHostAllocatorFlow extends AbstractHostAllocatorFlow {
    @Override
    public void allocate() {
        throwExceptionIfIAmTheFirstFlow();

        if (spec.isListAllHosts()) {
            next(candidates);
            return;
        }

        final VmInstanceInventory vm = spec.getVmInstance();
        HostVO vo = CollectionUtils.find(candidates, new Function<HostVO, HostVO>() {
            @Override
            public HostVO call(HostVO arg) {
                if (arg.getUuid().equals(vm.getLastHostUuid())) {
                    return arg;
                }
                return null;
            }
        });

        if (vo != null) {
            List<HostVO> vos = new ArrayList<>();
            vos.add(vo);
            next(vos);
        } else {
            next(candidates);
        }
    }
}
