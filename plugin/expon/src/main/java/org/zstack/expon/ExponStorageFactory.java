package org.zstack.expon;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageSvcBuilder;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.PrimaryStorageControllerSvc;
import org.zstack.header.storage.addon.primary.PrimaryStorageNodeSvc;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;

import java.util.LinkedHashMap;

public class ExponStorageFactory implements ExternalPrimaryStorageSvcBuilder {
    @Override
    public PrimaryStorageControllerSvc buildControllerSvc(ExternalPrimaryStorageVO vo) {
        return new ExponStorageController(vo);
    }

    @Override
    public PrimaryStorageNodeSvc buildNodeSvc(ExternalPrimaryStorageVO vo) {
        return new ExponStorageController(vo);
    }

    @Override
    public void discover(String url, String config, ReturnValueCompletion<LinkedHashMap> completion) {
        ExponStorageController controller = new ExponStorageController(url);
        controller.connect(config, url, completion);
    }

    @Override
    public String getIdentity() {
        return ExponConstants.IDENTITY;
    }
}
