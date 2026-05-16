package org.zstack.sdnController;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.header.network.sdncontroller.*;
import org.zstack.sdnController.header.SdnControllerCanonicalEvents;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractSdnControllerFactory implements SdnControllerFactory {
    private static final CLogger logger = Utils.getLogger(AbstractSdnControllerFactory.class);

    @Autowired
    private DatabaseFacade factoryDbf;
    @Autowired
    private EventFacade evtf;

    @Override
    public void changeSdnControllerStatus(SdnControllerVO vo, SdnControllerStatusEvent event) {
        SdnControllerStatus newStatus = resolveStatus(event, vo);
        if (newStatus == vo.getStatus()) {
            return;
        }
        SdnControllerStatus oldStatus = vo.getStatus();
        logger.debug(String.format("sdn controller[%s] event[%s]: %s -> %s",
                vo.getUuid(), event, oldStatus, newStatus));
        SQL.New(SdnControllerVO.class)
                .eq(SdnControllerVO_.uuid, vo.getUuid())
                .set(SdnControllerVO_.status, newStatus)
                .update();
        vo.setStatus(newStatus);
        SdnControllerCanonicalEvents.SdnControllerStatusChangedData d = new SdnControllerCanonicalEvents.SdnControllerStatusChangedData();
        d.setSdnControllerUuid(vo.getUuid());
        d.setSdnControllerType(vo.getVendorType());
        d.setOldStatus(oldStatus.toString());
        d.setNewStatus(newStatus.toString());
        d.setInv(SdnControllerInventory.valueOf(vo));
        evtf.fire(SdnControllerCanonicalEvents.SDNCONTROLLER_STATUS_CHANGED_PATH, d);
    }
}
