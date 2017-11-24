package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.ReconnectPrimaryStorageMsg;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagLifeCycleExtension;
import org.zstack.storage.ceph.CephSystemTags;

import java.util.Collections;
import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephSystemTagLifeCycleExtension implements SystemTagLifeCycleExtension {
    @Autowired
    private CloudBus bus;

    @Override
    public List<String> getResourceTypeOfSystemTags() {
        return Collections.singletonList(PrimaryStorageVO.class.getSimpleName());
    }

    private void tryReconnect(final SystemTagInventory tag) {
        if (!CephSystemTags.NO_CEPHX.isMatch(tag.getTag())) {
            return;
        }

        final String psUuid = tag.getResourceUuid();
        final PrimaryStorageStatus status = Q.New(CephPrimaryStorageVO.class)
                .eq(CephPrimaryStorageVO_.uuid, psUuid)
                .select(CephPrimaryStorageVO_.status)
                .findValue();
        if (status == PrimaryStorageStatus.Connecting) {
            return;
        }

        ReconnectPrimaryStorageMsg rmsg = new ReconnectPrimaryStorageMsg();
        rmsg.setPrimaryStorageUuid(psUuid);
        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, psUuid);
        bus.send(rmsg);
    }

    @Override
    public void tagCreated(SystemTagInventory tag) {
        tryReconnect(tag);
    }

    @Override
    public void tagDeleted(SystemTagInventory tag) {
        tryReconnect(tag);
    }

    @Override
    public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
    }
}
