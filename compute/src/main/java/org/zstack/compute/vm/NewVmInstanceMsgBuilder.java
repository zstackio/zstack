package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.configuration.InstanceOfferingVO_;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2020/10/10.
 */
public class NewVmInstanceMsgBuilder {
    private static List<VmNicSpec> getVmNicSpecsFromNewVmInstanceMsg(NewVmInstanceMessage msg) {
        if (CollectionUtils.isEmpty(msg.getL3NetworkUuids())) {
            return Collections.EMPTY_LIST;
        }
        List<VmNicParam> vmNicParams = new ArrayList<>();
        if (!StringUtils.isEmpty(msg.getVmNicParams())) {
            vmNicParams.addAll(JSONObjectUtil.toCollection(msg.getVmNicParams(), ArrayList.class, VmNicParam.class));
        }

        List<VmNicSpec> nicSpecs = new ArrayList<>();
        for (String l3Uuid : msg.getL3NetworkUuids()) {
            List<L3NetworkInventory> l3Invs = new ArrayList<>();
            L3NetworkVO l3vo = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, l3Uuid).find();
            L3NetworkInventory inv = L3NetworkInventory.valueOf(l3vo);
            l3Invs.add(inv);

            VmNicSpec vmNicSpec = new VmNicSpec(l3Invs);
            if (!vmNicParams.isEmpty()) {
                List<VmNicParam> nicParamOfL3 = vmNicParams.stream().filter(vmNicParam -> vmNicParam.getL3NetworkUuid().equals(l3Uuid)).distinct().collect(Collectors.toList());
                if (!nicParamOfL3.isEmpty()) {
                    vmNicSpec.setVmNicParams(nicParamOfL3);
                    vmNicSpec.setNicDriverType(nicParamOfL3.get(0).getDriverType());
                    vmNicParams.removeAll(nicParamOfL3);
                }
            }
            nicSpecs.add(vmNicSpec);
        }


        return nicSpecs;
    }

    private static List<String> getDisableL3FromNewVmInstanceMsg(NewVmInstanceMessage msg) {
        if (StringUtils.isEmpty(msg.getVmNicParams())) {
            return Collections.EMPTY_LIST;
        }
        List<VmNicParam> vmNicParams = JSONObjectUtil.toCollection(msg.getVmNicParams(), ArrayList.class, VmNicParam.class);
        return vmNicParams.stream().filter(nic -> VmNicState.disable.toString().equals(nic.getState())).map(VmNicParam::getL3NetworkUuid).collect(Collectors.toList());
    }

    public static CreateVmInstanceMsg fromAPINewVmInstanceMsg(NewVmInstanceMessage2 msg) {
        CreateVmInstanceMsg cmsg = new CreateVmInstanceMsg();
        APICreateMessage api = (APICreateMessage) msg;

        if(msg.getZoneUuid() != null){
            cmsg.setZoneUuid(msg.getZoneUuid());
        }else{
            if (!CollectionUtils.isEmpty(msg.getL3NetworkUuids())) {
                String zoneUuid = Q.New(L3NetworkVO.class)
                        .select(L3NetworkVO_.zoneUuid)
                        .eq(L3NetworkVO_.uuid, msg.getL3NetworkUuids().get(0))
                        .findValue();
                cmsg.setZoneUuid(zoneUuid);
            }
        }

        final String instanceOfferingUuid = msg.getInstanceOfferingUuid();
        if (instanceOfferingUuid != null) {
            InstanceOfferingVO iovo = Q.New(InstanceOfferingVO.class).eq(InstanceOfferingVO_.uuid, instanceOfferingUuid).find();
            cmsg.setInstanceOfferingUuid(iovo.getUuid());
            cmsg.setCpuSpeed(iovo.getCpuSpeed());
            cmsg.setAllocatorStrategy(iovo.getAllocatorStrategy());
        }

        cmsg.setCpuNum(msg.getCpuNum());
        cmsg.setMemorySize(msg.getMemorySize());
        cmsg.setReservedMemorySize(msg.getReservedMemorySize() == null ? 0 : msg.getReservedMemorySize());

        cmsg.setAccountUuid(api.getSession().getAccountUuid());
        cmsg.setName(msg.getName());
        cmsg.setL3NetworkSpecs(getVmNicSpecsFromNewVmInstanceMsg(msg));
        cmsg.setDisableL3Networks(getDisableL3FromNewVmInstanceMsg(msg));
        cmsg.setType(msg.getType());

        cmsg.setClusterUuid(msg.getClusterUuid());
        cmsg.setHostUuid(msg.getHostUuid());
        cmsg.setDescription(msg.getDescription());
        cmsg.setResourceUuid(api.getResourceUuid());
        cmsg.setDefaultL3NetworkUuid(msg.getDefaultL3NetworkUuid());
        cmsg.setStrategy(msg.getStrategy());
        cmsg.setServiceId(api.getServiceId());
        cmsg.setHeaders(api.getHeaders());
        cmsg.setSystemTags(api.getSystemTags());
        cmsg.setUserTags(api.getUserTags());

        return cmsg;
    }
}
