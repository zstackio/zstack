package org.zstack.storage.addon.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.storage.primary.*;
import org.zstack.storage.addon.backup.ExternalBackupStorageFactory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalPrimaryStorageFactory implements PrimaryStorageFactory, Component, PSCapacityExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ExternalBackupStorageFactory.class);
    public static PrimaryStorageType type = new PrimaryStorageType(PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE);

    protected static Map<String, PrimaryStorageControllerSvc> controllers = new HashMap<>();
    protected static Map<String, PrimaryStorageNodeSvc> nodes = new HashMap<>();

    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected DatabaseFacade dbf;


    @Override
    public boolean start() {
        pluginRgty.saveExtensionAsMap(ExternalPrimaryStorageSvcBuilder.class, ExternalPrimaryStorageSvcBuilder::getIdentity);
        List<ExternalPrimaryStorageVO> extPs = dbf.listAll(ExternalPrimaryStorageVO.class);
        for (ExternalPrimaryStorageVO vo : extPs) {
            saveController(vo);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public String buildAllocatedInstallUrl(AllocatePrimaryStorageSpaceMsg msg, PrimaryStorageInventory psInv) {
        PrimaryStorageControllerSvc controller = controllers.get(psInv.getUuid());
        if (controller == null) {
            return psInv.getUrl();
        }

        AllocateSpaceSpec aspec = new AllocateSpaceSpec();
        aspec.setDryRun(true);
        aspec.setSize(msg.getSize());
        aspec.setRequiredUrl(msg.getRequiredInstallUri());
        return controller.allocateSpace(aspec);
    }

    @Override
    public long reserveCapacity(AllocatePrimaryStorageSpaceMsg msg, String allocatedInstallUrl, long size, String psUuid) {
        PrimaryStorageControllerSvc controller = controllers.get(psUuid);
        if (controller == null) {
            return size;
        }

        AllocateSpaceSpec aspec = new AllocateSpaceSpec();
        aspec.setDryRun(false);
        aspec.setSize(msg.getSize());
        aspec.setRequiredUrl(msg.getRequiredInstallUri());
        controller.allocateSpace(aspec);
        return size;
    }

    @Override
    public void releaseCapacity(String allocatedInstallUrl, long size, String psUuid) {

    }

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        APIAddExternalPrimaryStorageMsg amsg = (APIAddExternalPrimaryStorageMsg) msg;
        String identity = amsg.getIdentity();
        ExternalPrimaryStorageSvcBuilder builder = getSvcBuilder(identity);
        if (builder == null) {
            throw new OperationFailureException(
                    Platform.operr("No primary storage plugin registered with identity: %s", identity)
            );
        }

        final ExternalPrimaryStorageVO lvo = new ExternalPrimaryStorageVO(vo);
        lvo.setIdentity(identity);
        lvo.setDefaultProtocol(amsg.getDefaultOutputProtocol());
        lvo.setConfig(amsg.getConfig());
        lvo.setMountPath(identity);
        dbf.persist(lvo);

        saveController(lvo);
        return lvo.toInventory();
    }

    private void saveController(ExternalPrimaryStorageVO extVO) {
        ExternalPrimaryStorageSvcBuilder builder = getSvcBuilder(extVO.getIdentity());
        PrimaryStorageControllerSvc controller = builder.buildControllerSvc(extVO);
        controllers.put(extVO.getUuid(), controller);
        if (controller instanceof PrimaryStorageNodeSvc) {
            nodes.put(extVO.getUuid(), (PrimaryStorageNodeSvc) controller);
        } else {
            nodes.put(extVO.getUuid(), builder.buildNodeSvc(extVO));
        }
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new ExternalPrimaryStorage(vo, controllers.get(vo.getUuid()), nodes.get(vo.getUuid()));
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return ExternalPrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, ExternalPrimaryStorageVO.class));
    }

    public PrimaryStorageControllerSvc getControllerSvc(String primaryStorageUuid) {
        return controllers.get(primaryStorageUuid);
    }

    public PrimaryStorageNodeSvc getNodeSvc(String primaryStorageUuid) {
        return nodes.get(primaryStorageUuid);
    }

    public boolean support(String identity) {
        return getSvcBuilder(identity) != null;
    }

    private ExternalPrimaryStorageSvcBuilder getSvcBuilder(String identity) {
        return pluginRgty.getExtensionFromMap(identity, ExternalPrimaryStorageSvcBuilder.class);
    }
}
