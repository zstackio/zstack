package org.zstack.header.vm;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.volume.APICreateDataVolumeFromVolumeTemplateMsg;
import org.zstack.header.volume.APICreateDataVolumeMsg;

import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.isEmpty;
import static org.zstack.utils.CollectionUtils.transform;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "vm";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(VmInstanceVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        expandedPermission(APICreateVmInstanceMsg.class, api -> {
            if (!isEmpty(api.getDataDiskOfferingUuids())) {
                return transform(api.getDataDiskOfferingUuids(), uuid -> {
                    APICreateDataVolumeMsg expendMsg = new APICreateDataVolumeMsg();
                    expendMsg.setDiskOfferingUuid(uuid);
                    return expendMsg;
                });
            }

            if (!isEmpty(api.getDataDiskSizes())) {
                return transform(api.getDataDiskSizes(), size -> {
                    APICreateDataVolumeMsg expendMsg = new APICreateDataVolumeMsg();
                    expendMsg.setDiskSize(size);
                    return expendMsg;
                });
            }

            if (!isEmpty(api.getDiskAOs()) && api.getDiskAOs().size() > 1) {
                return api.getDiskAOs().subList(1, api.getDiskAOs().size()).stream()
                        .map(diskAO -> {
                            if (diskAO.getSize() > 0) {
                                APICreateDataVolumeMsg expendMsg = new APICreateDataVolumeMsg();
                                expendMsg.setDiskSize(diskAO.getSize());
                                return expendMsg;
                            } else if (diskAO.getDiskOfferingUuid() != null) {
                                APICreateDataVolumeMsg expendMsg = new APICreateDataVolumeMsg();
                                expendMsg.setDiskOfferingUuid(diskAO.getDiskOfferingUuid());
                                return expendMsg;
                            } else if (diskAO.getTemplateUuid() != null) {
                                APICreateDataVolumeFromVolumeTemplateMsg expendMsg = new APICreateDataVolumeFromVolumeTemplateMsg();
                                expendMsg.setImageUuid(diskAO.getTemplateUuid());
                                return expendMsg;
                            }

                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            return Collections.emptyList();
        });
    }

    @Override
    public void contributeToRoles() {
        resourceEnsembleContributorBuilder()
                .resource(VmNicVO.class)
                .resource(VmCdRomVO.class)
                .contributeTo(VmInstanceVO.class)
                .build();

        roleContributorBuilder()
                .actions(
                        APIQueryVmInstanceMsg.class,
                        APIGetVmBootOrderMsg.class,
                        APIGetVmCapabilitiesMsg.class,
                        APIGetVmHostnameMsg.class,
                        APIGetVmsCapabilitiesMsg.class,
                        APIQueryVmNicMsg.class,
                        APITakeVmConsoleScreenshotMsg.class,
                        APIGetVmConsoleAddressMsg.class
                )
                .toOtherRole()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("5f93cf6444ec44cc83209744c8c3d7cc")
                .permissionBaseOnThis()
                .build();

        roleBuilder()
                .uuid("d6b79564f9b641a4b8bb85ea249151c2")
                .name("vm-operation-without-create-permission")
                .permissionBaseOnThis()
                .excludeActions(APICreateVmInstanceMsg.class)
                .build();
    }
}
