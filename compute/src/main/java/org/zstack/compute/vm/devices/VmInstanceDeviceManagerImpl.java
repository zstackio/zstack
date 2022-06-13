package org.zstack.compute.vm.devices;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.header.vm.devices.*;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class VmInstanceDeviceManagerImpl implements VmInstanceDeviceManager {
    private static final CLogger logger = Utils.getLogger(VmInstanceDeviceManagerImpl.class);
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public VmInstanceDeviceAddressVO createOrUpdateVmDeviceAddress(String resourceUuid, PciAddressConfig pciAddress, String vmInstanceUuid, String metadata, String metadataClass) {
        if (resourceUuid == null || vmInstanceUuid == null) {
            throw new OperationFailureException(operr("missing parameter, resourceUuid: %s, vmInstanceUuid: %s is requested", resourceUuid, vmInstanceUuid));
        }

        ErrorCode errorCode = checkParams(vmInstanceUuid, resourceUuid);
        if (errorCode != null) {
            throw new OperationFailureException(errorCode);
        }

        boolean addressExists = Q.New(VmInstanceDeviceAddressVO.class)
                .eq(VmInstanceDeviceAddressVO_.vmInstanceUuid, vmInstanceUuid)
                .eq(VmInstanceDeviceAddressVO_.resourceUuid, resourceUuid)
                .isExists();

        VmInstanceDeviceAddressVO vo;
        if (addressExists) {
            vo = Q.New(VmInstanceDeviceAddressVO.class)
                    .eq(VmInstanceDeviceAddressVO_.vmInstanceUuid, vmInstanceUuid)
                    .eq(VmInstanceDeviceAddressVO_.resourceUuid, resourceUuid)
                    .find();
        } else {
            vo = new VmInstanceDeviceAddressVO();
        }

        vo.setPciAddress(pciAddress.toString());
        vo.setResourceUuid(resourceUuid);
        vo.setVmInstanceUuid(vmInstanceUuid);
        vo.setMetadata(metadata);

        if (metadataClass != null) {
            Class clazz;
            try {
                clazz = Class.forName(metadataClass);
            } catch (ClassNotFoundException e) {
                logger.warn(String.format("Unable to generate groovy class for %s", metadataClass), e);
                throw new CloudRuntimeException(e);
            }

            vo.setMetadataClass(clazz.getCanonicalName());
        }

        if (addressExists) {
            vo = dbf.updateAndRefresh(vo);
        } else {
            vo = dbf.persist(vo);
        }

        return vo;
    }

    @Override
    public VmInstanceDeviceAddressVO createOrUpdateVmDeviceAddress(VirtualDeviceInfo virtualDeviceInfo, String vmInstanceUuid) {
        return createOrUpdateVmDeviceAddress(virtualDeviceInfo.getResourceUuid(), virtualDeviceInfo.getPciInfo(), vmInstanceUuid, null, null);
    }

    @Override
    public String getVmDevicePciAddress(String resourceUuid, String vmInstanceUuid) {
        VmInstanceDeviceAddressVO vo = Q.New(VmInstanceDeviceAddressVO.class)
                .eq(VmInstanceDeviceAddressVO_.resourceUuid, resourceUuid)
                .eq(VmInstanceDeviceAddressVO_.vmInstanceUuid, vmInstanceUuid)
                .find();

        return vo != null ? vo.getPciAddress() : null;
    }

    @Override
    public ErrorCode deleteVmDeviceAddress(String resourceUuid, String vmInstanceUuid) {
        if (resourceUuid == null || vmInstanceUuid == null) {
            return operr("missing parameter, resourceUuid: %s, vmInstanceUuid: %s is requested", resourceUuid, vmInstanceUuid);
        }

        ErrorCode errorCode = checkParams(vmInstanceUuid, resourceUuid);
        if (errorCode != null) {
            return errorCode;
        }

        SQL.New(VmInstanceDeviceAddressVO.class)
                .eq(VmInstanceDeviceAddressVO_.resourceUuid, resourceUuid)
                .eq(VmInstanceDeviceAddressVO_.vmInstanceUuid, vmInstanceUuid)
                .delete();

        return null;
    }

    @Override
    public ErrorCode deleteAllDeviceAddressesByVm(String vmInstanceUuid) {
        return null;
    }

    @Override
    public VmInstanceDeviceAddressGroupVO archiveCurrentDeviceAddress(String vmInstanceUuid, String archiveForResourceUuid) {
        return new SQLBatchWithReturn<VmInstanceDeviceAddressGroupVO>() {
            @Override
            protected VmInstanceDeviceAddressGroupVO scripts() {
                List<VmInstanceDeviceAddressVO> deviceAddressVOList = q(VmInstanceDeviceAddressVO.class)
                        .eq(VmInstanceDeviceAddressVO_.vmInstanceUuid, vmInstanceUuid)
                        .list();

                VmInstanceDeviceAddressGroupVO group = new VmInstanceDeviceAddressGroupVO();
                group.setResourceUuid(archiveForResourceUuid);
                group.setUuid(Platform.getUuid());
                group = persist(group);

                for (VmInstanceDeviceAddressVO vo : deviceAddressVOList) {
                    VmInstanceDeviceAddressArchiveVO archiveVO = new VmInstanceDeviceAddressArchiveVO();
                    archiveVO.setPciAddress(vo.getPciAddress());
                    archiveVO.setResourceUuid(vo.getResourceUuid());
                    archiveVO.setVmInstanceUuid(vmInstanceUuid);
                    archiveVO.setAddressGroupUuid(group.getUuid());
                    persist(archiveVO);
                }

                return reload(group);
            }
        }.execute();
    }

    @Override
    public List<VmInstanceDeviceAddressVO> revertDeviceAddressFromArchive(String vmInstanceUuid, String archiveForResourceUuid) {
        VmInstanceDeviceAddressGroupVO group = Q.New(VmInstanceDeviceAddressGroupVO.class)
                .eq(VmInstanceDeviceAddressGroupVO_.resourceUuid, archiveForResourceUuid)
                .find();

        List<VmInstanceDeviceAddressVO> createdAddressList = new ArrayList<>();
        for (VmInstanceDeviceAddressArchiveVO archive : group.getAddressList()) {
            VmInstanceDeviceAddressVO vo = createOrUpdateVmDeviceAddress(archive.getResourceUuid(), PciAddressConfig.fromString(archive.getPciAddress()), vmInstanceUuid, archive.getMetadata(), archive.getMetadataClass());
            createdAddressList.add(vo);
        }

        return createdAddressList;
    }

    @Override
    public List<VmInstanceDeviceAddressVO> createDeviceAddressFromArchive(String vmInstanceUuid, String archiveForResourceUuid, Map<String, String> resourceMap) {
        VmInstanceDeviceAddressGroupVO group = Q.New(VmInstanceDeviceAddressGroupVO.class)
                .eq(VmInstanceDeviceAddressGroupVO_.resourceUuid, archiveForResourceUuid)
                .find();

        List<VmInstanceDeviceAddressVO> createdAddressList = new ArrayList<>();
        for (VmInstanceDeviceAddressArchiveVO archive : group.getAddressList()) {
            String matchedResourceUuid = resourceMap.get(archive.getResourceUuid());

            // create device address request new resourceUuid if not found skip pci address create
            if (matchedResourceUuid == null) {
                continue;
            }

            VmInstanceDeviceAddressVO vo = createOrUpdateVmDeviceAddress(archive.getResourceUuid(), PciAddressConfig.fromString(archive.getPciAddress()), vmInstanceUuid, archive.getMetadata(), archive.getMetadataClass());
            createdAddressList.add(vo);
        }

        return createdAddressList;
    }

    private boolean vmExists(String vmInstanceUuid) {
        return dbf.isExist(vmInstanceUuid, VmInstanceVO.class);
    }

    private boolean vmDeviceExists(String resourceUuid) {
        return new SQLBatchWithReturn<Boolean>() {

            @Override
            protected Boolean scripts() {
                boolean volumeExists = q(VolumeVO.class).eq(VolumeVO_.uuid, resourceUuid).isExists();
                boolean nicExists = q(VmNicVO.class).eq(VmNicVO_.uuid, resourceUuid).isExists();
                boolean cdRomExists = q(VmCdRomVO.class).eq(VmCdRomVO_.uuid, resourceUuid).isExists();

                return volumeExists || nicExists || cdRomExists;
            }
        }.execute();
    }

    private ErrorCode checkParams(String vmInstanceUuid, String resourceUuid) {
        if (!vmExists(vmInstanceUuid)) {
            return operr("cannot find vm with uuid: %s", vmInstanceUuid);
        }

        if (!vmDeviceExists(resourceUuid)) {
            return operr("cannot find vm device with uuid: %s", resourceUuid);
        }

        return null;
    }
}
