package org.zstack.compute.vm;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.Component;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.VSwitchType;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.UsedIpVO_;
import org.zstack.header.tag.*;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.VmInstanceDeviceManager;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import javax.persistence.LockModeType;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmNicManagerImpl implements VmNicManager, VmNicExtensionPoint, PrepareDbInitialValueExtensionPoint, Component {
    private static final CLogger logger = Utils.getLogger(VmNicManagerImpl.class);

    @Autowired
    private VmInstanceDeviceManager vidm;
    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    private DatabaseFacade dbf;

    private List<String> supportNicDriverTypes;
    private String defaultPVNicDriver;
    private String defaultNicDriver;
    private String pcNetNicDriver;

    @Override
    public void afterAddIpAddress(String vmNicUUid, String usedIpUuid) {
        /* update UsedIpVO */
        SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, usedIpUuid).set(UsedIpVO_.vmNicUuid, vmNicUUid).update();

        VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid).find();
        if (nic == null) {
            logger.debug(String.format("VmNic[uuid:%s] not found, skip afterAddIpAddress", vmNicUUid));
            return;
        }

        UsedIpVO temp = null;
        /* if there is ipv4 addresses, we put the first attached ipv4 address to VmNic.ip
         * or we put the first attached ipv6 address to vmNic.Ip */
        List<UsedIpVO> refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv4)
                .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
        if (refs != null && !refs.isEmpty()) {
            temp = refs.get(0);
        } else {
            refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv6)
                    .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
            if (refs != null && !refs.isEmpty()) {
                temp = refs.get(0);
            }
        }

        if (temp != null && !temp.getUuid().equals(nic.getUsedIpUuid())) {
            SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid)
                    .set(VmNicVO_.ip, temp.getIp())
                    .set(VmNicVO_.netmask, temp.getNetmask())
                    .set(VmNicVO_.gateway, temp.getGateway())
                    .set(VmNicVO_.usedIpUuid, temp.getUuid())
                    .set(VmNicVO_.ipVersion, temp.getIpVersion())
                    .set(VmNicVO_.l3NetworkUuid, temp.getL3NetworkUuid()).update();
        }
    }

    @Override
    public void afterDelIpAddress(String vmNicUUid, String usedIpUuid) {
        VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid).find();
        if (nic == null) {
            logger.debug(String.format("VmNic[uuid:%s] not found, skip afterDelIpAddress", vmNicUUid));
            return;
        }
        if (nic.getUsedIpUuid() != null && !nic.getUsedIpUuid().equals(usedIpUuid)) {
            return;
        }

        UsedIpVO temp = null;
        /* if there is ipv4 addresses, we put the first attached ipv4 address to VmNic.ip
         * or we put the first attached ipv6 address to vmNic.Ip */
        List<UsedIpVO> refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv4)
                .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
        if (refs != null && !refs.isEmpty()) {
            temp = refs.get(0);
        } else {
            refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv6)
                    .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
            if (refs != null && !refs.isEmpty()) {
                temp = refs.get(0);
            }
        }

        if (temp != null) {
            SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid)
                    .set(VmNicVO_.ip, temp.getIp())
                    .set(VmNicVO_.netmask, temp.getNetmask())
                    .set(VmNicVO_.gateway, temp.getGateway())
                    .set(VmNicVO_.usedIpUuid, temp.getUuid())
                    .set(VmNicVO_.ipVersion, temp.getIpVersion())
                    .set(VmNicVO_.l3NetworkUuid, temp.getL3NetworkUuid()).update();
        }
    }

    @Override
    public void prepareDbInitialValue() {
        // Do not rewrite non-null historical models or prevent management-node startup.
        List<VmNicVO> nics = Q.New(VmNicVO.class).isNull(VmNicVO_.driverType)
                .eq(VmNicVO_.type, VmInstanceConstant.VIRTUAL_NIC_TYPE)
                .notNull(VmNicVO_.vmInstanceUuid).list();
        Map<String, VmInstanceInventory> vms = new HashMap<>();
        for (VmNicVO vo : nics) {
            VmInstanceInventory vm = vms.computeIfAbsent(vo.getVmInstanceUuid(), uuid -> {
                VmInstanceVO vmVO = dbf.findByUuid(uuid, VmInstanceVO.class);
                return vmVO == null ? null : VmInstanceInventory.valueOf(vmVO);
            });
            if (vm == null) {
                continue;
            }
            VmNicInventory nic = VmNicInventory.valueOf(vo);
            try {
                nic.setDriverType(getDefaultNicDriver(vm, VmSystemTags.VIRTIO.hasTag(vm.getUuid())));
                validateNicDriver(nic, vm, false);
                SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, nic.getUuid()).isNull(VmNicVO_.driverType)
                        .set(VmNicVO_.driverType, nic.getDriverType()).update();
            } catch (OperationFailureException | IllegalArgumentException e) {
                logger.warn(String.format("Cannot initialize NIC[%s] model; keep the historical value for explicit repair: %s",
                        nic.getUuid(), e.getMessage()));
            }
        }
    }

    @Override
    public void validateNicDriver(VmNicInventory nic, VmInstanceInventory vm, boolean explicitModel) {
        VmNicDriverModel model;
        try {
            model = VmNicType.valueOf(nic.getType()).getDriverModel();
        } catch (IllegalArgumentException e) {
            rejectNicDriver(nic, "the backend type is not registered");
            return;
        }
        if (model == null) {
            rejectNicDriver(nic, "the backend has not declared its driver model capability");
        }
        if (model == VmNicDriverModel.Passthrough) {
            if (explicitModel) {
                rejectNicDriver(nic, "PCI passthrough does not expose an emulated NIC model");
            }
            return;
        }
        // Shared allocation is also used by ESX cloning, whose models are validated
        // by VMwareVmNicManager (E1000/E1000E/Vmxnet3/Sriov), not this KVM list.
        String hypervisor = nic.getHypervisorType() == null ? vm.getHypervisorType() : nic.getHypervisorType();
        if ((hypervisor == null || VmInstanceConstant.KVM_HYPERVISOR_TYPE.equals(hypervisor)
                || VmInstanceConstant.XDRAGON_HYPERVISOR_TYPE.equals(hypervisor))
                && !supportNicDriverTypes.contains(nic.getDriverType())) {
            rejectNicDriver(nic, "supported models are " + supportNicDriverTypes);
        }
        if (model == VmNicDriverModel.Virtio && !VmNicDriverType.VIRTIO.toString().equals(nic.getDriverType())) {
            rejectNicDriver(nic, "the backend requires virtio");
        }
        for (VmNicDriverValidationExtensionPoint ext : pluginRegistry.getExtensionList(VmNicDriverValidationExtensionPoint.class)) {
            ext.validateNicDriver(vm, nic);
        }
    }

    private void rejectNicDriver(VmNicInventory nic, String reason) {
        throw new OperationFailureException(argerr(ORG_ZSTACK_COMPUTE_VM_10346,
                "NIC[uuid:%s, type:%s] does not support driver model[%s]: %s",
                nic.getUuid(), nic.getType(), nic.getDriverType(), reason));
    }

    @Override
    public String getDefaultNicDriver(VmInstanceInventory vm, boolean virtioEnabled) {
        Map<Object, Integer> orders = new IdentityHashMap<>();
        for (PluginExtension extension : pluginRegistry.getExtensionByInterfaceName(VmNicSetDriverExtensionPoint.class.getName())) {
            orders.merge(extension.getInstance(), extension.getOrder(), Math::max);
        }
        Set<String> preferences = new TreeSet<>();
        int highestOrder = Integer.MIN_VALUE;
        for (VmNicSetDriverExtensionPoint ext : pluginRegistry.getExtensionList(VmNicSetDriverExtensionPoint.class)) {
            String preferred = ext.getPreferredVmNicDriver(vm);
            if (preferred == null) {
                continue;
            }
            int order = orders.getOrDefault(ext, 0);
            if (order > highestOrder) {
                highestOrder = order;
                preferences.clear();
            }
            if (order == highestOrder) {
                preferences.add(preferred);
            }
        }
        if (preferences.size() > 1) {
            throw new OperationFailureException(argerr(ORG_ZSTACK_COMPUTE_VM_10347,
                    "Conflicting NIC driver preferences[%s] for VM[uuid:%s]", preferences, vm.getUuid()));
        }
        return preferences.isEmpty() ? (virtioEnabled || ImagePlatform.valueOf(vm.getPlatform()).isParaVirtualization()
                ? defaultPVNicDriver : defaultNicDriver) : preferences.iterator().next();
    }

    @Override
    @Transactional
    public void changeVmPlatform(VmInstanceInventory target) {
        lockVm(target.getUuid());
        resetVmNicDriverType(target, getDefaultNicDriver(target, VmSystemTags.VIRTIO.hasTag(target.getUuid())));
    }

    private VmInstanceVO lockVm(String uuid) {
        return dbf.getEntityManager().find(VmInstanceVO.class, uuid, LockModeType.PESSIMISTIC_WRITE);
    }

    private void validateReset(VmInstanceInventory vm, String driverType) {
        for (VmNicVO vo : Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vm.getUuid()).<VmNicVO>list()) {
            if (VmNicType.valueOf(vo.getType()).getDriverModel() == VmNicDriverModel.Passthrough) {
                continue;
            }
            VmNicInventory nic = VmNicInventory.valueOf(vo);
            nic.setDriverType(driverType);
            validateNicDriver(nic, vm, false);
        }
    }

    private void resetVmNicDriverType(VmInstanceInventory vm, String driverType) {
        validateReset(vm, driverType);
        List<String> uuids = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vm.getUuid()).<VmNicVO>list().stream()
                .filter(n -> VmNicType.valueOf(n.getType()).getDriverModel() != VmNicDriverModel.Passthrough)
                .filter(n -> !Objects.equals(driverType, n.getDriverType()))
                .map(VmNicVO::getUuid).collect(Collectors.toList());
        if (!uuids.isEmpty()) {
            SQL.New(VmNicVO.class).in(VmNicVO_.uuid, uuids).set(VmNicVO_.driverType, driverType).update();
        }
    }

    public void setSupportNicDriverTypes(List<String> supportNicDriverTypes) {
        this.supportNicDriverTypes = supportNicDriverTypes;
    }

    public void setDefaultPVNicDriver(String defaultPVNicDriver) {
        this.defaultPVNicDriver = defaultPVNicDriver;
    }

    public void setDefaultNicDriver(String defaultNicDriver) {
        this.defaultNicDriver = defaultNicDriver;
    }

    @Override
    public List<String> getSupportNicDriverTypes() {
        return supportNicDriverTypes;
    }

    @Override
    public String getDefaultPVNicDriver() {
        return defaultPVNicDriver;
    }

    @Override
    public String getDefaultNicDriver() {
        return defaultNicDriver;
    }

    @Override
    public String getPcNetNicDriver() {
        return pcNetNicDriver;
    }

    public void setPcNetNicDriver(String pcNetNicDriver) {
        this.pcNetNicDriver = pcNetNicDriver;
    }

    @Override
    public void setNicDriverType(VmNicInventory nic, boolean isImageSupportVirtIo, boolean isParaVirtualization, VmInstanceInventory vm) {
        nic.setDriverType(getDefaultNicDriver(vm, isImageSupportVirtIo || isParaVirtualization
                || VmSystemTags.VIRTIO.hasTag(vm.getUuid())));
    }

    @Override
    public VmNicType getVmNicType(String vmUuid, L3NetworkInventory l3nw) {
        return getVmNicType(vmUuid, l3nw, null);
    }

    @Override
    public VmNicType getVmNicType(String vmUuid, L3NetworkInventory l3nw, List<String> vmSystemTags) {
        L2NetworkVO l2nw =  dbf.findByUuid(l3nw.getL2NetworkUuid(), L2NetworkVO.class);
        VSwitchType vSwitchType = VSwitchType.valueOf(l2nw.getvSwitchType());

        if (L2NetworkConstant.VSWITCH_TYPE_ZNS.equals(l2nw.getvSwitchType())) {
            List<String> znsNicModes = new ArrayList<>();
            if (vmSystemTags != null) {
                znsNicModes.addAll(vmSystemTags.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(VmSystemTags.ZNS_NIC_MODE::isMatch)
                        .map(tag -> VmSystemTags.ZNS_NIC_MODE.getTokenByTag(tag, VmSystemTags.ZNS_NIC_MODE_TOKEN))
                        .collect(Collectors.toList()));
            }
            znsNicModes.addAll(VmSystemTags.ZNS_NIC_MODE.getTags(vmUuid).stream()
                    .map(tag -> VmSystemTags.ZNS_NIC_MODE.getTokenByTag(tag, VmSystemTags.ZNS_NIC_MODE_TOKEN))
                    .collect(Collectors.toList()));

            List<String> normalizedModes = znsNicModes.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());

            for (String mode : normalizedModes) {
                if (!VmInstanceConstant.ZNS_NIC_MODE_DPDK.equals(mode) && !VmInstanceConstant.ZNS_NIC_MODE_KERNEL.equals(mode)) {
                    throw new OperationFailureException(argerr(ORG_ZSTACK_COMPUTE_VM_10257,
                            "invalid znsNicMode[%s], valid values are [%s, %s]",
                            mode, VmInstanceConstant.ZNS_NIC_MODE_DPDK, VmInstanceConstant.ZNS_NIC_MODE_KERNEL));
                }
            }

            if (normalizedModes.size() > 1) {
                throw new OperationFailureException(argerr(ORG_ZSTACK_COMPUTE_VM_10257,
                        "conflicting znsNicMode tags %s, only one mode is allowed", normalizedModes));
            }

            boolean enableZnsDpdk = normalizedModes.contains(VmInstanceConstant.ZNS_NIC_MODE_DPDK);
            logger.debug(String.format("create %s on zns l3 network[uuid:%s] inside VmAllocateNicFlow",
                    enableZnsDpdk ? "dpdk vhostuser nic" : "vnic", l3nw.getUuid()));
            return vSwitchType.getVmNicType(enableZnsDpdk ? VmNicType.VmNicSubType.VHOSTUSER : VmNicType.VmNicSubType.NONE);
        }

        List<String> tags = new ArrayList<>();
        tags.add(String.format("enableSRIOV::%s", l3nw.getUuid()));
        tags.add(String.format("enableVFHA::%s", l3nw.getUuid()));
        boolean enableSriov = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .eq(SystemTagVO_.resourceUuid, vmUuid)
                .in(SystemTagVO_.tag, tags)
                .isExists();
        logger.debug(String.format("create %s on l3 network[uuid:%s] inside VmAllocateNicFlow",
                enableSriov ? "vf nic" : "vnic", l3nw.getUuid()));
        boolean enableVhostUser = NetworkServiceGlobalConfig.ENABLE_VHOSTUSER.value(Boolean.class);

        boolean enableDpdkVhostuser = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .eq(SystemTagVO_.resourceUuid, vmUuid)
                .eq(SystemTagVO_.tag, String.format("enableDpdkVhostuser::%s", l3nw.getUuid()))
                .isExists();

        VmNicType.VmNicSubType subType = VmNicType.VmNicSubType.NONE;
        if (enableSriov) {
            subType = VmNicType.VmNicSubType.SRIOV;
        } else if (enableVhostUser || enableDpdkVhostuser) {
            subType = VmNicType.VmNicSubType.VHOSTUSER;
        }

        return vSwitchType.getVmNicType(subType);
    }

    @Override
    public boolean start() {
        VmSystemTags.VIRTIO.setFailOnListenerError(true);
        VmSystemTags.VIRTIO.installJudger(new AbstractSystemTagOperationJudger() {
            private void validate(SystemTagInventory tag, String driver) {
                VmInstanceVO vo = lockVm(tag.getResourceUuid());
                if (vo == null || vo.getState() == VmInstanceState.Destroyed) {
                    return;
                }
                // Keep state restrictions in the existing creation validator. Internal
                // GuestTools flows also create this tag while stopping/rebooting;
                // deleting it while running changes the next-start configuration.
                validateReset(VmInstanceInventory.valueOf(vo), driver);
            }

            @Override
            public void tagPreCreated(SystemTagInventory tag) {
                validate(tag, defaultPVNicDriver);
            }

            @Override
            public void tagPreDeleted(SystemTagInventory tag) {
                validate(tag, defaultNicDriver);
            }
        });
        VmSystemTags.VIRTIO.installLifeCycleListener(new SystemTagLifeCycleListener() {
            @Override
            public void tagCreated(SystemTagInventory tag) {
                VmInstanceVO vo = lockVm(tag.getResourceUuid());
                if (vo == null || vo.getState() == VmInstanceState.Destroyed) { return; }
                resetVmNicDriverType(VmInstanceInventory.valueOf(vo), defaultPVNicDriver);
                vidm.deleteDeviceAddressesByVmModifyVirtIO(tag.getResourceUuid());
            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {
                VmInstanceVO vo = lockVm(tag.getResourceUuid());
                if (vo == null || vo.getState() == VmInstanceState.Destroyed) { return; }
                resetVmNicDriverType(VmInstanceInventory.valueOf(vo), defaultNicDriver);
                vidm.deleteDeviceAddressesByVmModifyVirtIO(tag.getResourceUuid());
            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {

            }
        });

        VmSystemTags.VIRTIO.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                VmInstanceState state = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, resourceUuid)
                        .select(VmInstanceVO_.state)
                        .findValue();

                if (state == VmInstanceState.Running || state == VmInstanceState.Unknown) {
                    throw new OperationFailureException(argerr(ORG_ZSTACK_COMPUTE_VM_10088, "vm current state[%s], " +
                            "modify virtio requires the vm state[%s]", state, VmInstanceState.Stopped));
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
