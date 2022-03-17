package org.zstack.compute.vm;

import org.zstack.core.db.Q;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.configuration.InstanceOfferingVO_;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.NewVmInstanceMessage;
import org.zstack.header.vm.NewVmInstanceMessage2;
import org.zstack.header.vm.VmNicSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2020/10/10.
 */
public class NewVmInstanceMsgBuilder {
    private static List<VmNicSpec> getVmNicSpecsFromNewVmInstanceMsg(NewVmInstanceMessage msg) {
        List<VmNicSpec> nicSpecs = new ArrayList<>();

        for (String l3Uuid : msg.getL3NetworkUuids()) {
            List<L3NetworkInventory> l3Invs = new ArrayList<>();
            L3NetworkVO l3vo = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, l3Uuid).find();
            L3NetworkInventory inv = L3NetworkInventory.valueOf(l3vo);
            l3Invs.add(inv);
            nicSpecs.add(new VmNicSpec(l3Invs));
        }

        return nicSpecs;
    }

    public static CreateVmInstanceMsg fromAPINewVmInstanceMsg(NewVmInstanceMessage2 msg) {
        CreateVmInstanceMsg cmsg = new CreateVmInstanceMsg();
        APICreateMessage api = (APICreateMessage) msg;

        if(msg.getZoneUuid() != null){
            cmsg.setZoneUuid(msg.getZoneUuid());
        }else{
            String zoneUuid = Q.New(L3NetworkVO.class)
                    .select(L3NetworkVO_.zoneUuid)
                    .eq(L3NetworkVO_.uuid, msg.getL3NetworkUuids().get(0))
                    .findValue();
            cmsg.setZoneUuid(zoneUuid);
        }

        final String instanceOfferingUuid = msg.getInstanceOfferingUuid();
        if (instanceOfferingUuid != null) {
            InstanceOfferingVO iovo = Q.New(InstanceOfferingVO.class).eq(InstanceOfferingVO_.uuid, instanceOfferingUuid).find();
            cmsg.setInstanceOfferingUuid(iovo.getUuid());
            cmsg.setCpuSpeed(iovo.getCpuSpeed());
            cmsg.setAllocatorStrategy(iovo.getAllocatorStrategy());
        }

        cmsg.setCpuNum(msg.getCpuNum() == null ? 0 : msg.getCpuNum());
        cmsg.setMemorySize(msg.getMemorySize() == null ? 0 : msg.getMemorySize());

        cmsg.setAccountUuid(api.getSession().getAccountUuid());
        cmsg.setName(msg.getName());
        cmsg.setL3NetworkSpecs(getVmNicSpecsFromNewVmInstanceMsg(msg));
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
