package org.zstack.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultDiskOfferingFactory implements DiskOfferingFactory {
    public static DiskOfferingType type = new DiskOfferingType("DefaultDiskOfferingType");

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public DiskOfferingType getDiskOfferingType() {
        return type;
    }

    @Override
    public DiskOfferingInventory createDiskOffering(DiskOfferingVO vo, APICreateDiskOfferingMsg msg) {
        vo = dbf.persistAndRefresh(vo);
        return DiskOfferingInventory.valueOf(vo);
    }

    @Override
    public DiskOffering getDiskOffering(DiskOfferingVO vo) {
        return new DiskOfferingBase(vo);
    }
}
