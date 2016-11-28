package org.zstack.test.deployer;

import org.zstack.compute.vm.VmSystemTags;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.UserVmConfig;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class UserVmDeployer implements VmDeployer<UserVmConfig> {
    @Override
    public Class<UserVmConfig> getSupportedDeployerClassType() {
        return UserVmConfig.class;
    }

    @Override
    public void deploy(List<UserVmConfig> vms, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (UserVmConfig vc : vms) {
            VmCreator creator = new VmCreator(deployer.getApi());
            for (String l3ref : vc.getL3NetworkRef()) {
                L3NetworkInventory l3inv = deployer.l3Networks.get(l3ref);
                if (l3inv == null) {
                    throw new CloudRuntimeException(String.format("Cannot find L3Network[name:%s]", l3ref));
                }
                creator.addL3Network(l3inv.getUuid());
            }

            if (vc.getDefaultL3NetworkRef() != null) {
                L3NetworkInventory defaultL3 = deployer.l3Networks.get(vc.getDefaultL3NetworkRef());
                if (defaultL3 == null) {
                    throw new CloudRuntimeException(String.format("Cannot find L3Network[name:%s]", vc.getDefaultL3NetworkRef()));
                }
                creator.defaultL3NetworkUuid = defaultL3.getUuid();
            }

            for (String dref : vc.getDiskOfferingRef()) {
                DiskOfferingInventory dinv = deployer.diskOfferings.get(dref);
                if (dinv == null) {
                    throw new CloudRuntimeException(String.format("Cannot find DiskOffering[name:%s]", dref));
                }
                creator.addDisk(dinv.getUuid());
            }

            if (vc.getZoneRef() != null) {
                ZoneInventory zinv = deployer.zones.get(vc.getZoneRef());
                if (zinv == null) {
                    throw new CloudRuntimeException(String.format("Cannot find zone[name:%s]", vc.getZoneRef()));
                }
                creator.zoneUuid = zinv.getUuid();
            }

            if (vc.getClusterRef() != null) {
                ClusterInventory cinv = deployer.clusters.get(vc.getClusterRef());
                if (cinv == null) {
                    throw new CloudRuntimeException(String.format("Cannot find cluster[name:%s]", vc.getClusterRef()));
                }
                creator.clusterUUid = cinv.getUuid();
            }
            if (vc.getHostRef() != null) {
                HostInventory hinv = deployer.hosts.get(vc.getHostRef());
                if (hinv == null) {
                    throw new CloudRuntimeException(String.format("Cannot find host[name:%s]", vc.getHostRef()));
                }
                creator.hostUuid = hinv.getUuid();
            }
            if (vc.getRootDiskOfferingRef() != null) {
                DiskOfferingInventory rdinv = deployer.diskOfferings.get(vc.getRootDiskOfferingRef());
                if (rdinv == null) {
                    throw new CloudRuntimeException(String.format("Cannot find root DiskOffering[name:%s]", vc.getRootDiskOfferingRef()));
                }
                creator.rootDiskOfferingUuid = rdinv.getUuid();
            }
            InstanceOfferingInventory ioinv = deployer.instanceOfferings.get(vc.getInstanceOfferingRef());
            if (ioinv == null) {
                throw new CloudRuntimeException(String.format("Cannot find root InstanceOffering[name:%s]", vc.getInstanceOfferingRef()));
            }
            creator.instanceOfferingUuid = ioinv.getUuid();

            ImageInventory iminv = deployer.images.get(vc.getImageRef());
            if (iminv == null) {
                throw new CloudRuntimeException(String.format("Cannot find image[name:%s]", vc.getImageRef()));
            }
            creator.imageUuid = iminv.getUuid();

            creator.name = vc.getName();
            creator.description = vc.getDescription();
            if (vc.getAccountRef() != null) {
                creator.session = deployer.loginByAccountRef(vc.getAccountRef(), config);
            }

            List<String> sysTags = new ArrayList<String>();
            for (String hostname : vc.getHostname()) {
                String tag = VmSystemTags.HOSTNAME.instantiateTag(map(
                        e(VmSystemTags.HOSTNAME_TOKEN, hostname)
                ));
                sysTags.add(tag);
            }
            creator.systemTags = sysTags;

            VmInstanceInventory inv = creator.create();
            deployer.vms.put(inv.getName(), inv);
        }
    }

}
