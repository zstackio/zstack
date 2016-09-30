package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.allocator.*;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.ArrayList;
import java.util.List;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LastHostPreferredAllocatorFlow extends AbstractHostAllocatorFlow {
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
            ArrayList<HostVO> vos = new ArrayList<HostVO>();
            vos.add(vo);
            next(vos);
        } else {
            next(candidates);
        }
    }
}
