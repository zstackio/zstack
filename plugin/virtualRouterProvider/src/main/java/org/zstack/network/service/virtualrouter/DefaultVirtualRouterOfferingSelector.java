package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.db.TransactionalCallback.Operation;
import org.zstack.utils.DebugUtils;

import javax.persistence.Query;

/**
 * Created by frank on 7/31/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DefaultVirtualRouterOfferingSelector {
    @Autowired
    private DatabaseFacade dbf;

    private String offeringUuid;
    private String zoneUuid;
    private Boolean preferToBeDefault;
    private boolean created;

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

    public String getOfferingUuid() {
        return offeringUuid;
    }

    public void setOfferingUuid(String offeringUuid) {
        this.offeringUuid = offeringUuid;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public Boolean isPreferToBeDefault() {
        return preferToBeDefault;
    }

    public void setPreferToBeDefault(Boolean preferToBeDefault) {
        this.preferToBeDefault = preferToBeDefault;
    }

    @Transactional
    private void cleanOtherDefault() {
        dbf.entityForTranscationCallback(Operation.UPDATE, VirtualRouterOfferingVO.class);
        String sql = "update VirtualRouterOfferingVO v set v.isDefault = 0 where v.zoneUuid = :zoneUuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("zoneUuid", zoneUuid);
        q.executeUpdate();
    }

    public void selectDefaultOffering() {
        DebugUtils.Assert(zoneUuid != null, "zoneUuid cannot be null");
        DebugUtils.Assert(offeringUuid != null, "offeringUuid cannot be null");

        VirtualRouterOfferingVO offering = dbf.findByUuid(offeringUuid, VirtualRouterOfferingVO.class);
        SimpleQuery<VirtualRouterOfferingVO> vq = dbf.createQuery(VirtualRouterOfferingVO.class);
        vq.add(VirtualRouterOfferingVO_.zoneUuid, Op.EQ, zoneUuid);
        vq.add(VirtualRouterOfferingVO_.uuid, Op.NOT_EQ, offeringUuid);
        if (!vq.isExists() && created) {
            // the first offering is always the default one
            offering.setDefault(true);
            dbf.update(offering);
            return;
        }

        if (preferToBeDefault != null && preferToBeDefault) {
            cleanOtherDefault();
            offering.setDefault(true);
            dbf.update(offering);
        } else if (preferToBeDefault != null) {
            offering.setDefault(false);
            dbf.update(offering);
        }
    }
}
