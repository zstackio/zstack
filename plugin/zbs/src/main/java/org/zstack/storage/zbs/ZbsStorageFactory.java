package org.zstack.storage.zbs;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageSvcBuilder;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.PrimaryStorageControllerSvc;
import org.zstack.header.storage.addon.primary.PrimaryStorageNodeSvc;

import java.util.LinkedHashMap;

/**
 * @author Xingwei Yu
 * @date 2024/3/21 11:56
 */
public class ZbsStorageFactory implements ExternalPrimaryStorageSvcBuilder {
    @Override
    public PrimaryStorageControllerSvc buildControllerSvc(ExternalPrimaryStorageVO vo) {
        return new ZbsStorageController(vo);
    }

    @Override
    public PrimaryStorageNodeSvc buildNodeSvc(ExternalPrimaryStorageVO vo) {
        return new ZbsStorageController(vo);
    }

    @Override
    public void discover(String url, String config, ReturnValueCompletion<LinkedHashMap> completion) {

    }

    @Override
    public String getIdentity() {
        return ZbsConstants.IDENTITY;
    }
}
