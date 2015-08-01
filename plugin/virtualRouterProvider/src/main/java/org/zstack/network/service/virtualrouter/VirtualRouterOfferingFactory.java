package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.db.TransactionalCallback.Operation;
import org.zstack.header.configuration.*;

import javax.persistence.Query;

public class VirtualRouterOfferingFactory implements InstanceOfferingFactory {
	static final InstanceOfferingType type = new InstanceOfferingType(VirtualRouterConstant.VIRTUAL_ROUTER_OFFERING_TYPE);
	
	@Autowired
	private DatabaseFacade dbf;
	
	public InstanceOfferingType getInstanceOfferingType() {
		return type;
	}

	public InstanceOfferingInventory createInstanceOffering(InstanceOfferingVO vo, APICreateInstanceOfferingMsg msg) {
		VirtualRouterOfferingVO rvo = new VirtualRouterOfferingVO(vo);
		APICreateVirtualRouterOfferingMsg amsg = (APICreateVirtualRouterOfferingMsg) msg;
		rvo.setManagementNetworkUuid(amsg.getManagementNetworkUuid());
		rvo.setPublicNetworkUuid(amsg.getPublicNetworkUuid());
		rvo.setZoneUuid(amsg.getZoneUuid());
		rvo.setImageUuid(amsg.getImageUuid());
		rvo = dbf.persistAndRefresh(rvo);

		DefaultVirtualRouterOfferingSelector selector = new DefaultVirtualRouterOfferingSelector();
		selector.setOfferingUuid(rvo.getUuid());
		selector.setZoneUuid(rvo.getZoneUuid());
		selector.setPreferToBeDefault(amsg.isDefault());
        selector.setCreated(true);
		selector.selectDefaultOffering();

		return VirtualRouterOfferingInventory.valueOf(dbf.reload(rvo));
	}

    @Override
    public InstanceOffering getInstanceOffering(InstanceOfferingVO vo) {
        VirtualRouterOfferingVO vro = dbf.findByUuid(vo.getUuid(), VirtualRouterOfferingVO.class);
        return new VirtualRouterOffering(vro);
    }
}
