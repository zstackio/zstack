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

	@Transactional
	private void cleanOtherDefault(APICreateVirtualRouterOfferingMsg msg) {
		dbf.entityForTranscationCallback(Operation.UPDATE, VirtualRouterOfferingVO.class);
		String sql = "update VirtualRouterOfferingVO v set v.isDefault = 0 where v.zoneUuid = :zoneUuid";
		Query q = dbf.getEntityManager().createQuery(sql);
		q.setParameter("zoneUuid", msg.getZoneUuid());
		q.executeUpdate();
	}
	
	public InstanceOfferingInventory createInstanceOffering(InstanceOfferingVO vo, APICreateInstanceOfferingMsg msg) {
		VirtualRouterOfferingVO rvo = new VirtualRouterOfferingVO(vo);
		APICreateVirtualRouterOfferingMsg amsg = (APICreateVirtualRouterOfferingMsg) msg;
		rvo.setManagementNetworkUuid(amsg.getManagementNetworkUuid());
		rvo.setPublicNetworkUuid(amsg.getPublicNetworkUuid());
		rvo.setZoneUuid(amsg.getZoneUuid());
		rvo.setImageUuid(amsg.getImageUuid());

		SimpleQuery<VirtualRouterOfferingVO> q = dbf.createQuery(VirtualRouterOfferingVO.class);
		q.add(VirtualRouterOfferingVO_.zoneUuid, Op.EQ, amsg.getZoneUuid());
		q.add(VirtualRouterOfferingVO_.isDefault, Op.EQ, true);
        if (!q.isExists()) {
            rvo.setDefault(true);
        } else {
            rvo.setDefault(amsg.isDefault());
        }
		if (amsg.isDefault()) {
			cleanOtherDefault(amsg);
		}
		rvo = dbf.persistAndRefresh(rvo);
		return VirtualRouterOfferingInventory.valueOf(rvo);
	}

    @Override
    public InstanceOffering getInstanceOffering(InstanceOfferingVO vo) {
        VirtualRouterOfferingVO vro = dbf.findByUuid(vo.getUuid(), VirtualRouterOfferingVO.class);
        return new VirtualRouterOffering(vro);
    }
}
