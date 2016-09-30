package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.*;
import org.zstack.header.host.HostVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AvoidHostAllocatorFlow extends AbstractHostAllocatorFlow {
    @Override
    public void allocate() {
        throwExceptionIfIAmTheFirstFlow();

        List<HostVO> ret = CollectionUtils.transformToList(candidates, new Function<HostVO, HostVO>() {
            @Override
            public HostVO call(HostVO arg) {
                if (!spec.getAvoidHostUuids().contains(arg.getUuid())) {
                    return arg;
                }
                return null;
            }
        });

        if (ret.isEmpty()) {
            fail(String.format("after rule out avoided host%s, there is no host left in candidates", spec.getAvoidHostUuids()));
        } else {
            next(ret);
        }
    }
}
