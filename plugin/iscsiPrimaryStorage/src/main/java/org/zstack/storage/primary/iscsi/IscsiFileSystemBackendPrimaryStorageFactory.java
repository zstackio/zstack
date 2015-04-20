package org.zstack.storage.primary.iscsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.primary.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by frank on 4/19/2015.
 */
public class IscsiFileSystemBackendPrimaryStorageFactory implements PrimaryStorageFactory, Component {
    public static final PrimaryStorageType type = new PrimaryStorageType(IscsiConstants.ISCSI_FILE_SYSTEM_BACKEND_PRIMARY_STORAGE_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    private Map<BackupStorageType, Map<HypervisorType, IscsiFileSystemBackendPrimaryToBackupStorageMediator>> mediators = new HashMap<BackupStorageType, Map<HypervisorType, IscsiFileSystemBackendPrimaryToBackupStorageMediator>>();

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    IscsiFileSystemBackendPrimaryToBackupStorageMediator getPrimaryToBackupStorageMediator(BackupStorageType bsType, HypervisorType hvType) {
        Map<HypervisorType, IscsiFileSystemBackendPrimaryToBackupStorageMediator> mediatorMap = mediators.get(bsType);
        if (mediatorMap == null) {
            throw new CloudRuntimeException(String.format("primary storage[type:%s] wont have mediator supporting backup storage[type:%s]", type, bsType));
        }
        IscsiFileSystemBackendPrimaryToBackupStorageMediator mediator = mediatorMap.get(hvType);
        if (mediator == null) {
            throw new CloudRuntimeException(String.format("PrimaryToBackupStorageMediator[primary storage type: %s, backup storage type: %s] doesn't have backend supporting hypervisor type[%s]" , type, bsType, hvType));
        }
        return mediator;
    }

    private void populateExtensions() {
        for (IscsiFileSystemBackendPrimaryToBackupStorageMediator extp : pluginRgty.getExtensionList(IscsiFileSystemBackendPrimaryToBackupStorageMediator.class)) {
            if (extp.getSupportedPrimaryStorageType().equals(type)) {
                Map<HypervisorType, IscsiFileSystemBackendPrimaryToBackupStorageMediator> map = mediators.get(extp.getSupportedBackupStorageType());
                if (map == null) {
                    map = new HashMap<HypervisorType, IscsiFileSystemBackendPrimaryToBackupStorageMediator>(1);
                }
                for (HypervisorType hvType : extp.getSupportedHypervisorTypes()) {
                    map.put(hvType, extp);
                }
                mediators.put(extp.getSupportedBackupStorageType(), map);
            }
        }
    }



    @Override
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        IscsiFileSystemBackendPrimaryStorageVO ivo = new IscsiFileSystemBackendPrimaryStorageVO(vo);
        APIAddIscsiFileSystemBackendPrimaryStorageMsg amsg = (APIAddIscsiFileSystemBackendPrimaryStorageMsg) msg;
        ivo.setFilesystemType(amsg.getFilesystemType());
        ivo.setHostname(amsg.getHostname());
        ivo.setSshPassword(amsg.getSshPassword());
        ivo.setSshUsername(amsg.getSshUsername());
        ivo.setChapPassword(amsg.getChapPassword());
        ivo.setChapUsername(amsg.getChapUsername());
        ivo = dbf.persistAndRefresh(ivo);
        return IscsiFileSystemBackendPrimaryStorageInventory.valueOf(ivo);
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new IscsiFilesystemBackendPrimaryStorage(dbf.findByUuid(vo.getUuid(), IscsiFileSystemBackendPrimaryStorageVO.class));
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        IscsiFileSystemBackendPrimaryStorageVO vo = dbf.findByUuid(uuid, IscsiFileSystemBackendPrimaryStorageVO.class);
        return IscsiFileSystemBackendPrimaryStorageInventory.valueOf(vo);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
