package org.zstack.compute.vm;

import org.zstack.core.db.Q;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.vm.*;
import org.zstack.network.service.DnsUtils;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.network.IPv6Constants;

import java.util.ArrayList;
import java.util.List;

public class VmNicParamBuilder {
    public static List<VmNicParam> buildByVmUuid(String vmInstanceUuid) {
        List<VmNicParam> vmNicParams = new ArrayList<>();
        boolean isWindowsVm = ImagePlatform.Windows.toString().equals(
                Q.New(VmInstanceVO.class).select(VmInstanceVO_.platform).eq(VmInstanceVO_.uuid, vmInstanceUuid).findValue());

        if (isWindowsVm) {
            List<VmNicVO> nicVOList = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vmInstanceUuid).list();
            for (VmNicVO nicVO: nicVOList) {
                VmNicParam nicParam = new VmNicParam();
                nicParam.setL3NetworkUuid(nicVO.getL3NetworkUuid());

                boolean isSet = false;

                List<String> dnsList = DnsUtils.getVmNicDnsList(nicVO.getUuid(), IPv6Constants.IPv4);
                if (!CollectionUtils.isEmpty(dnsList)) {
                    nicParam.setDnsList(dnsList);
                    isSet = true;
                }

                List<String> dns6List = DnsUtils.getVmNicDnsList(nicVO.getUuid(), IPv6Constants.IPv6);
                if (!CollectionUtils.isEmpty(dns6List)) {
                    nicParam.setDns6List(dns6List);
                    isSet = true;
                }

                if (isSet) {
                    vmNicParams.add(nicParam);
                }
            }
        } else {
            String defaultL3Uuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.defaultL3NetworkUuid).eq(VmInstanceVO_.uuid, vmInstanceUuid).findValue();
            VmNicParam nicParam = new VmNicParam();
            nicParam.setL3NetworkUuid(defaultL3Uuid);
            boolean isSet = false;

            List<String> dnsList = DnsUtils.getVmDnsList(vmInstanceUuid);
            if (!CollectionUtils.isEmpty(dnsList)) {
                nicParam.setDnsList(dnsList);
                isSet = true;
            }

            if (isSet) {
                vmNicParams.add(nicParam);
            }
        }

        return vmNicParams;
    }

    public static String buildJsonStringByVmUuid(String vmInstanceUuid) {
        return JSONObjectUtil.toJsonString(buildByVmUuid(vmInstanceUuid));
    }
}
