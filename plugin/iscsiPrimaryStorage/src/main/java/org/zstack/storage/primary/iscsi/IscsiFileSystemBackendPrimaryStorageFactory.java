package org.zstack.storage.primary.iscsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.*;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.kvm.KVMAgentCommands.VolumeTO;
import org.zstack.kvm.KVMException;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmExtensionPoint;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 4/19/2015.
 */
public class IscsiFileSystemBackendPrimaryStorageFactory implements PrimaryStorageFactory, KVMStartVmExtensionPoint, Component {
    public static final PrimaryStorageType type = new PrimaryStorageType(IscsiPrimaryStorageConstants.ISCSI_FILE_SYSTEM_BACKEND_PRIMARY_STORAGE_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;
    @Autowired
    private AnsibleFacade asf;

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
    public PrimaryStorageInventory createPrimaryStorage(final PrimaryStorageVO vo, final APIAddPrimaryStorageMsg msg) {
        IscsiFileSystemBackendPrimaryStorageVO ivo = new IscsiFileSystemBackendPrimaryStorageVO(vo);
        APIAddIscsiFileSystemBackendPrimaryStorageMsg amsg = (APIAddIscsiFileSystemBackendPrimaryStorageMsg) msg;
        ivo.setFilesystemType(amsg.getFilesystemType());
        ivo.setHostname(amsg.getHostname());
        ivo.setSshPassword(amsg.getSshPassword());
        ivo.setSshUsername(amsg.getSshUsername());
        ivo.setChapPassword(amsg.getChapPassword());
        ivo.setChapUsername(amsg.getChapUsername());
        ivo.setMountPath(ivo.getUrl());
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

        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            asf.deployModule(IscsiFileSystemBackendPrimaryStorageGlobalProperty.ANSIBLE_MODULE_PATH, IscsiFileSystemBackendPrimaryStorageGlobalProperty.ANSIBLE_PLAYBOOK_NAME);
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, StartVmCmd cmd) throws KVMException {
        List<VolumeTO> dataTOs = new ArrayList<VolumeTO>(cmd.getDataVolumes().size());
        for (final VolumeTO to : cmd.getDataVolumes()) {
            if (!VolumeTO.ISCSI.equals(to.getDeviceType())) {
                dataTOs.add(to);
                continue;
            }

            VolumeInventory vol = CollectionUtils.find(spec.getDestDataVolumes(), new Function<VolumeInventory, VolumeInventory>() {
                @Override
                public VolumeInventory call(VolumeInventory arg) {
                    return arg.getDeviceId() == to.getDeviceId() ? arg : null;
                }
            });

            SimpleQuery<IscsiFileSystemBackendPrimaryStorageVO> q  = dbf.createQuery(IscsiFileSystemBackendPrimaryStorageVO.class);
            q.select(IscsiFileSystemBackendPrimaryStorageVO_.chapUsername, IscsiFileSystemBackendPrimaryStorageVO_.chapPassword);
            q.add(IscsiFileSystemBackendPrimaryStorageVO_.uuid, Op.EQ, vol.getPrimaryStorageUuid());
            Tuple t = q.findTuple();
            if (t == null) {
                // for other ISCSI plugins
                dataTOs.add(to);
            } else {
                String chapUsername = t.get(0, String.class);
                String chapPassword = t.get(1, String.class);

                KVMIscsiVolumeTO kto = new KVMIscsiVolumeTO(to);
                kto.setChapUsername(chapUsername);
                kto.setChapPassword(chapPassword);
                IscsiVolumePath path = new IscsiVolumePath(to.getInstallPath());
                kto.setInstallPath(path.getInstallPath());
                dataTOs.add(kto);
            }
        }

        cmd.setDataVolumes(dataTOs);

        if (cmd.getRootVolume().getDeviceType().equals(VolumeTO.ISCSI)) {
            SimpleQuery<IscsiFileSystemBackendPrimaryStorageVO> q  = dbf.createQuery(IscsiFileSystemBackendPrimaryStorageVO.class);
            q.select(IscsiFileSystemBackendPrimaryStorageVO_.chapUsername, IscsiFileSystemBackendPrimaryStorageVO_.chapPassword);
            q.add(IscsiFileSystemBackendPrimaryStorageVO_.uuid, Op.EQ, spec.getDestRootVolume().getPrimaryStorageUuid());
            Tuple t = q.findTuple();
            if (t != null) {
                String chapUsername = t.get(0, String.class);
                String chapPassword = t.get(1, String.class);

                KVMIscsiVolumeTO kto = new KVMIscsiVolumeTO(cmd.getRootVolume());
                IscsiVolumePath path = new IscsiVolumePath(spec.getDestRootVolume().getInstallPath());
                kto.setInstallPath(path.disassemble().assembleIscsiPath());
                kto.setChapUsername(chapUsername);
                kto.setChapPassword(chapPassword);
                cmd.setRootVolume(kto);
            }
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }
}
