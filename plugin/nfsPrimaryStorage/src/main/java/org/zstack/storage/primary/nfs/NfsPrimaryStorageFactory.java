package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotTag;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.primary.PrimaryStorageManager;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse;
import org.zstack.tag.TagManager;
import org.zstack.utils.path.PathUtil;

import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class NfsPrimaryStorageFactory implements NfsPrimaryStorageManager, PrimaryStorageFactory, Component {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private PrimaryStorageManager psMgr;
    @Autowired
    private ErrorFacade errf;

    private Map<String, NfsPrimaryStorageBackend> backends = new HashMap<String, NfsPrimaryStorageBackend>();
    private Map<BackupStorageType, Map<HypervisorType, NfsPrimaryToBackupStorageMediator>> mediators = new HashMap<BackupStorageType, Map<HypervisorType, NfsPrimaryToBackupStorageMediator>>();
    
	private static final PrimaryStorageType type = new PrimaryStorageType(NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE);

	@Override
	public PrimaryStorageType getPrimaryStorageType() {
		return type;
	}

	@Override
	public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
	    String mountPathBase = NfsPrimaryStorageGlobalConfig.MOUNT_BASE.value(String.class);
	    if (mountPathBase == null) {
	        mountPathBase = NfsPrimaryStorageConstant.DEFAULT_NFS_MOUNT_PATH_ON_HOST;
	    }
	    String mountPath = PathUtil.join(mountPathBase, "prim-" + vo.getUuid());
	    vo.setMountPath(mountPath);
		vo = dbf.persistAndRefresh(vo);

        tagMgr.createSysTag(vo.getUuid(), VolumeSnapshotTag.CAPABILITY_HYPERVISOR_SNAPSHOT.completeTag(KVMConstant.KVM_HYPERVISOR_TYPE),
                PrimaryStorageVO.class.getSimpleName());
        return PrimaryStorageInventory.valueOf(vo);
	}

	@Override
	public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
	    return new NfsPrimaryStorage(vo);
	}

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        PrimaryStorageVO vo = dbf.findByUuid(uuid, PrimaryStorageVO.class);
        return PrimaryStorageInventory.valueOf(vo);
    }

    private void populateExtensions() {
        for (NfsPrimaryStorageBackend extp : pluginRgty.getExtensionList(NfsPrimaryStorageBackend.class)) {
            NfsPrimaryStorageBackend old = backends.get(extp.getHypervisorType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate NfsPrimaryStorageBackend[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), old.getHypervisorType()));
            }
            backends.put(extp.getHypervisorType().toString(), extp);
        }

	    for (NfsPrimaryToBackupStorageMediator extp : pluginRgty.getExtensionList(NfsPrimaryToBackupStorageMediator.class)) {
	        if (extp.getSupportedPrimaryStorageType().equals(type)) {
	            Map<HypervisorType, NfsPrimaryToBackupStorageMediator> map = mediators.get(extp.getSupportedBackupStorageType());
	            if (map == null) {
	                map = new HashMap<HypervisorType, NfsPrimaryToBackupStorageMediator>(1);
	            }
                for (HypervisorType hvType : extp.getSupportedHypervisorTypes()) {
                    map.put(hvType, extp);
                }
	            mediators.put(extp.getSupportedBackupStorageType(), map);
	        }
	    }
	}
	
	NfsPrimaryToBackupStorageMediator getPrimaryToBackupStorageMediator(BackupStorageType bsType, HypervisorType hvType) {
	    Map<HypervisorType, NfsPrimaryToBackupStorageMediator> mediatorMap = mediators.get(bsType);
	    if (mediatorMap == null) {
	        throw new CloudRuntimeException(String.format("primary storage[type:%s] wont have mediator supporting backup storage[type:%s]", type, bsType));
	    }
	    NfsPrimaryToBackupStorageMediator mediator = mediatorMap.get(hvType);
	    if (mediator == null) {
	        throw new CloudRuntimeException(String.format("PrimaryToBackupStorageMediator[primary storage type: %s, backup storage type: %s] doesn't have backend supporting hypervisor type[%s]" , type, bsType, hvType));
	    }
	    return mediator;
	}

    @Override
    public boolean start() {
        populateExtensions();
        NfsPrimaryStorageGlobalConfig.MOUNT_BASE.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                if (!value.startsWith("/")) {
                    throw new GlobalConfigException(String.format("%s must be an absolute path starting with '/'", NfsPrimaryStorageGlobalConfig.MOUNT_BASE.getCanonicalName()));
                }
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
    
    public NfsPrimaryStorageBackend getHypervisorBackend(HypervisorType hvType) {
        NfsPrimaryStorageBackend backend = backends.get(hvType.toString());
        if (backend == null) {
            throw new CloudRuntimeException(String.format("Cannot find hypervisor backend for nfs primary storage supporting hypervisor type[%s]", hvType));
        }
        return backend;
    }
    
    @Transactional
    public HostInventory getConnectedHostForOperation(PrimaryStorageInventory pri) {
        if (pri.getAttachedClusterUuids().isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("cannot find a Connected host to execute command for nfs primary storage[uuid:%s]", pri.getUuid())
            ));
        }
        
        String sql = "select h from HostVO h where h.state = :state and h.status = :connectionState and h.clusterUuid in (:clusterUuids)";
        TypedQuery<HostVO> q = dbf.getEntityManager().createQuery(sql, HostVO.class);
        q.setParameter("state", HostState.Enabled);
        q.setParameter("connectionState", HostStatus.Connected);
        q.setParameter("clusterUuids", pri.getAttachedClusterUuids());
        q.setMaxResults(1);
        List<HostVO> ret = q.getResultList();
        if (ret.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("cannot find a Connected host to execute command for nfs primary storage[uuid:%s]", pri.getUuid())
            ));
        } else {
            Collections.shuffle(ret);
            return HostInventory.valueOf(ret.get(0));
        }
    }

    @Override
    public void reportCapacityIfNeeded(String psUuid, NfsPrimaryStorageAgentResponse rsp) {
        if (rsp.getAvailableCapacity() != null && rsp.getTotalCapacity() != null) {
            psMgr.sendCapacityReportMessage(rsp.getTotalCapacity(), rsp.getAvailableCapacity(), psUuid);
        }
    }

    @Override
    public HypervisorType findHypervisorTypeByImageFormatAndPrimaryStorageUuid(String imageFormat, final String psUuid) {
        HypervisorType hvType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(imageFormat);
        if (hvType != null) {
            return hvType;
        }

        String type = new Callable<String>() {
            @Override
            @Transactional(readOnly = true)
            public String call() {
                String sql = "select c.hypervisorType from ClusterVO c, PrimaryStorageClusterRefVO ref where c.uuid = ref.clusterUuid and ref.primaryStorageUuid = :psUuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("psUuid", psUuid);
                List<String> types = q.getResultList();
                return types.isEmpty() ? null : types.get(0);
            }
        }.call();

        if (type != null) {
            return HypervisorType.valueOf(type);
        }

        throw new OperationFailureException(errf.stringToOperationError(
                String.format("cannot find proper hypervisorType for primary storage[uuid:%s] to handle image format or volume format[%s]", psUuid, imageFormat)
        ));
    }
}
