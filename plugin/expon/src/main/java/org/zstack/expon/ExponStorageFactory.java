package org.zstack.expon;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.addon.primary.*;

import java.util.LinkedHashMap;
import java.util.List;

public class ExponStorageFactory implements ExternalPrimaryStorageSvcBuilder, BackupStorageSelector {

    private List<String> preferBackupStorageTypes;

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

    @Override
    public List<String> getPreferBackupStorageTypes() {
        return preferBackupStorageTypes;
    }

    public void setPreferBackupStorageTypes(List<String> preferBackupStorageTypes) {
        this.preferBackupStorageTypes = preferBackupStorageTypes;
    }
}
