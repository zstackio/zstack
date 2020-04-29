package org.zstack.compute.vm;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.UsedIpVO_;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import java.util.List;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmNicManagerImpl implements VmNicManager, VmNicExtensionPoint, PrepareDbInitialValueExtensionPoint, VmPlatformChangedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmNicManagerImpl.class);

    private List<String> supportNicDriverTypes;
    private String defaultPVNicDriver;
    private String defaultNicDriver;

    @Override
    public void afterAddIpAddress(String vmNicUUid, String usedIpUuid) {
        /* update UsedIpVO */
        SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, usedIpUuid).set(UsedIpVO_.vmNicUuid, vmNicUUid).update();

        VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid).find();

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

        if (!temp.getUuid().equals(nic.getUsedIpUuid())) {
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
        List<VmNicVO> nics = Q.New(VmNicVO.class).notNull(VmNicVO_.vmInstanceUuid).list();
        for (VmNicVO nic : nics) {
            if (nic.getUsedIps().size() <= 1) {
                continue;
            }

            /* systemTags for dualStack nic existed */
            List<String> secondaryL3Uuids = new DualStackNicSecondaryNetworksOperator().getSecondaryNetworksByVmUuidNic(nic.getVmInstanceUuid(), nic.getL3NetworkUuid());
            if (secondaryL3Uuids != null) {
                continue;
            }

            for (UsedIpVO ip : nic.getUsedIps()) {
                if (ip.getL3NetworkUuid().equals(nic.getL3NetworkUuid())) {
                    continue;
                }

                new DualStackNicSecondaryNetworksOperator().createSecondaryNetworksByVmNic(VmNicInventory.valueOf(nic), ip.getL3NetworkUuid());
            }
        }

        nics.forEach(nic -> {
            if (nic.getDriverType() != null) {
                return;
            }

            if (nic.getType() != VmInstanceConstant.VIRTUAL_NIC_TYPE) {
                return;
            }
            SQL.New(VmNicVO.class)
                    .eq(VmNicVO_.uuid, nic.getUuid())
                    .set(VmNicVO_.driverType, getNicDefaultDriver(nic.getVmInstanceUuid()))
                    .update();
        });
    }

    @Override
    public void vmPlatformChange(VmInstanceInventory vm, String previousPlatform, String nowPlatform) {
        if (ImagePlatform.valueOf(nowPlatform).isParaVirtualization()) {
            resetVmNicDriverType(vm, defaultPVNicDriver);
            return;
        }

        resetVmNicDriverType(vm, defaultNicDriver);
    }

    private void resetVmNicDriverType(VmInstanceInventory vm, String driverType) {
        VmInstanceVO vo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.getUuid()).find();
        List<String> needUpdateNics = vo.getVmNics().stream()
                .filter(nic -> nic.getDriverType() == null || !nic.getDriverType().equals(driverType))
                .map(VmNicVO::getUuid)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(needUpdateNics)) {
            return;
        }

        SQL.New(VmNicVO.class).in(VmNicVO_.uuid, needUpdateNics).set(VmNicVO_.driverType, driverType).update();
    }

    private String getNicDefaultDriver(String vmUuid) {
        VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
        return ImagePlatform.valueOf(vm.getPlatform()).isParaVirtualization() ?
                defaultPVNicDriver : defaultNicDriver;
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
}
