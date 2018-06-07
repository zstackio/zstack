package org.zstack.compute.allocator;

import org.zstack.core.db.SQL;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostAllocatorStrategyType;
import org.zstack.header.host.HostAllocateExtensionPoint;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;

public class LeastVmPreferredHostAllocatorStrategyFactory extends AbstractHostAllocatorStrategyFactory implements HostAllocateExtensionPoint {
	private static final HostAllocatorStrategyType type = new HostAllocatorStrategyType(HostAllocatorConstant.LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE);

	@Override
    public HostAllocatorStrategyType getHostAllocatorStrategyType() {
	    return type;
    }

	@Override
	public void beforeAllocateHostSuccessReply(HostAllocatorSpec spec, String replyHostUuid) {
		if (!HostAllocatorConstant.LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE.equals(spec.getAllocatorStrategy())) {
			return;
		}

		String vmInstanceUuid = spec.getVmInstance().getUuid();
		SQL.New(VmInstanceVO.class)
				.set(VmInstanceVO_.hostUuid, replyHostUuid)
				.eq(VmInstanceVO_.uuid, vmInstanceUuid)
				.update();
	}
}
